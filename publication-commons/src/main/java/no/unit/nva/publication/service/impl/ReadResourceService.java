package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.*;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.model.business.publicationstate.FileDeletedEvent;
import no.unit.nva.publication.model.storage.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static no.unit.nva.publication.model.business.Resource.resourceQueryObject;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.conditionValueMapToAttributeValueMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.*;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings({"PMD.CouplingBetweenObjects"})
public class ReadResourceService {

    public static final String PUBLICATION_NOT_FOUND_CLIENT_MESSAGE = "Publication not found: ";

    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Could not find resource ";
    public static final int DEFAULT_LIMIT = 100;
    private static final String ADDITIONAL_IDENTIFIER_CRISTIN = "Cristin";
    private final AmazonDynamoDB client;
    private final String tableName;

    protected ReadResourceService(AmazonDynamoDB client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    public List<PublicationSummary> getResourcesByOwner(UserInstance userInstance) {
        var partitionKey = constructPrimaryPartitionKey(userInstance);
        var querySpec = partitionKeyToQuerySpec(partitionKey);
        var valuesMap = conditionValueMapToAttributeValueMap(querySpec.getValueMap(), String.class);
        var namesMap = querySpec.getNameMap();
        var result = performQuery(querySpec.getKeyConditionExpression(), valuesMap, namesMap, DEFAULT_LIMIT);

        return queryResultToListOfPublicationSummaries(result);
    }

    // TODO: Fetch PublicationChannels and set them when persistence of PublicationChannel is implemented in NP-49090
    public Optional<Resource> getResourceByIdentifier(SortableIdentifier identifier) {
        var partitionKey = resourceQueryObject(identifier).toDao().getByTypeAndIdentifierPartitionKey();
        var queryRequest = new QueryRequest()
                               .withTableName(tableName)
                               .withIndexName(BY_TYPE_AND_IDENTIFIER_INDEX_NAME)
                               .withKeyConditionExpression("#PK3 = :value")
                               .withExpressionAttributeNames(Map.of("#PK3", "PK3"))
                               .withExpressionAttributeValues(Map.of(":value", new AttributeValue(partitionKey)));

        var entries = client.query(queryRequest).getItems().stream()
                          .map(map -> parseAttributeValuesMap(map, Dao.class))
                          .toList();

        var resource = extractResource(entries);
        var fileEntries = extractFileEntries(entries);
        var publicationChannels = extractPublicationChannels(entries);

        resource.ifPresent(res -> {
            var associatedArtifacts = new ArrayList<AssociatedArtifact>();

            var files = fileEntries.stream().map(FileEntry::getFile).toList();
            var associatedLinks = res.getAssociatedArtifacts().stream()
                                      .filter(associatedArtifact -> !(associatedArtifact instanceof File))
                                      .toList();

            associatedArtifacts.addAll(files);
            associatedArtifacts.addAll(associatedLinks);

            res.setFileEntries(fileEntries);
            res.setAssociatedArtifacts(new AssociatedArtifactList(associatedArtifacts));
            res.setPublicationChannels(publicationChannels);
        });
        return resource;
    }

    public Stream<TicketEntry> fetchAllTicketsForResource(Resource resource) {
        var dao = (ResourceDao) resource.toDao();
        return dao.fetchAllTickets(client)
                   .stream()
                   .map(TicketDao::getData)
                   .map(TicketEntry.class::cast)
                   .filter(ReadResourceService::isNotRemoved);
    }

    private static boolean isNotRemoved(TicketEntry ticket) {
        return !TicketStatus.REMOVED.equals(ticket.getStatus());
    }

    private static Optional<Resource> extractResource(List<Dao> entries) {
        return entries.stream()
                   .filter(ResourceDao.class::isInstance)
                   .map(ResourceDao.class::cast)
                   .map(ResourceDao::getResource)
                   .findFirst();
    }

    private static List<FileEntry> extractFileEntries(List<Dao> entries) {
        return entries.stream()
                   .filter(FileDao.class::isInstance)
                   .map(FileDao.class::cast)
                   .map(FileDao::getFileEntry)
                   .filter(ReadResourceService::isNotSoftDeleted)
                   .toList();
    }

    private List<PublicationChannel> extractPublicationChannels(List<Dao> entries) {
        return entries.stream()
                   .filter(PublicationChannelDao.class::isInstance)
                   .map(PublicationChannelDao.class::cast)
                   .map(PublicationChannelDao::getData)
                   .map(PublicationChannel.class::cast)
                   .toList();
    }

    private static boolean isNotSoftDeleted(FileEntry fileEntry) {
        return !(fileEntry.getFileEvent() instanceof FileDeletedEvent);
    }

    public List<Publication> getPublicationsByCristinIdentifier(String cristinIdentifier) {
        var queryObject = new ResourceDao(resourceQueryObjectWithCristinIdentifier(cristinIdentifier));
        var queryRequest = queryObject.createQueryFindByCristinIdentifier();
        var queryResult = client.query(queryRequest);
        return queryResultToListOfPublications(queryResult);
    }

    protected List<Dao> fetchResourceAndDoiRequestFromTheByResourceIndex(UserInstance userInstance,
                                                                         SortableIdentifier resourceIdentifier) {
        ResourceDao queryObject = ResourceDao.queryObject(userInstance, resourceIdentifier);
        QueryRequest queryRequest = attempt(() -> queryByResourceIndex(queryObject)).orElseThrow();
        QueryResult queryResult = client.query(queryRequest);
        return parseResultSetToDaos(queryResult);
    }

    private static List<Resource> queryResultToResourceList(QueryResult result) {
        return result.getItems()
                   .stream()
                   .map(resultValuesMap -> parseAttributeValuesMap(resultValuesMap, ResourceDao.class))
                   .map(ResourceDao::getData)
                   .map(Resource.class::cast)
                   .collect(Collectors.toList());
    }

    private Resource resourceQueryObjectWithCristinIdentifier(String cristinIdentifier) {
        var resource = new Resource();
        resource.setAdditionalIdentifiers(
            Set.of(new AdditionalIdentifier(ADDITIONAL_IDENTIFIER_CRISTIN, cristinIdentifier)));
        return resource;
    }

    private String constructPrimaryPartitionKey(UserInstance userInstance) {
        return ResourceDao.constructPrimaryPartitionKey(userInstance.getCustomerId(),
                                                        userInstance.getUsername());
    }

    private List<Publication> queryResultToListOfPublications(QueryResult result) {
        return queryResultToResourceList(result)
                   .stream()
                   .map(Resource::toPublication)
                   .toList();
    }

    private List<PublicationSummary> queryResultToListOfPublicationSummaries(QueryResult result) {
        return queryResultToResourceList(result)
                   .stream()
                   .map(Resource::toSummary)
                   .toList();
    }

    private QueryResult performQuery(String conditionExpression, Map<String, AttributeValue> valuesMap,
                                     Map<String, String> namesMap, int limit) {
        return client.query(
            new QueryRequest().withKeyConditionExpression(conditionExpression)
                .withExpressionAttributeNames(namesMap)
                .withExpressionAttributeValues(valuesMap)
                .withTableName(tableName)
                .withLimit(limit)
        );
    }

    private QueryExpressionSpec partitionKeyToQuerySpec(String partitionKey) {
        return new ExpressionSpecBuilder()
                   .withKeyCondition(S(PRIMARY_KEY_PARTITION_KEY_NAME).eq(partitionKey)).buildForQuery();
    }

    private QueryRequest queryByResourceIndex(ResourceDao queryObject) {
        var doiRequestQueryObject = DoiRequestDao.queryObject(queryObject);
        Map<String, Condition> keyConditions = queryObject
                                                   .byResource(
                                                       queryObject.joinByResourceContainedOrderedType(),
                                                       doiRequestQueryObject.joinByResourceContainedOrderedType()
                                                   );
        return new QueryRequest()
                   .withTableName(tableName)
                   .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                   .withKeyConditions(keyConditions);
    }

    private List<Dao> parseResultSetToDaos(QueryResult queryResult) {
        return queryResult.getItems()
                   .stream()
                   .map(values -> parseAttributeValuesMap(values, Dao.class))
                   .collect(Collectors.toList());
    }
}

package no.unit.nva.publication.service.impl;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.Resource.resourceQueryObject;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.conditionValueMapToAttributeValueMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_1_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.IMPORT_CANDIDATE_KEY_PATTERN;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.SCOPUS_IDENTIFIER_INDEX_FIELD_PREFIX;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.model.Publication;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.ResourceRelationship;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.model.business.publicationstate.FileDeletedEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.ResourceRelationshipDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.model.storage.importcandidate.DatabaseEntryWithData;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;
import no.unit.nva.publication.storage.model.DatabaseConstants;

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

    public Optional<Resource> getResourceByIdentifier(SortableIdentifier identifier) {
        var partitionKey = resourceQueryObject(identifier).toDao().getByTypeAndIdentifierPartitionKey();
        var queryRequest = new QueryRequest()
                               .withTableName(tableName)
                               .withIndexName(BY_TYPE_AND_IDENTIFIER_INDEX_NAME)
                               .withKeyConditionExpression("#PK3 = :value")
                               .withExpressionAttributeNames(Map.of("#PK3", "PK3"))
                               .withExpressionAttributeValues(Map.of(":value", new AttributeValue(partitionKey)));

        var entries = client.query(queryRequest).getItems().stream().toList();

        var resource = extractResource(entries);
        var fileEntries = extractFileEntries(entries);
        var publicationChannels = extractPublicationChannels(entries);
        var resourceRelationships = extractResourceRelationships(entries);

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
            res.setRelatedResources(resourceRelationships);
        });
        return resource;
    }

    private List<SortableIdentifier> extractResourceRelationships(Collection<Map<String, AttributeValue>> entries) {
        return entries.stream()
                   .filter(map -> hasTypeProperty(map, ResourceRelationshipDao.TYPE))
                   .map(map -> DatabaseEntryWithData.fromAttributeValuesMap(map, ResourceRelationshipDao.class))
                   .map(ResourceRelationshipDao::getData)
                   .map(ResourceRelationship::childIdentifier)
                   .toList();
    }

    public Optional<ImportCandidate> getImportCandidateByIdentifier(SortableIdentifier identifier) {
        var getItemRequest = getGetItemRequest(identifier);
        var result = client.getItem(getItemRequest);
        if (isNull(result.getItem()) || result.getItem().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(DatabaseEntryWithData.fromAttributeValuesMap(result.getItem(), ImportCandidateDao.class))
                   .map(ImportCandidateDao::getData);
    }

    private GetItemRequest getGetItemRequest(SortableIdentifier identifier) {
        var primaryKey = new AttributeValue(IMPORT_CANDIDATE_KEY_PATTERN.formatted(identifier));
        return new GetItemRequest(tableName, Map.of(PRIMARY_KEY_PARTITION_KEY_NAME, primaryKey,
                                                    PRIMARY_KEY_SORT_KEY_NAME, primaryKey));
    }

    public Stream<TicketEntry> fetchAllTicketsForResource(Resource resource) {
        var dao = (ResourceDao) resource.toDao();
        return dao.fetchAllTickets(client)
                   .stream()
                   .map(TicketDao::getData)
                   .map(TicketEntry.class::cast)
                   .filter(ReadResourceService::isNotRemoved);
    }

    public List<Dao> fetchAllResourceAssociatedEntries(URI customerId, SortableIdentifier resourceIdentifier) {
        var value = "Customer:%s:Resource:%s".formatted(Dao.orgUriToOrgIdentifier(customerId), resourceIdentifier);

        var queryRequest = new QueryRequest()
                               .withTableName(RESOURCES_TABLE_NAME)
                               .withIndexName(DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME)
                               .withKeyConditionExpression("PK2 = :value")
                               .withExpressionAttributeValues(Map.of(":value", new AttributeValue().withS(value)));

        var daoList = new ArrayList<Dao>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        do {
            if (nonNull(lastEvaluatedKey)) {
                queryRequest = queryRequest.withExclusiveStartKey(lastEvaluatedKey);
            }
            var queryResult = client.query(queryRequest);
            var currentPageItems = queryResult.getItems().stream()
                                             .map(item -> parseAttributeValuesMap(item, Dao.class))
                                             .toList();
            daoList.addAll(currentPageItems);

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();

        } while (nonNull(lastEvaluatedKey));

        return daoList;
    }

    private static boolean isNotRemoved(TicketEntry ticket) {
        return !TicketStatus.REMOVED.equals(ticket.getStatus());
    }

    private static Optional<Resource> extractResource(Collection<Map<String, AttributeValue>> entries) {
        return entries.stream()
                   .filter(map -> hasTypeProperty(map, ResourceDao.TYPE))
                   .map(map -> parseAttributeValuesMap(map, Dao.class))
                   .filter(ResourceDao.class::isInstance)
                   .map(ResourceDao.class::cast)
                   .map(ResourceDao::getResource)
                   .findFirst();
    }

    private static boolean hasTypeProperty(Map<String, AttributeValue> map, String type) {
        return type.equals(map.getOrDefault("type", new AttributeValue()).getS());
    }

    private static List<FileEntry> extractFileEntries(Collection<Map<String, AttributeValue>> entries) {
        return entries.stream()
                   .filter(map -> hasTypeProperty(map, FileDao.TYPE))
                   .map(map -> parseAttributeValuesMap(map, Dao.class))
                   .filter(FileDao.class::isInstance)
                   .map(FileDao.class::cast)
                   .map(FileDao::getFileEntry)
                   .filter(ReadResourceService::isNotSoftDeleted)
                   .toList();
    }

    private List<PublicationChannel> extractPublicationChannels(Collection<Map<String, AttributeValue>> entries) {
        return entries.stream()
                   .filter(map -> hasTypeProperty(map, PublicationChannelDao.TYPE))
                   .map(map -> parseAttributeValuesMap(map, Dao.class))
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
        var queryObject = new ResourceDao(resourceQueryObjectWithAdditionalIdentifier(cristinIdentifier));
        var queryRequest = queryObject.createQueryFindByCristinIdentifier();
        var queryResult = client.query(queryRequest);
        return queryResultToListOfPublications(queryResult);
    }

    public List<Publication> getPublicationsByScopusIdentifier(String scopusIdentifier) {
        var queryRequest = new QueryRequest().withTableName(tableName)
                               .withIndexName(GSI_1_INDEX_NAME)
                               .withKeyConditionExpression("#PK1 = :value")
                               .withExpressionAttributeNames(Map.of("#PK1", "PK1"))
                               .withExpressionAttributeValues(Map.of(":value",
                                                                     new AttributeValue(String.format("%s:%s", SCOPUS_IDENTIFIER_INDEX_FIELD_PREFIX, scopusIdentifier))));
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

    private Resource resourceQueryObjectWithAdditionalIdentifier(String identifier) {
        var resource = new Resource();
        resource.setAdditionalIdentifiers(
            Set.of(new AdditionalIdentifier(ADDITIONAL_IDENTIFIER_CRISTIN, identifier)));
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

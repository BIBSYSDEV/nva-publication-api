package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.NoResponseException;
import no.unit.nva.publication.exception.NotImplementedException;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.PublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.utils.Environment;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DynamoDBPublicationService implements PublicationService {

    public static final String TABLE_NAME_ENV = "TABLE_NAME";
    public static final String BY_PUBLISHER_INDEX_NAME_ENV = "BY_PUBLISHER_INDEX_NAME";
    public static final String DYNAMODB_KEY_DELIMITER = "#";
    public static final String ERROR_READING_FROM_DYNAMO_DB = "Error reading from DynamoDB";

    private final Index byPublisherIndex;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for DynamoDBPublicationService.
     */
    public DynamoDBPublicationService(ObjectMapper objectMapper, Index byPublisherIndex) {
        this.objectMapper = objectMapper;
        this.byPublisherIndex = byPublisherIndex;
    }

    /**
     * Constructor for DynamoDBPublicationService.
     */
    public DynamoDBPublicationService(AmazonDynamoDB client, ObjectMapper objectMapper, Environment environment) {
        String tableName = environment.readEnv(TABLE_NAME_ENV);
        String byPublisherIndexName = environment.readEnv(BY_PUBLISHER_INDEX_NAME_ENV);
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(tableName);

        this.byPublisherIndex = table.getIndex(byPublisherIndexName);
        this.objectMapper = objectMapper;
    }

    @Override
    public Publication getPublication(UUID identifier, String authorization)  throws ApiGatewayException {
        throw new NotImplementedException();
    }

    @Override
    public Publication updatePublication(Publication publication, String authorization) throws ApiGatewayException {
        throw new NotImplementedException();
    }

    @Override
    public List<PublicationSummary> getPublicationsByPublisher(URI publisherId, String authorization)
            throws ApiGatewayException {
        throw new NotImplementedException();
    }

    @Override
    public List<PublicationSummary> getPublicationsByOwner(String owner, URI publisherId, String authorization)
            throws ApiGatewayException{
        allFieldsAreNonNull(owner, publisherId, authorization);

        String publisherOwner = String.join(DYNAMODB_KEY_DELIMITER, publisherId.toString(), owner);

        Map<String, String> nameMap = Map.of(
            "#publisherId", "publisherId",
            "#publisherOwnerDate", "publisherOwnerDate");
        Map<String, Object> valueMap = Map.of(
            ":publisherId", publisherId.toString(),
            ":publisherOwner", publisherOwner);

        QuerySpec querySpec = new QuerySpec()
            .withKeyConditionExpression(
                "#publisherId = :publisherId and begins_with(#publisherOwnerDate, :publisherOwner)")
            .withNameMap(nameMap)
            .withValueMap(valueMap);

        ItemCollection<QueryOutcome> items;
        try {
            items = byPublisherIndex.query(querySpec);
        } catch (Exception e) {
            throw new NoResponseException(ERROR_READING_FROM_DYNAMO_DB, e);
        }

        List<PublicationSummary> publications = parseJsonToPublicationSummaries(items);
        return filterOutOlderVersionsOfPublications(publications);
    }

    private List<PublicationSummary> parseJsonToPublicationSummaries(ItemCollection<QueryOutcome> items) {
        List<PublicationSummary> publications = new ArrayList<>();
        items.forEach(item -> toPublicationSummary(item).ifPresent(publications::add));
        return publications;
    }

    protected static List<PublicationSummary> filterOutOlderVersionsOfPublications(
        List<PublicationSummary> publications) {
        return publications.stream()
                           .collect(groupByIdentifer())
                           .entrySet()
                           .parallelStream()
                           .flatMap(DynamoDBPublicationService::pickNewestVersion)
                           .collect(Collectors.toList());
    }

    private static Collector<PublicationSummary, ?, Map<UUID, List<PublicationSummary>>> groupByIdentifer() {
        return Collectors.groupingBy(PublicationSummary::getIdentifier);
    }

    private static Stream<PublicationSummary> pickNewestVersion(Map.Entry<UUID, List<PublicationSummary>> group) {
        List<PublicationSummary> publications = group.getValue();
        Optional<PublicationSummary> mostRecent = publications.stream()
                                                              .max(Comparator.comparing(
                                                                  PublicationSummary::getModifiedDate));
        return mostRecent.stream();
    }

    private void allFieldsAreNonNull(String owner, URI publisherId, String authorization) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(publisherId);
        Objects.isNull(authorization);
    }

    protected Optional<PublicationSummary> toPublicationSummary(Item item) {
        try {
            PublicationSummary publicationSummary;
            publicationSummary = objectMapper.readValue(item.toJSON(), PublicationSummary.class);
            return Optional.of(publicationSummary);
        } catch (JsonProcessingException e) {
            System.out.println(e.getMessage());
            return Optional.empty();
        }
    }
}

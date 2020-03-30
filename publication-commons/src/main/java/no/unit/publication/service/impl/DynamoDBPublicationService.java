package no.unit.publication.service.impl;

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
import no.unit.publication.Environment;
import no.unit.publication.Logger;
import no.unit.nva.model.Publication;
import no.unit.publication.model.PublicationSummary;
import no.unit.publication.service.PublicationService;
import no.unit.publication.PublicationHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DynamoDBPublicationService implements PublicationService {

    public static final String NOT_IMPLEMENTED = "Not implemented";
    public static final String TABLE_NAME_ENV = "TABLE_NAME";
    public static final String BY_PUBLISHER_INDEX_NAME_ENV = "BY_PUBLISHER_INDEX_NAME";
    public static final String DYNAMODB_KEY_DELIMITER = "#";

    private final Index byPublisherIndex;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for DynamoDBPublicationService.
     *
     */
    public DynamoDBPublicationService(ObjectMapper objectMapper, Index byPublisherIndex) {
        this.objectMapper = objectMapper;
        this.byPublisherIndex = byPublisherIndex;
    }

    /**
     * Constructor for DynamoDBPublicationService.
     *
     */
    public DynamoDBPublicationService(AmazonDynamoDB client, ObjectMapper objectMapper, Environment environment) {
        String tableName = environment.get(TABLE_NAME_ENV).orElseThrow(
            () -> new IllegalStateException(PublicationHandler.ENVIRONMENT_VARIABLE_NOT_SET + TABLE_NAME_ENV));

        String byPublisherIndexName = environment.get(BY_PUBLISHER_INDEX_NAME_ENV).orElseThrow(
            () -> new IllegalStateException(PublicationHandler.ENVIRONMENT_VARIABLE_NOT_SET + BY_PUBLISHER_INDEX_NAME_ENV));

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(tableName);

        this.byPublisherIndex = table.getIndex(byPublisherIndexName);
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Publication> getPublication(UUID identifier, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public Publication updatePublication(Publication publication, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public List<PublicationSummary> getPublicationsByPublisher(URI publisherId, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public List<PublicationSummary> getPublicationsByOwner(String owner, URI publisherId, String authorization) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(publisherId);
        Objects.isNull(authorization);

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

        ItemCollection<QueryOutcome> items = byPublisherIndex.query(querySpec);

        List<PublicationSummary> publications = new ArrayList<>();
        items.forEach(item -> toPublicationSummary(item).ifPresent(publications::add));

        return publications;
    }

    protected Optional<PublicationSummary> toPublicationSummary(Item item) {
        try {
            PublicationSummary publicationSummary;
            publicationSummary = objectMapper.readValue(item.toJSON(), PublicationSummary.class);
            return Optional.of(publicationSummary);
        } catch (JsonProcessingException e) {
            Logger.logError(e);
            return Optional.empty();
        }
    }
}

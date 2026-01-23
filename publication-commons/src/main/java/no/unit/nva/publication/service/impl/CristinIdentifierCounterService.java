package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.publication.model.storage.CounterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class CristinIdentifierCounterService implements CounterService {

    public static final String UPDATING_COUNTER_EXCEPTION_MESSAGE = "Failed to update counter: {}";
    private static final String COLUMN_NAME = "value";
    private static final String ONE = "1";
    private final DynamoDbClient client;
    private final Logger logger = LoggerFactory.getLogger(CristinIdentifierCounterService.class);
    private final String tableName;

    protected CristinIdentifierCounterService(DynamoDbClient client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    @Override
    public CounterDao fetch() {
        return attempt(() -> CounterDao.toGetItemRequest(tableName)).map(client::getItem)
                   .map(CounterDao::fromGetItemResponse)
                   .map(a -> a.orElse(new CounterDao(10_000_000)))
                   .orElseThrow();
    }

    private CounterDao incrementAndReturn() {
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#ctr", COLUMN_NAME);

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":inc", AttributeValue.builder().n(ONE).build());

        var updateItemRequest = UpdateItemRequest.builder()
                                    .tableName(tableName)
                                    .key(CounterDao.primaryKey())
                                    .updateExpression("ADD #ctr :inc")
                                    .expressionAttributeNames(expressionAttributeNames)
                                    .expressionAttributeValues(expressionAttributeValues)
                                    .conditionExpression(keyExistsCondition())
                                    .returnValues(ReturnValue.UPDATED_NEW)
                                    .build();

        var updateItemResponse = client.updateItem(updateItemRequest);
        var updatedCount = updateItemResponse.attributes().get(COLUMN_NAME);
        return CounterDao.fromValue(Integer.parseInt(updatedCount.n()));
    }

    public CounterDao fetchCount(DynamoDbClient client) {
        var getItemRequest = GetItemRequest.builder()
                                 .tableName(tableName)
                                 .key(CounterDao.primaryKey())
                                 .projectionExpression("#ctr")
                                 .expressionAttributeNames(Map.of("#ctr", COLUMN_NAME))
                                 .build();

        var counter = client.getItem(getItemRequest);
        return CounterDao.fromGetItemResponse(counter).orElseThrow();
    }

    @Override
    public CounterDao next() {
        return attempt(this::incrementAndReturn).orElse(failure -> {
            logger.error(UPDATING_COUNTER_EXCEPTION_MESSAGE, failure.getException().getMessage());
            // Assuming the counter does not exist, we create it
            createCounterStartingAt(client);
            return fetchCount(client);
        });
    }

    private void createCounterStartingAt(DynamoDbClient client) {
        Map<String, AttributeValue> item = new HashMap<>(CounterDao.primaryKey());
        item.put(COLUMN_NAME, AttributeValue.builder().n(String.valueOf(10_000_000)).build());

        var putItemRequest = Put.builder()
                                 .tableName(tableName)
                                 .item(item)
                                 .conditionExpression(keyNotExistsCondition())
                                 .build();

        client.transactWriteItems(
            TransactWriteItemsRequest.builder()
                .transactItems(TransactWriteItem.builder().put(putItemRequest).build())
                .build());
    }

    private static String keyNotExistsCondition() {
        return String.format("attribute_not_exists(%s) AND attribute_not_exists(%s)", PRIMARY_KEY_PARTITION_KEY_NAME,
                             PRIMARY_KEY_SORT_KEY_NAME);
    }

    private static String keyExistsCondition() {
        return String.format("attribute_exists(%s) AND attribute_exists(%s)", PRIMARY_KEY_PARTITION_KEY_NAME,
                             PRIMARY_KEY_SORT_KEY_NAME);
    }
}

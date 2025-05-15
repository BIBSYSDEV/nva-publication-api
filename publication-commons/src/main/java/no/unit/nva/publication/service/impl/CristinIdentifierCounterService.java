package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.publication.model.storage.CounterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinIdentifierCounterService implements CounterService {

    public static final String UPDATING_COUNTER_EXCEPTION_MESSAGE = "Failed to update counter";
    private static final String COLUMN_NAME = "value";
    private static final String ONE = "1";
    private final AmazonDynamoDB client;
    private final Logger logger = LoggerFactory.getLogger(CristinIdentifierCounterService.class);
    private final String tableName;

    protected CristinIdentifierCounterService(AmazonDynamoDB client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    @Override
    public CounterDao fetch() {
        return attempt(() -> CounterDao.toGetItemRequest(tableName)).map(client::getItem)
                   .map(CounterDao::fromGetItemResult)
                   .map(a -> a.orElse(new CounterDao(10_000_000)))
                   .orElseThrow();
    }

    private CounterDao incrementAndReturn() {
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#ctr", COLUMN_NAME);

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":inc", new AttributeValue().withN(ONE));

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                                                  .withTableName(tableName)
                                                  .withKey(CounterDao.primaryKey())
                                                  .withUpdateExpression("ADD #ctr :inc")
                                                  .withExpressionAttributeNames(expressionAttributeNames)
                                                  .withExpressionAttributeValues(expressionAttributeValues)
                                                  .withConditionExpression(keyExistsCondition())
                                                  .withReturnValues("UPDATED_NEW");

        var updateItemResult = client.updateItem(updateItemRequest);
        AttributeValue updatedCount = updateItemResult.getAttributes().get(COLUMN_NAME);
        return CounterDao.fromValue(Integer.parseInt(updatedCount.getN()));
    }

    public CounterDao fetchCount(AmazonDynamoDB client) {
        GetItemRequest getItemRequest = new GetItemRequest().withTableName(tableName)
                                            .withKey(CounterDao.primaryKey())
                                            .withProjectionExpression("#ctr")
                                            .withExpressionAttributeNames(Map.of("#ctr", COLUMN_NAME));

        var counter = client.getItem(getItemRequest);
        return CounterDao.fromGetItemResult(counter).orElseThrow();
    }

    @Override
    public CounterDao increment() {
        return attempt(this::incrementAndReturn).orElse(failure -> {
            logger.error(UPDATING_COUNTER_EXCEPTION_MESSAGE, failure.getException());
            // Assuming the counter does not exist, we create it
            createCounterStartingAt(client);
            return fetchCount(client);
        });
    }

    private void createCounterStartingAt(AmazonDynamoDB client) {
        Map<String, AttributeValue> item = new HashMap<>(CounterDao.primaryKey());
        item.put(COLUMN_NAME, new AttributeValue().withN(String.valueOf(10_000_000)));

        Put putItemRequest = new Put().withTableName(tableName)
                                 .withItem(item)
                                 .withConditionExpression(keyNotExistsCondition());

        client.transactWriteItems(
            new TransactWriteItemsRequest().withTransactItems(new TransactWriteItem().withPut(putItemRequest)));
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

package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PARTITION_KEY_VALUE_PLACEHOLDER;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.SORT_KEY_VALUE_PLACEHOLDER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import no.unit.nva.publication.service.impl.CounterService;
import nva.commons.core.Environment;

public record CounterDao(@JsonProperty("value") int value) {

    public static final String COUNTER = "Counter";
    public static final String PARTITION_KEY_PLACEHOLDER = "#partitionKey";
    public static final String PARTITION_KEY_VALUE = "PK0";
    public static final String SORT_KEY_PLACEHOLDER = "#sortKey";
    public static final String SORT_KEY_VALUE = "SK0";
    public static final String VALUE_PLACEHOLDER = "#value";
    public static final String VALUE = "value";

    public static CounterDao fromValue(int value) {
        return new CounterDao(value);
    }

    public static GetItemRequest toGetItemRequest() {
        return new GetItemRequest().withTableName(getTableName()).withKey(primaryKey());
    }

    public static CounterDao fromGetItemResult(GetItemResult getItemResult) {
        return attempt(() -> ItemUtils.toItem(getItemResult.getItem())).map(Item::toJSON)
                   .map(CounterDao::toDao)
                   .orElseThrow();
    }

    public static CounterDao fetch(CounterService counterService) {
        return counterService.fetch();
    }

    public PutItemRequest toCreateTransactionWriteItem() {
        return new PutItemRequest().withItem(this.toDynamoFormat())
                   .withTableName(getTableName())
                   .withConditionExpression(KEY_NOT_EXISTS_CONDITION)
                   .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES);
    }

    public TransactWriteItem toPutTransactionWriteItem() {
        var attributes = Map.of(PARTITION_KEY_PLACEHOLDER, PARTITION_KEY_VALUE, SORT_KEY_PLACEHOLDER, SORT_KEY_VALUE,
                                VALUE_PLACEHOLDER, VALUE);
        var put = new Put().withItem(this.toDynamoFormat())
                   .withTableName(getTableName())
                   .withConditionExpression(newUniqueCounterConditionExpression())
                   .withExpressionAttributeNames(attributes)
                   .withExpressionAttributeValues(attributeValues());
        return new TransactWriteItem().withPut(put);
    }

    public void insert(CounterService counterService) {
        counterService.insert(value);
    }

    public CounterDao increment(CounterService counterService) {
        return counterService.increment();
    }

    public CounterDao increment() {
        return new CounterDao(value + 1);
    }

    private static String newUniqueCounterConditionExpression() {
        return "#partitionKey = :partitionKey AND #sortKey = :sortKey AND #value <> :newValue";
    }

    private static CounterDao toDao(String value) throws JsonProcessingException {
        return dynamoDbObjectMapper.readValue(value, CounterDao.class);
    }

    private static Map<String, AttributeValue> primaryKey() {
        return Map.of(PRIMARY_KEY_PARTITION_KEY_NAME, attributeValue(), PRIMARY_KEY_SORT_KEY_NAME, attributeValue());
    }

    private static AttributeValue attributeValue() {
        return new AttributeValue(COUNTER);
    }

    private static String getTableName() {
        return new Environment().readEnv("TABLE_NAME");
    }

    private Map<String, AttributeValue> attributeValues() {
        return Map.of(PARTITION_KEY_VALUE_PLACEHOLDER, attributeValue(), SORT_KEY_VALUE_PLACEHOLDER, attributeValue(),
                      ":newValue", new AttributeValue().withN(String.valueOf(value)));
    }

    private Map<String, AttributeValue> toDynamoFormat() {
        return attempt(() -> Item.fromJSON(dynamoDbObjectMapper.writeValueAsString(this))).map(
            ItemUtils::toAttributeValues).orElseThrow();
    }

    @JsonProperty("PK0")
    private String getPrimaryKey() {
        return COUNTER;
    }

    @JsonProperty("SK0")
    private String getSortKey() {
        return COUNTER;
    }
}

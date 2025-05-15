package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;

public record CounterDao(@JsonProperty("value") int value) {

    private static final String RESOURCE_COUNTER = "ResourceCounter";
    private static final AttributeValue RESOURCE_COUNTER_ATTR = new AttributeValue().withS(RESOURCE_COUNTER);
    private static final Map<String, AttributeValue> COUNTER_KEY = Map.of(PRIMARY_KEY_PARTITION_KEY_NAME,
                                                                          RESOURCE_COUNTER_ATTR,
                                                                          PRIMARY_KEY_SORT_KEY_NAME,
                                                                          RESOURCE_COUNTER_ATTR);

    public static CounterDao fromValue(int value) {
        return new CounterDao(value);
    }

    public static GetItemRequest toGetItemRequest(String tableName) {
        return new GetItemRequest().withTableName(tableName).withKey(primaryKey());
    }

    public static Optional<CounterDao> fromGetItemResult(GetItemResult getItemResult) {
        return attempt(() -> ItemUtils.toItem(getItemResult.getItem())).map(Item::toJSON)
                   .map(CounterDao::toDao)
                   .toOptional();
    }

    private static CounterDao toDao(String value) throws JsonProcessingException {
        return dynamoDbObjectMapper.readValue(value, CounterDao.class);
    }

    public static Map<String, AttributeValue> primaryKey() {
        return COUNTER_KEY;
    }
}

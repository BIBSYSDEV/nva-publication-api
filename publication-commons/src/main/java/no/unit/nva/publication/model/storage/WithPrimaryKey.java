package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;

public interface WithPrimaryKey {

    /**
     * Returns the value for the Partition key of the DynamoDB table (primary key), not the key of any index.
     *
     * @return a String with value of the partition key of the DynamoDB table.
     */
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    String getPrimaryKeyPartitionKey();

    @JacocoGenerated
    default void setPrimaryKeyPartitionKey(String key) {
        // do nothing
    }

    /**
     * Returns the value for the Sort key of the DynamoDB table (primary key), not the key of any index.
     *
     * @return a String with value of the sort key of the DynamoDB table.
     */
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    String getPrimaryKeySortKey();

    @JacocoGenerated
    default void setPrimaryKeySortKey(String key) {
        // do nothing
    }

    /**
     * Returns a Map of field-name:fieldValue for the primary key of the DynamoDB table. It's intended use is primarily
     * to get a specific item
     *
     * <p>Example:
     *
     * <p>{@code
     * GetItemRequest getItemRequest = GetItemRequest.builder()
     *     .tableName(tableName)
     *     .key(dao.primaryKey())
     *     .build();
     * GetItemResponse queryResult = client.getItem(getItemRequest);
     * Map<String, AttributeValue> item = queryResult.item();
     * ResourceDao resourceDao = parseAttributeValuesMap(item, ResourceDao.class);
     * }
     *
     * @return a Map with field-name:field-value pairs.
     */
    @JsonIgnore
    default Map<String, AttributeValue> primaryKey() {
        final Map<String, AttributeValue> map = new ConcurrentHashMap<>();
        var partKeyValue = AttributeValue.builder().s(getPrimaryKeyPartitionKey()).build();
        var sortKeyValue = AttributeValue.builder().s(getPrimaryKeySortKey()).build();
        map.put(PRIMARY_KEY_PARTITION_KEY_NAME, partKeyValue);
        map.put(PRIMARY_KEY_SORT_KEY_NAME, sortKeyValue);
        return map;
    }

    @JsonIgnore
    default Map<String, Condition> primaryKeyPartitionKeyCondition() {
        var condition = Condition.builder()
                            .comparisonOperator(ComparisonOperator.EQ)
                            .attributeValueList(AttributeValue.builder().s(getPrimaryKeyPartitionKey()).build())
                            .build();
        return Map.of(PRIMARY_KEY_PARTITION_KEY_NAME, condition);
    }
}


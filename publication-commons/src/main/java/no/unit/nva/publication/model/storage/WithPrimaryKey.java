package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nva.commons.core.JacocoGenerated;

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
     * GetItemRequest getItemRequest = new GetItemRequest() .withTableName(tableName) .withKey(dao.primaryKey());
     * GetItemResult queryResult=client.getItem(getItemRequest); Map<String, AttributeValue> item =
     * queryResult.getItem(); ResourceDao resourceDao = parseAttributeValuesMap(item,ResourceDao.class); }
     *
     * @return a Map with field-name:field-value pairs.
     */
    @JsonIgnore
    default Map<String, AttributeValue> primaryKey() {
        final Map<String, AttributeValue> map = new ConcurrentHashMap<>();
        AttributeValue partKeyValue = new AttributeValue(getPrimaryKeyPartitionKey());
        AttributeValue sortKeyValue = new AttributeValue(getPrimaryKeySortKey());
        map.put(PRIMARY_KEY_PARTITION_KEY_NAME, partKeyValue);
        map.put(PRIMARY_KEY_SORT_KEY_NAME, sortKeyValue);
        return map;
    }
    
    @JsonIgnore
    default Map<String, Condition> primaryKeyPartitionKeyCondition() {
        Condition condition = new Condition()
            .withComparisonOperator(ComparisonOperator.EQ)
            .withAttributeValueList(new AttributeValue(getPrimaryKeyPartitionKey()));
        return Map.of(PRIMARY_KEY_PARTITION_KEY_NAME, condition);
    }
}


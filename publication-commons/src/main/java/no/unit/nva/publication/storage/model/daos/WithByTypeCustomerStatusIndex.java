package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.CUSTOMER_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.STATUS_INDEX_FIELD_PREFIX;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;

public interface WithByTypeCustomerStatusIndex {
    
    static String formatByTypeCustomerStatusPartitionKey(String type, String status, URI customerUri) {
        String customerIdentifier = Dao.orgUriToOrgIdentifier(customerUri);
        return type
               + KEY_FIELDS_DELIMITER
               + CUSTOMER_INDEX_FIELD_PREFIX
               + KEY_FIELDS_DELIMITER
               + customerIdentifier
               + KEY_FIELDS_DELIMITER
               + STATUS_INDEX_FIELD_PREFIX
               + KEY_FIELDS_DELIMITER
               + status;
    }
    
    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME)
    String getByTypeCustomerStatusPartitionKey();
    
    default void setByTypeCustomerStatusPartitionKey(String byTypeCustomerStatusPartitionKey) {
        //Do nothing
    }
    
    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME)
    String getByTypeCustomerStatusSortKey();
    
    default void setByTypeCustomerStatusSortKey(String byTypeCustomerStatusSk) {
        // do nothing
    }
    
    /**
     * Returns a Map of field-name:Condition for the key of the By-Type-Customer-Status index table. It's intended use
     * is primarily to get one specific item (by query) if one has the identifier of the entry. It provides an
     * alternative read access pattern to entries based on their Status.
     *
     * <p>Example:
     *
     * <p>{@code
     * new QueryRequest() .withTableName(RESOURCES_TABLE_NAME) .withIndexName(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME)
     * .withKeyConditions(dao.byTypeCustomerStatusKey());
     * <p>
     * }
     *
     * @return a Map with field-name:Condition pair.
     */
    default Map<String, Condition> fetchEntryByTypeCustomerStatusKey() {
        Condition partitionKeyCondition = equalityIndexKeyCondition(getByTypeCustomerStatusPartitionKey());
        Condition sortKeyCondition = equalityIndexKeyCondition(getByTypeCustomerStatusSortKey());
        return
            Map.of(
                BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME, partitionKeyCondition,
                BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME, sortKeyCondition
            );
    }
    
    default Map<String, Condition> fetchEntryCollectionByTypeCustomerStatusKey() {
        Condition partitionKeyCondition = equalityIndexKeyCondition(getByTypeCustomerStatusPartitionKey());
        return
            Map.of(
                BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME, partitionKeyCondition
            );
    }
    
    private static Condition equalityIndexKeyCondition(String keyValue) {
        return new Condition()
            .withAttributeValueList(new AttributeValue(keyValue))
            .withComparisonOperator(ComparisonOperator.EQ);
    }
}

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

    default Map<String, Condition> byTypeCustomerStatusKey() {
        Condition partitionKeyCondition = equalityIndexKeyCondition(getByTypeCustomerStatusPartitionKey());
        Condition sortKeyCondition = equalityIndexKeyCondition(getByTypeCustomerStatusSortKey());
        return
            Map.of(
                BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME, //key
                partitionKeyCondition, //value
                BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME,  //key
                sortKeyCondition //value
            );
    }

    private static Condition equalityIndexKeyCondition(String keyvalue) {
        return new Condition()
            .withAttributeValueList(new AttributeValue(keyvalue))
            .withComparisonOperator(ComparisonOperator.EQ);
    }

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
}

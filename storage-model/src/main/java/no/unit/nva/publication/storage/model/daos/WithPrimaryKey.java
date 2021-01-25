package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface WithPrimaryKey {

    String getPrimaryKeyPartitionKey();

    String getPrimaryKeySortKey();

    @JsonIgnore
    default Map<String, AttributeValue> primaryKey() {
        final Map<String, AttributeValue> map = new ConcurrentHashMap<>();
        AttributeValue partKeyValue = new AttributeValue(getPrimaryKeyPartitionKey());
        AttributeValue sortKeyValue = new AttributeValue(getPrimaryKeySortKey());
        map.put(PRIMARY_KEY_PARTITION_KEY_NAME, partKeyValue);
        map.put(PRIMARY_KEY_SORT_KEY_NAME, sortKeyValue);
        return map;
    }
}


package no.unit.nva.publication.storage.model.daos;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;

public interface WithPrimaryKey {

    String getPrimaryKeyPartitionKey();

    String getPrimaryKeySortKey();

    @JsonIgnore
    Map<String, AttributeValue> primaryKey();
}

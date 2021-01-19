package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import nva.commons.core.JacocoGenerated;

public class IdentifierEntry implements WithPrimaryKey {

    public static final String ILLEGAL_ACCESS_PATTERN_ERROR = "IdentifierEntries are not supposed to be altered or "
        + "deserialized";
    private static String TYPE = "IdEntry#";
    private String partitionKey;
    private String sortKey;

    /*For JSON Jackson*/
    @JacocoGenerated
    public IdentifierEntry() {

    }

    public IdentifierEntry(String identifier) {
        this.partitionKey = TYPE + identifier;
        this.sortKey = TYPE + identifier;
    }

    @Override
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getPrimaryKeyPartitionKey() {
        return partitionKey;
    }

    public void setPrimaryKeyPartitionKey(String partitionKey) {
        throw new IllegalStateException(ILLEGAL_ACCESS_PATTERN_ERROR);
    }

    @Override
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public String getPrimaryKeySortKey() {
        return sortKey;
    }

    @Override
    public Map<String, AttributeValue> primaryKey() {
        return Map.of(
            PRIMARY_KEY_PARTITION_KEY_NAME, new AttributeValue(getPrimaryKeyPartitionKey()),
            PRIMARY_KEY_SORT_KEY_NAME, new AttributeValue(getPrimaryKeySortKey())
        );
    }

    public void setPrimaryKeySortKey(String sortKey) {
        throw new IllegalStateException(ILLEGAL_ACCESS_PATTERN_ERROR);
    }
}

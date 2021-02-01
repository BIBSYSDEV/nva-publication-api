package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

public abstract class UniquenessEntry implements WithPrimaryKey {

    public static final String ILLEGAL_ACCESS_PATTERN_ERROR = "IdentifierEntries are not supposed to be read,"
                                                              + "altered or deserialized";

    private String partitionKey;
    private String sortKey;

    /*For JSON Jackson*/
    @JacocoGenerated
    public UniquenessEntry() {

    }

    public UniquenessEntry(String identifier) {
        this.partitionKey = getType() + identifier;
        this.sortKey = partitionKey;
    }

    @Override
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public final String getPrimaryKeyPartitionKey() {
        return partitionKey;
    }

    @Override
    public final void setPrimaryKeyPartitionKey(String partitionKey) {
        throw new IllegalStateException(ILLEGAL_ACCESS_PATTERN_ERROR);
    }

    @Override
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public final String getPrimaryKeySortKey() {
        return sortKey;
    }

    @Override
    public final void setPrimaryKeySortKey(String sortKey) {
        throw new IllegalStateException(ILLEGAL_ACCESS_PATTERN_ERROR);
    }

    protected abstract String getType();
}

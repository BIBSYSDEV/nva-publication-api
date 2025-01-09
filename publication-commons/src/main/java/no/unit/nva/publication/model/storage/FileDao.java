package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.LogEntryDao.KEY_PATTERN;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeName(FileDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class FileDao extends Dao implements DynamoEntryByIdentifier {

    public static final String TYPE = "File";
    private final SortableIdentifier identifier;
    private final SortableIdentifier resourceIdentifier;
    private final Instant modifiedDate;

    private FileDao(FileEntry fileEntry) {
        super(fileEntry);
        this.identifier = fileEntry.getIdentifier();
        this.resourceIdentifier = fileEntry.getResourceIdentifier();
        this.modifiedDate = fileEntry.getModifiedDate();
    }

    public static Dao fromFileEntry(FileEntry fileEntry) {
        return new FileDao(fileEntry);
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getPrimaryKeyPartitionKey() {
        return KEY_PATTERN.formatted(Resource.TYPE, getResourceIdentifier());
    }

    @Override
    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    public String getPrimaryKeySortKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
    }

    @Override
    public String indexingType() {
        return TYPE;
    }

    @Override
    public URI getCustomerId() {
        return getData().getCustomerId();
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        return null;
    }

    @Override
    public void updateExistingEntry(AmazonDynamoDB client) {
        // To implement
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getIdentifier(), getResourceIdentifier(), getModifiedDate());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FileDao fileDao)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(getIdentifier(), fileDao.getIdentifier()) &&
               Objects.equals(getResourceIdentifier(), fileDao.getResourceIdentifier()) &&
               Objects.equals(getModifiedDate(), fileDao.getModifiedDate());
    }

    @Override
    protected User getOwner() {
        return getData().getOwner();
    }

    @Override
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    public String getByTypeAndIdentifierPartitionKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
    }

    @Override
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
    public String getByTypeAndIdentifierSortKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
    }
}

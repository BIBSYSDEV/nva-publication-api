package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_PK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_SK_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_FORMAT;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_FORMAT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")

@JsonSubTypes({
    @JsonSubTypes.Type(name = ResourceDao.TYPE, value = ResourceDao.class),
    @JsonSubTypes.Type(TicketDao.class),
    @JsonSubTypes.Type(name = MessageDao.TYPE, value = MessageDao.class)
})
public abstract class Dao
    implements DynamoEntry,
               WithPrimaryKey,
               DynamoEntryByIdentifier,
               WithByTypeCustomerStatusIndex {
    
    public static final String URI_PATH_SEPARATOR = "/";
    public static final String CONTAINED_DATA_FIELD_NAME = "data";
    public static final String UNSUPORTED_SET_IDENTIFIER_ERROR =
        "Daos cannot set their identifier. They get it from their contained data";
    
    public static String orgUriToOrgIdentifier(URI uri) {
        String[] pathParts = uri.getPath().split(URI_PATH_SEPARATOR);
        return pathParts[pathParts.length - 1];
    }
    
    /**
     * Filtering expression to be used when we need to scan the whole database and perform actions on every data entry.
     * This expression excludes Uniqueness entries (i.e. entries for guaranteeing the uniqueness of certain values * see
     * link below). This filter is used primarily when migrating the Resources table.
     * <p>
     * *
     * {@see * <a
     * href=https://aws.amazon.com/blogs/database/simulating-amazon-dynamodb-unique-constraints-using-transactions>
     * documentation </a>}
     *
     * @return filtering expression string.
     */
    public static String scanFilterExpressionForDataEntries() {
        return "begins_with (#PK, :Resource) or "
               + "begins_with(#PK, :Ticket) or "
               + "begins_with(#PK, :Message)";
    }
    
    // replaces the hash values in the filter expression with the actual key name
    public static Map<String, String> scanFilterExpressionAttributeNames() {
        return Map.of("#PK", DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME);
    }
    
    // replaces the colon values in the filter expression with the actual value
    public static Map<String, AttributeValue> scanFilterExpressionAttributeValues() {
        return Map.of(":Resource", new AttributeValue(ResourceDao.TYPE + KEY_FIELDS_DELIMITER),
            ":Ticket", new AttributeValue(TicketDao.TICKETS_INDEXING_TYPE + KEY_FIELDS_DELIMITER),
            ":Message", new AttributeValue(MessageDao.TYPE + KEY_FIELDS_DELIMITER)
        );
    }
    
    @Override
    public final String getPrimaryKeyPartitionKey() {
        return formatPrimaryPartitionKey(getCustomerId(), getOwner());
    }
    
    @Override
    @JacocoGenerated
    public final void setPrimaryKeyPartitionKey(String key) {
        // do nothing
    }
    
    @Override
    @JacocoGenerated
    public final String getPrimaryKeySortKey() {
        return String.format(PRIMARY_KEY_SORT_KEY_FORMAT, indexingType(), getIdentifier());
    }
    
    @Override
    @JacocoGenerated
    public final void setPrimaryKeySortKey(String key) {
        // do nothing
    }
    
    @Override
    @JsonProperty(CONTAINED_DATA_FIELD_NAME)
    public abstract Entity getData();
    
    public abstract void setData(Entity data);
    
    @Override
    public final String getByTypeCustomerStatusPartitionKey() {
        String publisherId = customerIdentifier();
        Optional<String> publicationStatus = extractStatus();
    
        return publicationStatus
                   .map(status -> formatByTypeCustomerStatusIndexPartitionKey(publisherId, status))
                   .orElse(null);
    }
    
    @Override
    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME)
    public final String getByTypeCustomerStatusSortKey() {
        //Codacy complains that identifier is already a String
        SortableIdentifier identifier = getData().getIdentifier();
        return String.format(BY_TYPE_CUSTOMER_STATUS_SK_FORMAT, this.indexingType(), identifier.toString());
    }
    
    @JsonIgnore
    public final String getCustomerIdentifier() {
        return orgUriToOrgIdentifier(getCustomerId());
    }
    
    @JsonIgnore
    @Override
    public abstract String indexingType();
    
    @JsonIgnore
    public abstract URI getCustomerId();
    
    @Override
    @JsonIgnore
    public SortableIdentifier getIdentifier() {
        return getData().getIdentifier();
    }
    
    @JacocoGenerated
    public final void setIdentifier(SortableIdentifier identifier) {
        throw new UnsupportedOperationException(UNSUPORTED_SET_IDENTIFIER_ERROR);
    }
    
    public abstract TransactWriteItemsRequest createInsertionTransactionRequest();
    
    public final String dataType() {
        return getData().getType();
    }
    
    protected String formatPrimaryPartitionKey(URI organizationUri, String userIdentifier) {
        String organizationIdentifier = orgUriToOrgIdentifier(organizationUri);
        return formatPrimaryPartitionKey(organizationIdentifier, userIdentifier);
    }
    
    protected String formatPrimaryPartitionKey(String publisherId, String owner) {
        return String.format(PRIMARY_KEY_PARTITION_KEY_FORMAT, indexingType(), publisherId, owner);
    }
    
    @JsonIgnore
    protected abstract String getOwner();
    
    private String formatByTypeCustomerStatusIndexPartitionKey(String publisherId, String status) {
        return String.format(BY_TYPE_CUSTOMER_STATUS_PK_FORMAT,
            indexingType(),
            publisherId,
            status);
    }
    
    private Optional<String> extractStatus() {
        return attempt(this::getData)
                   .map(Entity.class::cast)
                   .map(Entity::getStatusString)
                   .toOptional();
    }
    
    private String customerIdentifier() {
        return orgUriToOrgIdentifier(getCustomerId());
    }
}

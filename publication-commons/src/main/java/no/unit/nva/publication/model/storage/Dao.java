package no.unit.nva.publication.model.storage;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.storage.DataCompressor.compressDaoData;
import static no.unit.nva.publication.storage.model.DatabaseConstants.*;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")

@JsonSubTypes({@JsonSubTypes.Type(name = ResourceDao.TYPE, value = ResourceDao.class),
    @JsonSubTypes.Type(TicketDao.class), @JsonSubTypes.Type(name = MessageDao.TYPE, value = MessageDao.class),
    @JsonSubTypes.Type(name = FileDao.TYPE, value = FileDao.class),
    @JsonSubTypes.Type(name = PublicationChannelDao.TYPE, value = PublicationChannelDao.class)})
public abstract class Dao
    implements DynamoEntry, WithPrimaryKey, DynamoEntryByIdentifier, WithByTypeCustomerStatusIndex {

    public static final String URI_PATH_SEPARATOR = "/";
    public static final String VERSION_FIELD = "version";
    private Entity data;

    @JsonProperty(VERSION_FIELD)
    private UUID version;

    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;

    protected Dao() {

    }

    protected Dao(Entity data) {
        this.version = UUID.randomUUID();
        this.data = data;
    }

    public static String orgUriToOrgIdentifier(URI uri) {
        String[] pathParts = uri.getPath().split(URI_PATH_SEPARATOR);
        return pathParts[pathParts.length - 1];
    }

    /**
     * Filtering expression to be used when we need to scan the whole database and perform actions on every data entry.
     * This expression excludes Uniqueness entries (i.e. entries for guaranteeing the uniqueness of certain values * see
     * link below). This filter is used primarily when migrating the Resources table.
     *
     * <p>{@see * <a
     * href=https://aws.amazon.com/blogs/database/simulating-amazon-dynamodb-unique-constraints-using-transactions>
     * documentation </a>}
     *
     * @return filtering expression string.
     */
    public static String scanFilterExpressionForDataEntries(Collection<KeyField> types) {
        return getTypesOrDefault(types).map(Dao::toQueryPart).collect(Collectors.joining(" or \n"));
    }

    // replaces the hash values in the filter expression with the actual key name
    public static Map<String, String> scanFilterExpressionAttributeNames() {
        return Map.of("#PK", DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME);
    }

    // replaces the colon values in the filter expression with the actual value
    public static Map<String, AttributeValue> scanFilterExpressionAttributeValues(Collection<KeyField> types) {
        return getTypesOrDefault(types).map(Dao::createFilterExpression)
                   .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public UUID getVersion() {
        return version;
    }

    public void setVersion(UUID version) {
        this.version = version;
    }

    @Override
    public String getPrimaryKeyPartitionKey() {
        return formatPrimaryPartitionKey(getCustomerId(), getOwner().toString());
    }

    @Override
    @JacocoGenerated
    public final void setPrimaryKeyPartitionKey(String key) {
        // do nothing
    }

    @Override
    @JacocoGenerated
    public String getPrimaryKeySortKey() {
        return String.format(PRIMARY_KEY_SORT_KEY_FORMAT, indexingType(), getIdentifier());
    }

    @Override
    @JacocoGenerated
    public final void setPrimaryKeySortKey(String key) {
        // do nothing
    }

    @Override
    @JsonProperty(CONTAINED_DATA_FIELD_NAME)
    public final Entity getData() {
        return this.data;
    }

    public final void setData(Entity data) {
        this.data = data;
    }

    @JsonIgnore
    @Override
    public abstract String indexingType();

    @Override
    public String getByTypeCustomerStatusPartitionKey() {
        String publisherId = customerIdentifier();
        Optional<String> publicationStatus = extractStatus();

        return publicationStatus.map(status -> formatByTypeCustomerStatusIndexPartitionKey(publisherId, status))
                   .orElse(null);
    }

    @Override
    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME)
    public String getByTypeCustomerStatusSortKey() {
        //Codacy complains that identifier is already a String
        SortableIdentifier identifier = getData().getIdentifier();
        return String.format(BY_TYPE_CUSTOMER_STATUS_SK_FORMAT, this.indexingType(), identifier.toString());
    }

    @JsonIgnore
    public final String getCustomerIdentifier() {
        return orgUriToOrgIdentifier(getCustomerId());
    }

    @JsonIgnore
    public abstract URI getCustomerId();

    @Override
    public SortableIdentifier getIdentifier() {
        return this.identifier;
    }

    @JacocoGenerated
    public final void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public Map<String, AttributeValue> toDynamoFormat() {
        return attempt(() -> compressDaoData(this)).orElseThrow();
    }

    public abstract TransactWriteItemsRequest createInsertionTransactionRequest();

    public abstract void updateExistingEntry(AmazonDynamoDB client);

    public final String dataType() {
        return getData().getType();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getVersion());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dao dao)) {
            return false;
        }
        return Objects.equals(getData(), dao.getData()) && Objects.equals(getVersion(), dao.getVersion());
    }

    protected String formatPrimaryPartitionKey(URI organizationUri, String userIdentifier) {
        String organizationIdentifier = orgUriToOrgIdentifier(organizationUri);
        return formatPrimaryPartitionKey(organizationIdentifier, userIdentifier);
    }

    protected String formatPrimaryPartitionKey(String publisherId, String owner) {
        return String.format(PRIMARY_KEY_PARTITION_KEY_FORMAT, indexingType(), publisherId, owner);
    }

    @JsonIgnore
    protected abstract User getOwner();

    private static Stream<KeyField> getTypesOrDefault(Collection<KeyField> types) {
        return types.isEmpty() ? Stream.of(KeyField.values()) : types.stream();
    }

    private static String toQueryPart(KeyField type) {
        return "begins_with (#PK, " + type.getKeyField() + ")";
    }

    private static Entry<String, AttributeValue> createFilterExpression(KeyField keyField) {
        return switch (keyField) {
            case RESOURCE ->
                Map.entry(keyField.getKeyField(), new AttributeValue(ResourceDao.TYPE + KEY_FIELDS_DELIMITER));
            case MESSAGE ->
                Map.entry(keyField.getKeyField(), new AttributeValue(MessageDao.TYPE + KEY_FIELDS_DELIMITER));
            case TICKET -> Map.entry(keyField.getKeyField(),
                                     new AttributeValue(TicketDao.TICKETS_INDEXING_TYPE + KEY_FIELDS_DELIMITER));
            case DOI_REQUEST ->
                Map.entry(keyField.getKeyField(), new AttributeValue("DoiRequest" + KEY_FIELDS_DELIMITER));
            case FILE_ENTRY ->
                Map.entry(keyField.getKeyField(), new AttributeValue("File" + KEY_FIELDS_DELIMITER));
        };
    }

    private String formatByTypeCustomerStatusIndexPartitionKey(String publisherId, String status) {
        return String.format(BY_TYPE_CUSTOMER_STATUS_PK_FORMAT, indexingType(), publisherId, status);
    }

    private Optional<String> extractStatus() {
        return attempt(this::getData).map(Entity::getStatusString).toOptional();
    }

    private String customerIdentifier() {
        return orgUriToOrgIdentifier(getCustomerId());
    }
}

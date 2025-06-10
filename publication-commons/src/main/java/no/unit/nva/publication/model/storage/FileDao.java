package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.model.storage.TicketDao.newPutTransactionItem;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(FileDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class FileDao extends Dao implements DynamoEntryByIdentifier, JoinWithResource {

    public static final String TYPE = "File";
    private static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "a";
    private static final String KEY_PATTERN = "%s:%s";

    @JsonProperty("identifier")
    private final SortableIdentifier identifier;
    private final SortableIdentifier resourceIdentifier;
    private final Instant modifiedDate;

    @JsonCreator
    public FileDao(@JsonProperty("identifier") SortableIdentifier identifier,
                   @JsonProperty("resourceIdentifier") SortableIdentifier resourceIdentifier,
                   @JsonProperty("modifiedDate") Instant modifiedDate,
                   @JsonProperty("data") FileEntry data) {
        super(data);
        this.identifier = identifier;
        this.resourceIdentifier = resourceIdentifier;
        this.modifiedDate = modifiedDate;
    }

    private FileDao(FileEntry fileEntry) {
        super(fileEntry);
        this.identifier = fileEntry.getIdentifier();
        this.resourceIdentifier = fileEntry.getResourceIdentifier();
        this.modifiedDate = fileEntry.getModifiedDate();
    }

    public static FileDao fromFileEntry(FileEntry fileEntry) {
        return new FileDao(fileEntry);
    }

    public static FileDao fromDynamoFormat(Map<String, AttributeValue> attributeValueMap) {
        return attempt(() -> ItemUtils.toItem(attributeValueMap)).map(Item::toJSON)
                   .map(json -> dynamoDbObjectMapper.readValue(json, FileDao.class))
                   .orElseThrow();
    }

    public static String getFileEntriesByResourceIdentifierPartitionKey(Resource resource) {
        return KEY_PATTERN.formatted(Resource.TYPE, resource.getIdentifier());
    }

    @Override
    @JsonProperty("resourceIdentifier")
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    public String getPrimaryKeyPartitionKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
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
    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME)
    public String getByTypeCustomerStatusPartitionKey() {
        return null;
    }

    @Override
    @JsonProperty(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME)
    public String getByTypeCustomerStatusSortKey() {
        return null;
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
    @JsonIgnore
    public String joinByResourceOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + getData().getType();
    }

    @Override
    public Map<String, AttributeValue> toDynamoFormat() {
        return attempt(() -> JsonUtils.dynamoObjectMapper.writeValueAsString(this)).map(Item::fromJSON)
                   .map(ItemUtils::toAttributeValues)
                   .orElseThrow();
    }

    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        return new TransactWriteItemsRequest().withTransactItems(newPutTransactionItem(this));
    }

    @Override
    public void updateExistingEntry(AmazonDynamoDB client) {
        var request = new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(toDynamoFormat());
        client.putItem(request);
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

    public TransactWriteItem toPutNewTransactionItem(String tableName) {
        var put = new Put()
                      .withItem(this.toDynamoFormat())
                      .withTableName(tableName)
                      .withConditionExpression(KEY_NOT_EXISTS_CONDITION)
                      .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES);
        return new TransactWriteItem().withPut(put);
    }

    public TransactWriteItem toPutTransactionItem(String tableName) {
        var put = new Put().withItem(this.toDynamoFormat()).withTableName(tableName);
        return new TransactWriteItem().withPut(put);
    }

    public TransactWriteItem toDeleteTransactionItem(String tableName) {
        var map = new ConcurrentHashMap<String, AttributeValue>();
        var partKeyValue = new AttributeValue(getPrimaryKeyPartitionKey());
        var sortKeyValue = new AttributeValue(getPrimaryKeySortKey());
        map.put(PRIMARY_KEY_PARTITION_KEY_NAME, partKeyValue);
        map.put(PRIMARY_KEY_SORT_KEY_NAME, sortKeyValue);
        var delete = new Delete()
                         .withTableName(tableName)
                         .withKey(map);
        return new TransactWriteItem().withDelete(delete);
    }

    @Override
    protected User getOwner() {
        return getData().getOwner();
    }

    @Override
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    public String getByTypeAndIdentifierPartitionKey() {
        return KEY_PATTERN.formatted(Resource.TYPE, getResourceIdentifier());
    }

    @Override
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
    public String getByTypeAndIdentifierSortKey() {
        return KEY_PATTERN.formatted(TYPE, getIdentifier());
    }

    public FileEntry getFileEntry() {
        return (FileEntry) getData();
    }
}

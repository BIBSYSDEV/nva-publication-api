package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.model.storage.TicketDao.newPutTransactionItem;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
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
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

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
        var document = EnhancedDocument.fromAttributeValueMap(attributeValueMap);
        return attempt(() -> dynamoDbObjectMapper.readValue(document.toJson(), FileDao.class))
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
        var json = attempt(() -> dynamoDbObjectMapper.writeValueAsString(this)).orElseThrow();
        return EnhancedDocument.fromJson(json).toMap();
    }

    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        return TransactWriteItemsRequest.builder().transactItems(newPutTransactionItem(this)).build();
    }

    @Override
    public void updateExistingEntry(DynamoDbClient client) {
        var request = PutItemRequest.builder().tableName(RESOURCES_TABLE_NAME).item(toDynamoFormat()).build();
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
        var put = Put.builder()
                      .item(this.toDynamoFormat())
                      .tableName(tableName)
                      .conditionExpression(KEY_NOT_EXISTS_CONDITION)
                      .expressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                      .build();
        return TransactWriteItem.builder().put(put).build();
    }

    public TransactWriteItem toPutTransactionItem(String tableName) {
        var put = Put.builder().item(this.toDynamoFormat()).tableName(tableName).build();
        return TransactWriteItem.builder().put(put).build();
    }

    public TransactWriteItem toDeleteTransactionItem(String tableName) {
        var map = new ConcurrentHashMap<String, AttributeValue>();
        var partKeyValue = AttributeValue.builder().s(getPrimaryKeyPartitionKey()).build();
        var sortKeyValue = AttributeValue.builder().s(getPrimaryKeySortKey()).build();
        map.put(PRIMARY_KEY_PARTITION_KEY_NAME, partKeyValue);
        map.put(PRIMARY_KEY_SORT_KEY_NAME, sortKeyValue);
        var delete = Delete.builder()
                         .tableName(tableName)
                         .key(map)
                         .build();
        return TransactWriteItem.builder().delete(delete).build();
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

    @JsonIgnore
    public FileEntry getFileEntry() {
        return (FileEntry) getData();
    }
}

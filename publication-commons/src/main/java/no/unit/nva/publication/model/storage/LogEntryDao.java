package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SuppressWarnings("PMD.UnusedPrivateMethod")
public record LogEntryDao(SortableIdentifier identifier, SortableIdentifier resourceIdentifier, Instant createdDate,
                          LogEntry data) {

    public static final String TYPE = "LogEntry";
    private static final String KEY_PATTERN = "%s:%s";

    public static LogEntryDao fromDynamoFormat(Map<String, AttributeValue> map) {
        var document = EnhancedDocument.fromAttributeValueMap(map);
        return attempt(() -> dynamoDbObjectMapper.readValue(document.toJson(), LogEntryDao.class))
                   .orElseThrow();
    }

    public static LogEntryDao fromLogEntry(LogEntry logEntry) {
        return new LogEntryDao(logEntry.identifier(), logEntry.resourceIdentifier(), Instant.now(), logEntry);
    }

    public static String getLogEntriesByResourceIdentifierPartitionKey(Resource resource) {
        return KEY_PATTERN.formatted(Resource.TYPE, resource.getIdentifier());
    }

    public Map<String, AttributeValue> toDynamoFormat() {
        var json = attempt(() -> dynamoDbObjectMapper.writeValueAsString(this)).orElseThrow();
        return EnhancedDocument.fromJson(json).toMap();
    }

    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    private String getByTypeAndIdentifierPartitionKey() {
        return KEY_PATTERN.formatted(TYPE, identifier());
    }

    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
    private String getByTypeAndIdentifierSortKey() {
        return KEY_PATTERN.formatted(TYPE, identifier());
    }

    @JsonProperty(PRIMARY_KEY_PARTITION_KEY_NAME)
    private String getPrimaryKeyPartitionKey() {
        return KEY_PATTERN.formatted(Resource.TYPE, resourceIdentifier());
    }

    @JsonProperty(PRIMARY_KEY_SORT_KEY_NAME)
    private String getPrimaryKeySortKey() {
        return KEY_PATTERN.formatted(TYPE, identifier());
    }

    @JsonProperty(BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME)
    private String getCustomerResourcePartitionKey() {
        return KEY_PATTERN.formatted(Resource.TYPE, resourceIdentifier());
    }

    @JsonProperty(BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME)
    private String getCustomerResourceSortKey() {
        return KEY_PATTERN.formatted(TYPE, identifier());
    }
}

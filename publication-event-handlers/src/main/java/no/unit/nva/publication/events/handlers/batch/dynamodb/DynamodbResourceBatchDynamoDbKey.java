package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.Map;
import nva.commons.core.JacocoGenerated;

public record DynamodbResourceBatchDynamoDbKey(String partitionKey, String sortKey, String indexName) {
    public DynamodbResourceBatchDynamoDbKey(String partitionKey, String sortKey) {
        this(partitionKey, sortKey, null);
    }

    public boolean isGsiQuery() {
        return indexName != null;
    }

    public Map<String, AttributeValue> toPrimaryKey() {
        if (isGsiQuery()) {
            throw new IllegalStateException("Cannot convert GSI key to primary key");
        }
        return Map.of(
            PRIMARY_KEY_PARTITION_KEY_NAME, new AttributeValue(partitionKey),
            PRIMARY_KEY_SORT_KEY_NAME, new AttributeValue(sortKey));
    }

    @JacocoGenerated
    @Override
    public String toString() {
        if (isGsiQuery()) {
            return String.format("DynamoDbKey{index='%s', pk='%s', sk='%s'}",
                                 indexName, partitionKey, sortKey);
        }
        return String.format("DynamoDbKey{pk='%s', sk='%s'}", partitionKey, sortKey);
    }
}
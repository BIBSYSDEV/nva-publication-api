package no.unit.nva.publication.events.handlers.batch.dynamodb;

import nva.commons.core.JacocoGenerated;

public record DynamodbResourceBatchDynamoDbKey(String partitionKey, String sortKey, String indexName) {
    public DynamodbResourceBatchDynamoDbKey(String partitionKey, String sortKey) {
        this(partitionKey, sortKey, null);
    }

    public boolean isGsiQuery() {
        return indexName != null;
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
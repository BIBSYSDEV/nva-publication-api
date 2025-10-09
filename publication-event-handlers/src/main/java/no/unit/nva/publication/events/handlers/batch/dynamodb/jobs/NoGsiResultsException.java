package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;

public class NoGsiResultsException extends RuntimeException {
    public NoGsiResultsException(DynamodbResourceBatchDynamoDbKey key) {
        super(String.format("No items found for GSI query on index: %s with partitionKey: %s and sortKey: %s",
                            key.indexName(), key.partitionKey(), key.sortKey()));
    }
}
package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

public class NoGsiResultsException extends RuntimeException {
    public NoGsiResultsException(String indexName, String partitionKey, String sortKey) {
        super(String.format("No items found for GSI query on index: %s with partitionKey: %s and sortKey: %s",
            indexName, partitionKey, sortKey));
    }
}
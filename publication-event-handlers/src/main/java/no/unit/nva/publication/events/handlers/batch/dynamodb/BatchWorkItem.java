package no.unit.nva.publication.events.handlers.batch.dynamodb;

import com.fasterxml.jackson.databind.JsonNode;

public record BatchWorkItem(DynamodbResourceBatchDynamoDbKey dynamoDbKey, String jobType, JsonNode parameters) {
    public BatchWorkItem(DynamodbResourceBatchDynamoDbKey dynamoDbKey, String jobType) {
        this(dynamoDbKey, jobType, null);
    }
}

package no.unit.nva.publication.events.handlers.batch.dynamodb;

import java.util.List;

// Add job implementations to DynamodbResourceBatchJobHandler.JOBS to register it
public interface DynamodbResourceBatchJobExecutor {
    void executeBatch(List<BatchWorkItem> workItems);
    String getJobType();
}
package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import java.util.List;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

public class MigrateResourceJob implements DynamodbResourceBatchJobExecutor {

    private static final String JOB_TYPE = "MIGRATE_RESOURCE";
    private final ResourceService resourceService;

    @JacocoGenerated
    public MigrateResourceJob() {
        this(ResourceService.defaultService());
    }

    public MigrateResourceJob(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void executeBatch(List<BatchWorkItem> workItems) {
        if (workItems.isEmpty()) {
            return;
        }

        var keys = workItems.stream()
            .map(workItem -> workItem.dynamoDbKey().toPrimaryKey())
            .toList();

        resourceService.refreshResourcesByKeys(keys);
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }
}

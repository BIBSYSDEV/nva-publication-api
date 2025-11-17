package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        var keys = workItems.stream().map(this::createKey).toList();

        resourceService.refreshResourcesByKeys(keys);
    }

    private Map<String, AttributeValue> createKey(BatchWorkItem workItem) {
        var key = new HashMap<String, AttributeValue>();
        key.put(PRIMARY_KEY_PARTITION_KEY_NAME, new AttributeValue(workItem.dynamoDbKey().partitionKey()));
        key.put(PRIMARY_KEY_SORT_KEY_NAME, new AttributeValue(workItem.dynamoDbKey().sortKey()));
        return key;
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }
}

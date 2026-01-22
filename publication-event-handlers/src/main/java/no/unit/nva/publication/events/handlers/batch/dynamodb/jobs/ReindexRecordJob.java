package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;

public class ReindexRecordJob implements DynamodbResourceBatchJobExecutor {
    private static final String REINDEX_RECORD = "REINDEX_RECORD";
    private static final Logger logger = LoggerFactory.getLogger(ReindexRecordJob.class);
    private static final String TABLE_NAME_ENV = "TABLE_NAME";
    private static final String VERSION_FIELD = "version";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @JacocoGenerated
    public ReindexRecordJob() {
        this(DynamoDbClient.create(),
             new Environment().readEnv(TABLE_NAME_ENV));
    }

    public ReindexRecordJob(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public void executeBatch(List<BatchWorkItem> workItems) {
        if (workItems.isEmpty()) {
            return;
        }

        updateVersionBatchByTransaction(workItems);
    }

    @Override
    public String getJobType() {
        return REINDEX_RECORD;
    }

    protected void updateVersionBatchByTransaction(List<BatchWorkItem> workItems) {
        var transactItems = workItems.stream()
            .map(this::createUpdateTransactItem)
            .toList();

        var transactRequest = TransactWriteItemsRequest.builder()
            .transactItems(transactItems)
            .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
            .returnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
            .build();

        try {
            dynamoDbClient.transactWriteItems(transactRequest);

            // Transaction succeeded - ALL items were updated (atomic transaction)
            logger.info("Successfully updated {} records in transaction", workItems.size());
        } catch (Exception e) {
            logger.error("Failed to update batch of {} records in transaction", workItems.size(), e);

            if (isConditionalCheckFailure(e)) {
                logger.error("Transaction failed due to conditional check - likely non-existent records");
                throw new RuntimeException(String.format(
                    "Failed to update %d records - records do not exist in database",
                    workItems.size()), e);
            }

            throw new RuntimeException("Failed to update batch for reindexing", e);
        }
    }

    private TransactWriteItem createUpdateTransactItem(BatchWorkItem workItem) {
        var key = workItem.dynamoDbKey();
        var newVersion = UUID.randomUUID().toString();

        var dynamoKey = Map.of(
            "PK0", AttributeValue.builder().s(key.partitionKey()).build(),
            "SK0", AttributeValue.builder().s(key.sortKey()).build()
        );

        var expressionAttributeValues = Map.of(
            ":newVersion", AttributeValue.builder().s(newVersion).build()
        );

        var update = Update.builder()
            .tableName(tableName)
            .key(dynamoKey)
            .updateExpression("SET #version = :newVersion")
            .conditionExpression("attribute_exists(PK0) AND attribute_exists(SK0)")
            .expressionAttributeNames(Map.of("#version", VERSION_FIELD))
            .expressionAttributeValues(expressionAttributeValues)
            .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
            .build();

        return TransactWriteItem.builder().update(update).build();
    }

    private boolean isConditionalCheckFailure(Exception e) {
        return e instanceof ConditionalCheckFailedException ||
               nonNull(e.getCause()) && e.getCause() instanceof ConditionalCheckFailedException;
    }
}

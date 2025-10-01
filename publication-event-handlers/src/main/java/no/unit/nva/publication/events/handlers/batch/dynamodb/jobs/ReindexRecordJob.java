package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static java.util.Objects.nonNull;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.Update;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReindexRecordJob implements DynamodbResourceBatchJobExecutor {
    private static final String REINDEX_RECORD = "REINDEX_RECORD";
    private static final Logger logger = LoggerFactory.getLogger(ReindexRecordJob.class);
    private static final String TABLE_NAME_ENV = "TABLE_NAME";
    private static final String VERSION_FIELD = "version";

    private final AmazonDynamoDB dynamoDbClient;
    private final String tableName;

    @JacocoGenerated
    public ReindexRecordJob() {
        this(AmazonDynamoDBClientBuilder.defaultClient(),
             new Environment().readEnv(TABLE_NAME_ENV));
    }
    
    public ReindexRecordJob(AmazonDynamoDB dynamoDbClient, String tableName) {
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
        
        var transactRequest = new TransactWriteItemsRequest()
            .withTransactItems(transactItems)
            .withReturnConsumedCapacity("TOTAL")
            .withReturnItemCollectionMetrics("SIZE");
        
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
        
        var dynamoKey = new HashMap<String, AttributeValue>();
        dynamoKey.put("PK0", new AttributeValue().withS(key.partitionKey()));
        dynamoKey.put("SK0", new AttributeValue().withS(key.sortKey()));
        
        var expressionAttributeValues = new HashMap<String, AttributeValue>();
        expressionAttributeValues.put(":newVersion", new AttributeValue().withS(newVersion));

        var update = new Update()
            .withTableName(tableName)
            .withKey(dynamoKey)
            .withUpdateExpression("SET #version = :newVersion")
            .withConditionExpression("attribute_exists(PK0) AND attribute_exists(SK0)")
            .withExpressionAttributeNames(Map.of(
                "#version", VERSION_FIELD
            ))
            .withExpressionAttributeValues(expressionAttributeValues)
            .withReturnValuesOnConditionCheckFailure("ALL_OLD");
        
        return new TransactWriteItem().withUpdate(update);
    }
    
    private boolean isConditionalCheckFailure(Exception e) {
        return e instanceof ConditionalCheckFailedException ||
               nonNull(e.getCause()) && e.getCause() instanceof ConditionalCheckFailedException;
    }
}
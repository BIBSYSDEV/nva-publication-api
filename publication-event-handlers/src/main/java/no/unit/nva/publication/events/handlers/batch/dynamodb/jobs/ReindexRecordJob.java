package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_KEY_PAIRS;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.Update;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;
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
    private static final String PK_0 = "PK0";
    private static final String SK_0 = "SK0";

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

        var partitionedItems = workItems.stream()
                                   .collect(Collectors.partitioningBy(item -> item.dynamoDbKey().isGsiQuery()));

        var gsiItems = partitionedItems.get(true);
        var primaryKeyItems = partitionedItems.get(false);

        var resolvedItems = Stream.concat(
            gsiItems.isEmpty() ? Stream.empty() : resolveGsiToPrimaryKeys(gsiItems).stream(),
            primaryKeyItems.stream()
        ).toList();

        if (!resolvedItems.isEmpty()) {
            var affectedKeys = updateVersionBatchByTransaction(resolvedItems);
            
            // Identify work items that were not successfully updated
            var failedItems = resolvedItems.stream()
                .filter(item -> !affectedKeys.contains(item.dynamoDbKey()))
                .toList();
            
            if (!failedItems.isEmpty()) {
                logger.error("Failed to update {} work items - likely non-existent records: {}", 
                           failedItems.size(), 
                           failedItems.stream()
                               .map(item -> String.format("[PK=%s, SK=%s]", 
                                                         item.dynamoDbKey().partitionKey(), 
                                                         item.dynamoDbKey().sortKey()))
                               .collect(Collectors.joining(", ")));
                
                // Fail the job for non-existent records
                throw new RuntimeException(String.format(
                    "Failed to update %d records - records do not exist in database", 
                    failedItems.size()));
            }
        }
    }

    @Override
    public String getJobType() {
        return REINDEX_RECORD;
    }

    protected Set<DynamodbResourceBatchDynamoDbKey> updateVersionBatchByTransaction(List<BatchWorkItem> workItems) {
        var transactItems = workItems.stream()
            .map(this::createUpdateTransactItem)
            .toList();
        
        var transactRequest = new TransactWriteItemsRequest()
            .withTransactItems(transactItems)
            .withReturnConsumedCapacity("TOTAL")
            .withReturnItemCollectionMetrics("SIZE");
        
        var affectedKeys = new HashSet<DynamodbResourceBatchDynamoDbKey>();
        
        try {
            dynamoDbClient.transactWriteItems(transactRequest);
            
            // Collect successfully updated keys
            for (BatchWorkItem workItem : workItems) {
                // If the transaction succeeded, all items were updated
                affectedKeys.add(workItem.dynamoDbKey());
            }
            
            logger.info("Successfully updated {} records in transaction", affectedKeys.size());
            return affectedKeys;
        } catch (Exception e) {
            logger.error("Failed to update batch of {} records in transaction", workItems.size(), e);
            
            // Try to determine which items failed
            var failedKeys = identifyFailedItems(workItems, e);
            
            // If we identified failed items due to conditional checks, throw specific error
            if (!failedKeys.isEmpty()) {
                logger.error("Failed to update {} work items - likely non-existent records", failedKeys.size());
                throw new RuntimeException(String.format(
                    "Failed to update %d records - records do not exist in database", 
                    failedKeys.size()), e);
            }
            
            // Otherwise, it's a different kind of error
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
    
    private Set<DynamodbResourceBatchDynamoDbKey> identifyFailedItems(List<BatchWorkItem> workItems, Exception e) {
        var failedKeys = new HashSet<DynamodbResourceBatchDynamoDbKey>();
        
        // Check if the exception provides details about which items failed
        var message = e.getMessage();
        var causeMessage = nonNull(e.getCause()) ? e.getCause().getMessage() : EMPTY_STRING;
        
        if (noNull(message) && message.contains("ConditionalCheckFailed")
            || nonNull(causeMessage) && causeMessage.contains("ConditionalCheckFailed")
            || e instanceof ConditionalCheckFailedException
            || e.getCause() instanceof ConditionalCheckFailedException) {
            // When condition checks fail, typically it means the record doesn't exist
            // Transaction failed due to conditional check - all items in batch are considered failed
            logger.warn("Conditional check failed - likely non-existent records in batch");
            
            // Mark all items as failed since DynamoDB transaction is atomic
            workItems.forEach(item -> failedKeys.add(item.dynamoDbKey()));
        }
        
        return failedKeys;
    }

    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    private List<BatchWorkItem> resolveGsiToPrimaryKeys(List<BatchWorkItem> gsiItems) {
        var resolvedItems = new ArrayList<BatchWorkItem>();
        
        for (BatchWorkItem gsiItem : gsiItems) {
            var key = gsiItem.dynamoDbKey();
            
            var gsiKey = GSI_KEY_PAIRS.get(key.indexName());
            var queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withIndexName(key.indexName())
                .withKeyConditionExpression("#pk = :pkval AND #sk = :skval")
                .withExpressionAttributeNames(Map.of(
                    "#pk", gsiKey.partitionKey(),
                    "#sk", gsiKey.sortKey()
                ))
                .withExpressionAttributeValues(Map.of(
                    ":pkval", new AttributeValue().withS(key.partitionKey()),
                    ":skval", new AttributeValue().withS(key.sortKey())
                ))
                .withProjectionExpression("PK0, SK0")
                .withLimit(100);
            
            try {
                var result = dynamoDbClient.query(queryRequest);
                
                var initialSize = resolvedItems.size();
                createWorkItems(gsiItem, result, resolvedItems);

                while (result.getLastEvaluatedKey() != null && !result.getLastEvaluatedKey().isEmpty()) {
                    queryRequest.setExclusiveStartKey(result.getLastEvaluatedKey());
                    result = dynamoDbClient.query(queryRequest);

                    createWorkItems(gsiItem, result, resolvedItems);
                }
                
                var itemsResolved = resolvedItems.size() - initialSize;
                if (itemsResolved == 0) {
                    throw new NoGsiResultsException(key.indexName(), key.partitionKey(), key.sortKey());
                }
                
                logger.info("Resolved {} primary keys from GSI query for index: {}", 
                           itemsResolved, key.indexName());
                
            } catch (Exception e) {
                logger.error("Failed to resolve GSI to primary keys for index: {}", key.indexName(), e);
                throw new RuntimeException("Failed to resolve GSI to primary keys", e);
            }
        }
        
        return resolvedItems;
    }

    private static void createWorkItems(BatchWorkItem gsiItem, QueryResult result, ArrayList<BatchWorkItem> resolvedItems) {
        var newItems = result.getItems().stream()
            .map(item -> {
                var primaryPk = item.get(PK_0).getS();
                var primarySk = item.get(SK_0).getS();
                var primaryKey = new DynamodbResourceBatchDynamoDbKey(primaryPk, primarySk, null);
                return new BatchWorkItem(primaryKey, gsiItem.jobType(), gsiItem.parameters());
            })
            .toList();
        
        resolvedItems.addAll(newItems);
    }
}
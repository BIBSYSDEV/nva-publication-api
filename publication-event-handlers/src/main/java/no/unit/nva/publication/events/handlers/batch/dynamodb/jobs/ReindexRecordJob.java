package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_KEY_PAIRS;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;
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

        var resolvedItems = resolvePrimaryBatchWorkItems(workItems);

        if (!resolvedItems.isEmpty()) {
            updateVersionBatchByTransaction(resolvedItems);
        }
    }

    @NotNull
    private List<BatchWorkItem> resolvePrimaryBatchWorkItems(List<BatchWorkItem> workItems) {
        var partitionedItems = workItems.stream()
                                   .collect(Collectors.partitioningBy(item -> item.dynamoDbKey().isGsiQuery()));

        var gsiItems = partitionedItems.get(true);
        var primaryKeyItems = partitionedItems.get(false);

        return Stream.concat(
            gsiItems.isEmpty() ? Stream.empty() : resolveGsiToPrimaryKeys(gsiItems).stream(),
            primaryKeyItems.stream()
        ).toList();
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

    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    private List<BatchWorkItem> resolveGsiToPrimaryKeys(List<BatchWorkItem> gsiItems) {
        var resolvedItems = new ArrayList<BatchWorkItem>();
        
        for (BatchWorkItem gsiItem : gsiItems) {
            var key = gsiItem.dynamoDbKey();

            var queryRequest = createGsiQueryRequest(key);

            try {
                var result = dynamoDbClient.query(queryRequest);
                
                var initialSize = resolvedItems.size();
                resolvedItems.addAll(createWorkItems(gsiItem, result));

                while (nonNull(result.getLastEvaluatedKey()) && !result.getLastEvaluatedKey().isEmpty()) {
                    queryRequest.setExclusiveStartKey(result.getLastEvaluatedKey());
                    result = dynamoDbClient.query(queryRequest);

                    resolvedItems.addAll(createWorkItems(gsiItem, result));
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

    private QueryRequest createGsiQueryRequest(DynamodbResourceBatchDynamoDbKey key) {
        var gsiKey = GSI_KEY_PAIRS.get(key.indexName());

        return new QueryRequest()
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
    }

    private static List<BatchWorkItem> createWorkItems(BatchWorkItem gsiItem, QueryResult result) {
        return result.getItems().stream()
            .map(item ->  createPrimaryKeyWorkFromGsi(gsiItem, item))
            .toList();
    }

    private static BatchWorkItem createPrimaryKeyWorkFromGsi(BatchWorkItem gsiItem, Map<String, AttributeValue> item) {
        var primaryPk = item.get(PRIMARY_KEY_PARTITION_KEY_NAME).getS();
        var primarySk = item.get(PRIMARY_KEY_SORT_KEY_NAME).getS();
        var primaryKey = new DynamodbResourceBatchDynamoDbKey(primaryPk, primarySk);
        return new BatchWorkItem(primaryKey, gsiItem.jobType(), gsiItem.parameters());
    }
}
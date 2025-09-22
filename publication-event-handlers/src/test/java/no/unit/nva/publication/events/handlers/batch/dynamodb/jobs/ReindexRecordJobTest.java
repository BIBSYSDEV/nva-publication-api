package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ReindexRecordJobTest {

    private static final String TEST_TABLE_NAME = "test-table";
    private static final String TEST_PARTITION_KEY = "Resource:0190e0e7-5eef-7d23-b716-02c670833fcd";
    private static final String TEST_SORT_KEY = "Resource";
    
    @Mock
    private AmazonDynamoDB mockDynamoDbClient;
    
    private ReindexRecordJob reindexRecordJob;
    
    @BeforeEach
    void setUp() {
        reindexRecordJob = new ReindexRecordJob(mockDynamoDbClient, TEST_TABLE_NAME);
    }
    
    @Test
    void shouldReturnCorrectJobType() {
        assertThat(reindexRecordJob.getJobType(), equalTo("REINDEX_RECORD"));
    }
    
    @Test
    void shouldUpdateSingleVersionWithNewUuid() {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY, null);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor = ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        TransactWriteItemsRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTransactItems(), notNullValue());
        assertThat(capturedRequest.getTransactItems().size(), equalTo(1));
        
        var update = capturedRequest.getTransactItems().getFirst().getUpdate();
        assertThat(update.getTableName(), equalTo(TEST_TABLE_NAME));
        assertThat(update.getKey().get("PK0").getS(), equalTo(TEST_PARTITION_KEY));
        assertThat(update.getKey().get("SK0").getS(), equalTo(TEST_SORT_KEY));
        assertThat(update.getUpdateExpression(), 
                  equalTo("SET #version = :newVersion"));
        assertThat(update.getExpressionAttributeNames().get("#version"), equalTo("version"));
        assertThat(update.getExpressionAttributeValues().get(":newVersion"), notNullValue());
    }
    
    @Test
    void shouldResolveGsiQueryToPrimaryKeys() {
        // Given
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:123", 
            "Resource:2024", 
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD", null);
        
        var queryResult = new QueryResult();
        var item1 = new java.util.HashMap<String, AttributeValue>();
        item1.put("PK0", new AttributeValue().withS("Resource:123"));
        item1.put("SK0", new AttributeValue().withS("Resource"));
        var item2 = new java.util.HashMap<String, AttributeValue>();
        item2.put("PK0", new AttributeValue().withS("Resource:456"));
        item2.put("SK0", new AttributeValue().withS("Resource"));
        queryResult.setItems(List.of(item1, item2));
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(queryResult);
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        verify(mockDynamoDbClient).query(argThat(request ->
            BY_CUSTOMER_RESOURCE_INDEX_NAME.equals(request.getIndexName()) &&
            TEST_TABLE_NAME.equals(request.getTableName())
        ));
        
        ArgumentCaptor<TransactWriteItemsRequest> transactCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(transactCaptor.capture());
        
        assertThat(transactCaptor.getValue().getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldPropagateExceptionWhenDynamoDbUpdateFails() {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY, null);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(new RuntimeException("DynamoDB error"));
        
        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem))
        );
        
        assertThat(exception.getMessage(), equalTo("Failed to update batch for reindexing"));
        assertThat(exception.getCause().getMessage(), equalTo("DynamoDB error"));
    }
    
    @Test
    void shouldProcessBatchWithTransaction() {
        var dynamoDbKey1 = new DynamodbResourceBatchDynamoDbKey(
            "Resource:0190e0e7-5eef-7d23-b716-02c670833fcd",
            "Resource",
            null
        );
        var dynamoDbKey2 = new DynamodbResourceBatchDynamoDbKey(
            "Resource:0190e0e7-5eef-7d23-b716-02c670833fce", 
            "Resource",
            null
        );
        
        var workItem1 = new BatchWorkItem(dynamoDbKey1, "REINDEX_RECORD", null);
        var workItem2 = new BatchWorkItem(dynamoDbKey2, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2)));
        
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor =
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        TransactWriteItemsRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTransactItems(), notNullValue());
        assertThat(capturedRequest.getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldHandleEmptyBatch() {
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of()));
        
        verify(mockDynamoDbClient, times(0)).transactWriteItems(any(TransactWriteItemsRequest.class));
    }
    
    @Test
    void shouldHandleMixedPrimaryAndGsiKeys() {
        var primaryKey = new DynamodbResourceBatchDynamoDbKey(
            TEST_PARTITION_KEY,
            TEST_SORT_KEY,
            null
        );
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "CristinIdentifier#12345",
            "Resource:2024",
            RESOURCE_BY_CRISTIN_ID_INDEX_NAME
        );
        
        var primaryWorkItem = new BatchWorkItem(primaryKey, "REINDEX_RECORD", null);
        var gsiWorkItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD", null);
        
        var queryResult = new QueryResult();
        var item = new java.util.HashMap<String, AttributeValue>();
        item.put("PK0", new AttributeValue().withS("Resource:789"));
        item.put("SK0", new AttributeValue().withS("Resource"));
        queryResult.setItems(List.of(item));
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(queryResult);
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(primaryWorkItem, gsiWorkItem)));
        
        verify(mockDynamoDbClient).query(any(QueryRequest.class)); // For GSI resolution
        
        ArgumentCaptor<TransactWriteItemsRequest> transactCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(transactCaptor.capture());
        
        assertThat(transactCaptor.getValue().getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldHandleGsiQueryWithPagination() {
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:456",
            "Resource:Article",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD", null);
        
        var firstResult = new QueryResult();
        var item1 = Map.of(
            "PK0", new AttributeValue().withS("Resource:001"),
            "SK0", new AttributeValue().withS("Resource")
        );
        firstResult.setItems(List.of(item1));
        firstResult.setLastEvaluatedKey(Map.of("dummy", new AttributeValue().withS("key")));
        
        var secondResult = new QueryResult();
        var item2 = Map.of(
            "PK0", new AttributeValue().withS("Resource:002"),
            "SK0", new AttributeValue().withS("Resource")
        );
        secondResult.setItems(List.of(item2));
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(firstResult)
            .thenReturn(secondResult);
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        verify(mockDynamoDbClient, times(2)).query(any(QueryRequest.class)); // Two queries for pagination
        
        ArgumentCaptor<TransactWriteItemsRequest> transactCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(transactCaptor.capture());
        
        assertThat(transactCaptor.getValue().getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldPropagateExceptionWhenGsiQueryFails() {
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:999",
            "Resource:Failed",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenThrow(new AmazonDynamoDBException("Query failed"));
        
        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem))
        );
        
        assertThat(exception.getMessage(), equalTo("Failed to resolve GSI to primary keys"));
        assertThat(exception.getCause().getClass(), equalTo(AmazonDynamoDBException.class));
        
        verify(mockDynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }
    
    @Test
    void shouldHandleLargeBatchOfPrimaryKeys() {
        var workItems = new java.util.ArrayList<BatchWorkItem>();
        for (int i = 0; i < 25; i++) {
            var key = new DynamodbResourceBatchDynamoDbKey(
                "Resource:" + String.format("%03d", i),
                "Resource",
                null
            );
            workItems.add(new BatchWorkItem(key, "REINDEX_RECORD", null));
        }
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(workItems));
        
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor =
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        assertThat(requestCaptor.getValue().getTransactItems().size(), equalTo(25));
    }
    
    @Test
    void shouldThrowExceptionWhenGsiQueryReturnsNoResults() {
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:NoResults",
            "Resource:Empty",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD", null);
        
        var queryResult = new QueryResult();
        queryResult.setItems(List.of()); // Empty result
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(queryResult);
        
        var exception = assertThrows(RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        assertThat(exception.getMessage(), containsString("Failed to resolve GSI to primary keys"));
        assertThat(exception.getCause() instanceof NoGsiResultsException, equalTo(true));
        assertThat(exception.getCause().getMessage(), containsString("No items found for GSI query"));
        
        verify(mockDynamoDbClient).query(any(QueryRequest.class));
        verify(mockDynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }
    
    @Test
    void shouldHandleMultipleGsiQueries() {
        var gsiKey1 = new DynamodbResourceBatchDynamoDbKey(
            "Customer:111",
            "Resource:Type1",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var gsiKey2 = new DynamodbResourceBatchDynamoDbKey(
            "CristinIdentifier#222",
            "Resource:Type2",
            RESOURCE_BY_CRISTIN_ID_INDEX_NAME
        );
        
        var workItem1 = new BatchWorkItem(gsiKey1, "REINDEX_RECORD", null);
        var workItem2 = new BatchWorkItem(gsiKey2, "REINDEX_RECORD", null);
        
        var queryResult1 = new QueryResult();
        var item1 = Map.of(
            "PK0", new AttributeValue().withS("Resource:AAA"),
            "SK0", new AttributeValue().withS("Resource")
        );
        queryResult1.setItems(List.of(item1));
        
        var queryResult2 = new QueryResult();
        var item2 = Map.of(
            "PK0", new AttributeValue().withS("Resource:BBB"),
            "SK0", new AttributeValue().withS("Resource")
        );
        queryResult2.setItems(List.of(item2));
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenAnswer(invocation -> {
                QueryRequest request = invocation.getArgument(0);
                if (BY_CUSTOMER_RESOURCE_INDEX_NAME.equals(request.getIndexName())) {
                    return queryResult1;
                } else if (RESOURCE_BY_CRISTIN_ID_INDEX_NAME.equals(request.getIndexName())) {
                    return queryResult2;
                }
                return new QueryResult();
            });
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2)));
        
        verify(mockDynamoDbClient, times(2)).query(any(QueryRequest.class));
        
        ArgumentCaptor<TransactWriteItemsRequest> transactCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(transactCaptor.capture());
        
        assertThat(transactCaptor.getValue().getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldVerifyTransactionContainsCorrectUpdateExpression() {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY, null);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        reindexRecordJob.executeBatch(List.of(workItem));
        
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor =
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        var update = requestCaptor.getValue().getTransactItems().getFirst().getUpdate();
        
        assertThat(update.getTableName(), equalTo(TEST_TABLE_NAME));
        assertThat(update.getKey().size(), equalTo(2));
        assertThat(update.getKey().containsKey("PK0"), equalTo(true));
        assertThat(update.getKey().containsKey("SK0"), equalTo(true));
        assertThat(update.getUpdateExpression(), notNullValue());
        assertThat(update.getExpressionAttributeNames(), notNullValue());
        assertThat(update.getExpressionAttributeValues(), notNullValue());
        assertThat(update.getConditionExpression(), equalTo("attribute_exists(PK0) AND attribute_exists(SK0)"));
        assertThat(update.getReturnValuesOnConditionCheckFailure(), equalTo("ALL_OLD"));
        
        var newVersion = update.getExpressionAttributeValues().get(":newVersion").getS();
        assertThat(newVersion, notNullValue());
        assertThat(newVersion.length(), equalTo(36)); // UUID length
    }

    @Test
    void shouldThrowExceptionWhenRecordsDoNotExist() {
        var nonExistentKey1 = new DynamodbResourceBatchDynamoDbKey(
            "Resource:non-existent-1",
            "Resource",
            null
        );
        var nonExistentKey2 = new DynamodbResourceBatchDynamoDbKey(
            "Resource:non-existent-2",
            "Resource",
            null
        );

        var workItem1 = new BatchWorkItem(nonExistentKey1, "REINDEX_RECORD", null);
        var workItem2 = new BatchWorkItem(nonExistentKey2, "REINDEX_RECORD", null);

        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(new ConditionalCheckFailedException("ConditionalCheckFailed: The conditional request failed"));

        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2))
        );

        assertThat(exception.getMessage(), containsString("records do not exist in database"));
        verify(mockDynamoDbClient).transactWriteItems(any(TransactWriteItemsRequest.class));
    }

    @Test
    void shouldIdentifyFailedItemsInErrorMessage() {
        var existingKey = new DynamodbResourceBatchDynamoDbKey(
            "Resource:existing",
            "Resource",
            null
        );
        var nonExistentKey = new DynamodbResourceBatchDynamoDbKey(
            "Resource:non-existent",
            "Resource",
            null
        );

        var workItem1 = new BatchWorkItem(existingKey, "REINDEX_RECORD", null);
        var workItem2 = new BatchWorkItem(nonExistentKey, "REINDEX_RECORD", null);

        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(new ConditionalCheckFailedException("ConditionalCheckFailed"));

        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2))
        );

        assertThat(exception.getMessage(), containsString("Failed to update 2 records"));
        assertThat(exception.getMessage(), containsString("records do not exist in database"));
    }

    @Test
    void shouldHandleConditionalCheckFailureInTransaction() {
        var validKey = new DynamodbResourceBatchDynamoDbKey(
            "Resource:valid-item",
            "Resource",
            null
        );
        var invalidKey = new DynamodbResourceBatchDynamoDbKey(
            "Resource:invalid-item",
            "Resource",
            null
        );

        var validWorkItem = new BatchWorkItem(validKey, "REINDEX_RECORD", null);
        var invalidWorkItem = new BatchWorkItem(invalidKey, "REINDEX_RECORD", null);

        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(new RuntimeException("Transaction cancelled, one or more conditional checks failed"));

        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(validWorkItem, invalidWorkItem))
        );

        assertThat(exception.getMessage(), equalTo("Failed to update batch for reindexing"));
        verify(mockDynamoDbClient).transactWriteItems(any(TransactWriteItemsRequest.class));
    }

    @Test
    void shouldIncludeReturnValuesOnConditionCheckFailureInUpdate() {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY, null);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD", null);

        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());

        reindexRecordJob.executeBatch(List.of(workItem));

        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor =
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());

        var update = requestCaptor.getValue().getTransactItems().getFirst().getUpdate();

        assertThat(update.getReturnValuesOnConditionCheckFailure(), equalTo("ALL_OLD"));
    }
    
    @Test
    void shouldFailJobWhenItemsNotSuccessfullyUpdated() {
        var key1 = new DynamodbResourceBatchDynamoDbKey("Resource:exists1", "Resource", null);
        var key2 = new DynamodbResourceBatchDynamoDbKey("Resource:exists2", "Resource", null);
        var key3 = new DynamodbResourceBatchDynamoDbKey("Resource:notexists", "Resource", null);
        
        var workItem1 = new BatchWorkItem(key1, "REINDEX_RECORD", null);
        var workItem2 = new BatchWorkItem(key2, "REINDEX_RECORD", null);
        var workItem3 = new BatchWorkItem(key3, "REINDEX_RECORD", null);
        
        var testJob = new ReindexRecordJobTestable(key3);

        var exception = assertThrows(
            RuntimeException.class,
            () -> testJob.executeBatch(List.of(workItem1, workItem2, workItem3))
        );
        
        assertThat(exception.getMessage(), containsString("Failed to update 1 records"));
        assertThat(exception.getMessage(), containsString("records do not exist in database"));
    }
    
    @Test
    void shouldFailWithDetailedErrorWhenSomeRecordsDoNotExist() {
        var spyJob = org.mockito.Mockito.spy(reindexRecordJob);
        
        var existingKey = new DynamodbResourceBatchDynamoDbKey("Resource:exists", "Resource", null);
        var nonExistentKey = new DynamodbResourceBatchDynamoDbKey("Resource:does-not-exist", "Resource", null);
        
        var workItem1 = new BatchWorkItem(existingKey, "REINDEX_RECORD", null);
        var workItem2 = new BatchWorkItem(nonExistentKey, "REINDEX_RECORD", null);
        
        org.mockito.Mockito.doReturn(Set.of(existingKey))
            .when(spyJob)
            .updateVersionBatchByTransaction(any());
        
        var exception = assertThrows(
            RuntimeException.class,
            () -> spyJob.executeBatch(List.of(workItem1, workItem2))
        );
        
        assertThat(exception.getMessage(), containsString("Failed to update 1 records"));
        assertThat(exception.getMessage(), containsString("records do not exist in database"));
    }
    
    private static class ReindexRecordJobTestable extends ReindexRecordJob {
        private final Set<DynamodbResourceBatchDynamoDbKey> failedKeys;
        
        ReindexRecordJobTestable(DynamodbResourceBatchDynamoDbKey... failedKeys) {
            super(null, "test-table");
            this.failedKeys = Set.of(failedKeys);
        }
        
        @Override
        public void executeBatch(List<BatchWorkItem> workItems) {
            if (workItems.isEmpty()) {
                return;
            }
            
            var partitionedItems = workItems.stream()
                .collect(Collectors.partitioningBy(item -> item.dynamoDbKey().isGsiQuery()));
            
            var primaryKeyItems = partitionedItems.get(false);
            
            if (!primaryKeyItems.isEmpty()) {
                var affectedKeys = primaryKeyItems.stream()
                    .map(BatchWorkItem::dynamoDbKey)
                    .filter(key -> !failedKeys.contains(key))
                    .collect(Collectors.toSet());
                
                var failedItems = primaryKeyItems.stream()
                    .filter(item -> !affectedKeys.contains(item.dynamoDbKey()))
                    .toList();
                
                if (!failedItems.isEmpty()) {
                    var logger = LoggerFactory.getLogger(ReindexRecordJob.class);
                    logger.error("Failed to update {} work items - likely non-existent records: {}", 
                               failedItems.size(), 
                               failedItems.stream()
                                   .map(item -> String.format("[PK=%s, SK=%s]", 
                                                             item.dynamoDbKey().partitionKey(), 
                                                             item.dynamoDbKey().sortKey()))
                                   .collect(Collectors.joining(", ")));
                    
                    throw new RuntimeException(String.format(
                        "Failed to update %d records - records do not exist in database", 
                        failedItems.size()));
                }
            }
        }
    }
}
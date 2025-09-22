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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.util.List;
import java.util.Map;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        // Given
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY, null);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        // When
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        // Then
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
        
        // Mock GSI query response
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
        
        // When
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        // Then
        verify(mockDynamoDbClient).query(argThat(request -> 
            BY_CUSTOMER_RESOURCE_INDEX_NAME.equals(request.getIndexName()) &&
            TEST_TABLE_NAME.equals(request.getTableName())
        ));
        
        ArgumentCaptor<TransactWriteItemsRequest> transactCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(transactCaptor.capture());
        
        // Should have 2 items in transaction (one for each resolved primary key)
        assertThat(transactCaptor.getValue().getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldPropagateExceptionWhenDynamoDbUpdateFails() {
        // Given
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY, null);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(new RuntimeException("DynamoDB error"));
        
        // When & Then
        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem))
        );
        
        assertThat(exception.getMessage(), equalTo("Failed to update batch for reindexing"));
        assertThat(exception.getCause().getMessage(), equalTo("DynamoDB error"));
    }
    
    @Test
    void shouldProcessBatchWithTransaction() {
        // Given
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
        
        // When
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2)));
        
        // Then
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        TransactWriteItemsRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getTransactItems(), notNullValue());
        assertThat(capturedRequest.getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldHandleEmptyBatch() {
        // When & Then
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of()));
        
        // Verify no DynamoDB calls were made
        verify(mockDynamoDbClient, times(0)).transactWriteItems(any(TransactWriteItemsRequest.class));
    }
    
    @Test
    void shouldHandleMixedPrimaryAndGsiKeys() {
        // Given - mix of primary key and GSI key items
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
        
        // Mock GSI query response
        var queryResult = new QueryResult();
        var item = new java.util.HashMap<String, AttributeValue>();
        item.put("PK0", new AttributeValue().withS("Resource:789"));
        item.put("SK0", new AttributeValue().withS("Resource"));
        queryResult.setItems(List.of(item));
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(queryResult);
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        // When
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(primaryWorkItem, gsiWorkItem)));
        
        // Then
        verify(mockDynamoDbClient).query(any(QueryRequest.class)); // For GSI resolution
        
        ArgumentCaptor<TransactWriteItemsRequest> transactCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(transactCaptor.capture());
        
        // Should have 2 items in transaction (1 primary + 1 resolved from GSI)
        assertThat(transactCaptor.getValue().getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldHandleGsiQueryWithPagination() {
        // Given
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:456",
            "Resource:Article",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD", null);
        
        // First page
        var firstResult = new QueryResult();
        var item1 = Map.of(
            "PK0", new AttributeValue().withS("Resource:001"),
            "SK0", new AttributeValue().withS("Resource")
        );
        firstResult.setItems(List.of(item1));
        firstResult.setLastEvaluatedKey(Map.of("dummy", new AttributeValue().withS("key")));
        
        // Second page
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
        
        // When
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        // Then
        verify(mockDynamoDbClient, times(2)).query(any(QueryRequest.class)); // Two queries for pagination
        
        ArgumentCaptor<TransactWriteItemsRequest> transactCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(transactCaptor.capture());
        
        // Should have 2 items from both pages
        assertThat(transactCaptor.getValue().getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldPropagateExceptionWhenGsiQueryFails() {
        // Given
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:999",
            "Resource:Failed",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenThrow(new AmazonDynamoDBException("Query failed"));
        
        // When & Then
        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem))
        );
        
        assertThat(exception.getMessage(), equalTo("Failed to resolve GSI to primary keys"));
        assertThat(exception.getCause().getClass(), equalTo(AmazonDynamoDBException.class));
        
        // Verify transaction was never attempted
        verify(mockDynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }
    
    @Test
    void shouldHandleLargeBatchOfPrimaryKeys() {
        // Given - batch of 25 items (max DynamoDB transaction limit)
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
        
        // When
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(workItems));
        
        // Then
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        assertThat(requestCaptor.getValue().getTransactItems().size(), equalTo(25));
    }
    
    @Test
    void shouldThrowExceptionWhenGsiQueryReturnsNoResults() {
        // Given
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:NoResults",
            "Resource:Empty",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD", null);
        
        // Mock empty GSI query response
        var queryResult = new QueryResult();
        queryResult.setItems(List.of()); // Empty result
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(queryResult);
        
        // When & Then
        var exception = assertThrows(RuntimeException.class, 
            () -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        assertThat(exception.getMessage(), containsString("Failed to resolve GSI to primary keys"));
        assertThat(exception.getCause() instanceof NoGsiResultsException, equalTo(true));
        assertThat(exception.getCause().getMessage(), containsString("No items found for GSI query"));
        
        verify(mockDynamoDbClient).query(any(QueryRequest.class));
        // No transaction should be attempted since there are no items to update
        verify(mockDynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
    }
    
    @Test
    void shouldHandleMultipleGsiQueries() {
        // Given - two different GSI queries
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
        
        // Mock GSI query responses
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
        
        // When
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2)));
        
        // Then
        verify(mockDynamoDbClient, times(2)).query(any(QueryRequest.class));
        
        ArgumentCaptor<TransactWriteItemsRequest> transactCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(transactCaptor.capture());
        
        // Should have 2 items (one from each GSI query)
        assertThat(transactCaptor.getValue().getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldVerifyTransactionContainsCorrectUpdateExpression() {
        // Given
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY, null);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD", null);
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
        
        // When
        reindexRecordJob.executeBatch(List.of(workItem));
        
        // Then
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor = 
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        var update = requestCaptor.getValue().getTransactItems().getFirst().getUpdate();
        
        // Verify all required fields are present
        assertThat(update.getTableName(), equalTo(TEST_TABLE_NAME));
        assertThat(update.getKey().size(), equalTo(2));
        assertThat(update.getKey().containsKey("PK0"), equalTo(true));
        assertThat(update.getKey().containsKey("SK0"), equalTo(true));
        assertThat(update.getUpdateExpression(), notNullValue());
        assertThat(update.getExpressionAttributeNames(), notNullValue());
        assertThat(update.getExpressionAttributeValues(), notNullValue());
        
        // Verify version is being updated with a UUID
        var newVersion = update.getExpressionAttributeValues().get(":newVersion").getS();
        assertThat(newVersion, notNullValue());
        assertThat(newVersion.length(), equalTo(36)); // UUID length
    }
}
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
    
    private static BatchWorkItem createWorkItem(String partitionKey, String sortKey) {
        return createWorkItem(partitionKey, sortKey, null);
    }
    
    private static BatchWorkItem createWorkItem(String partitionKey, String sortKey, String indexName) {
        var key = new DynamodbResourceBatchDynamoDbKey(partitionKey, sortKey, indexName);
        return new BatchWorkItem(key, "REINDEX_RECORD");
    }
    
    private static Map<String, AttributeValue> createDynamoItem(String pk, String sk) {
        return Map.of(
            "PK0", new AttributeValue().withS(pk),
            "SK0", new AttributeValue().withS(sk)
        );
    }
    
    private void mockSuccessfulTransaction() {
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(new TransactWriteItemsResult());
    }
    
    private TransactWriteItemsRequest captureTransactionRequest() {
        ArgumentCaptor<TransactWriteItemsRequest> captor = ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(captor.capture());
        return captor.getValue();
    }
    
    @Test
    void shouldReturnCorrectJobType() {
        assertThat(reindexRecordJob.getJobType(), equalTo("REINDEX_RECORD"));
    }
    
    @Test
    void shouldUpdateSingleVersionWithNewUuid() {
        var workItem = createWorkItem(TEST_PARTITION_KEY, TEST_SORT_KEY);
        mockSuccessfulTransaction();
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        var capturedRequest = captureTransactionRequest();
        assertThat(capturedRequest.getTransactItems(), notNullValue());
        assertThat(capturedRequest.getTransactItems().size(), equalTo(1));
        
        var update = capturedRequest.getTransactItems().getFirst().getUpdate();
        assertThat(update.getTableName(), equalTo(TEST_TABLE_NAME));
        assertThat(update.getKey().get("PK0").getS(), equalTo(TEST_PARTITION_KEY));
        assertThat(update.getKey().get("SK0").getS(), equalTo(TEST_SORT_KEY));
        assertThat(update.getUpdateExpression(), equalTo("SET #version = :newVersion"));
        assertThat(update.getExpressionAttributeNames().get("#version"), equalTo("version"));
        assertThat(update.getExpressionAttributeValues().get(":newVersion"), notNullValue());
    }
    
    @Test
    void shouldResolveGsiQueryToPrimaryKeys() {
        var workItem = createWorkItem("Customer:123", "Resource:2024", BY_CUSTOMER_RESOURCE_INDEX_NAME);
        
        var queryResult = new QueryResult();
        queryResult.setItems(List.of(
            createDynamoItem("Resource:123", "Resource"),
            createDynamoItem("Resource:456", "Resource")
        ));
        
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResult);
        mockSuccessfulTransaction();
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        verify(mockDynamoDbClient).query(argThat(request ->
            BY_CUSTOMER_RESOURCE_INDEX_NAME.equals(request.getIndexName()) &&
            TEST_TABLE_NAME.equals(request.getTableName())
        ));
        
        var capturedRequest = captureTransactionRequest();
        assertThat(capturedRequest.getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldPropagateExceptionWhenDynamoDbUpdateFails() {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD");
        
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
        var workItem1 = createWorkItem("Resource:0190e0e7-5eef-7d23-b716-02c670833fcd", "Resource");
        var workItem2 = createWorkItem("Resource:0190e0e7-5eef-7d23-b716-02c670833fce", "Resource");
        
        mockSuccessfulTransaction();
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2)));
        
        var capturedRequest = captureTransactionRequest();
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
        var primaryWorkItem = createWorkItem(TEST_PARTITION_KEY, TEST_SORT_KEY);
        var gsiWorkItem = createWorkItem("CristinIdentifier#12345", "Resource:2024", RESOURCE_BY_CRISTIN_ID_INDEX_NAME);
        
        var queryResult = new QueryResult();
        queryResult.setItems(List.of(createDynamoItem("Resource:789", "Resource")));
        
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResult);
        mockSuccessfulTransaction();
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(primaryWorkItem, gsiWorkItem)));
        
        verify(mockDynamoDbClient).query(any(QueryRequest.class));
        var capturedRequest = captureTransactionRequest();
        assertThat(capturedRequest.getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldHandleGsiQueryWithPagination() {
        var workItem = createWorkItem("Customer:456", "Resource:Article", BY_CUSTOMER_RESOURCE_INDEX_NAME);
        
        var firstResult = new QueryResult();
        firstResult.setItems(List.of(createDynamoItem("Resource:001", "Resource")));
        firstResult.setLastEvaluatedKey(Map.of("dummy", new AttributeValue().withS("key")));
        
        var secondResult = new QueryResult();
        secondResult.setItems(List.of(createDynamoItem("Resource:002", "Resource")));
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(firstResult)
            .thenReturn(secondResult);
        mockSuccessfulTransaction();
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of(workItem)));
        
        verify(mockDynamoDbClient, times(2)).query(any(QueryRequest.class));
        var capturedRequest = captureTransactionRequest();
        assertThat(capturedRequest.getTransactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldPropagateExceptionWhenGsiQueryFails() {
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:999",
            "Resource:Failed",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD");
        
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
            workItems.add(new BatchWorkItem(key, "REINDEX_RECORD"));
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
        var workItem = new BatchWorkItem(gsiKey, "REINDEX_RECORD");
        
        var queryResult = new QueryResult();
        queryResult.setItems(List.of());
        
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
        
        var workItem1 = new BatchWorkItem(gsiKey1, "REINDEX_RECORD");
        var workItem2 = new BatchWorkItem(gsiKey2, "REINDEX_RECORD");
        
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
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD");
        
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
        assertThat(newVersion.length(), equalTo(36));
    }

    @Test
    void shouldThrowExceptionWhenRecordsDoNotExist() {
        var workItem1 = createWorkItem("Resource:non-existent-1", "Resource");
        var workItem2 = createWorkItem("Resource:non-existent-2", "Resource");

        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(new ConditionalCheckFailedException("ConditionalCheckFailed"));

        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2))
        );

        assertThat(exception.getMessage(), containsString("records do not exist in database"));
        verify(mockDynamoDbClient).transactWriteItems(any(TransactWriteItemsRequest.class));
    }

    @Test
    void shouldHandleConditionalCheckFailureInTransaction() {
        var workItem1 = createWorkItem("Resource:valid-item", "Resource");
        var workItem2 = createWorkItem("Resource:invalid-item", "Resource");

        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(new RuntimeException("Transaction cancelled, one or more conditional checks failed"));

        var exception = assertThrows(
            RuntimeException.class,
            () -> reindexRecordJob.executeBatch(List.of(workItem1, workItem2))
        );

        assertThat(exception.getMessage(), equalTo("Failed to update batch for reindexing"));
        verify(mockDynamoDbClient).transactWriteItems(any(TransactWriteItemsRequest.class));
    }

    @Test
    void shouldIncludeReturnValuesOnConditionCheckFailureInUpdate() {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD");

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
    void shouldFailWithDetailedErrorWhenSomeRecordsDoNotExist() {
        var spyJob = org.mockito.Mockito.spy(reindexRecordJob);
        
        var workItem1 = createWorkItem("Resource:exists", "Resource");
        var workItem2 = createWorkItem("Resource:does-not-exist", "Resource");
        
        var existingKey = new DynamodbResourceBatchDynamoDbKey("Resource:exists", "Resource");
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
}
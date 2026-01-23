package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;
import java.util.List;
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
    private DynamoDbClient mockDynamoDbClient;
    
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
    
    private void mockSuccessfulTransaction() {
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(TransactWriteItemsResponse.builder().build());
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
        assertThat(capturedRequest.transactItems(), notNullValue());
        assertThat(capturedRequest.transactItems().size(), equalTo(1));
        
        var update = capturedRequest.transactItems().getFirst().update();
        assertThat(update.tableName(), equalTo(TEST_TABLE_NAME));
        assertThat(update.key().get("PK0").s(), equalTo(TEST_PARTITION_KEY));
        assertThat(update.key().get("SK0").s(), equalTo(TEST_SORT_KEY));
        assertThat(update.updateExpression(), equalTo("SET #version = :newVersion"));
        assertThat(update.expressionAttributeNames().get("#version"), equalTo("version"));
        assertThat(update.expressionAttributeValues().get(":newVersion"), notNullValue());
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
        assertThat(capturedRequest.transactItems(), notNullValue());
        assertThat(capturedRequest.transactItems().size(), equalTo(2));
    }
    
    @Test
    void shouldHandleEmptyBatch() {
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(List.of()));
        
        verify(mockDynamoDbClient, times(0)).transactWriteItems(any(TransactWriteItemsRequest.class));
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
            .thenReturn(TransactWriteItemsResponse.builder().build());
        
        assertDoesNotThrow(() -> reindexRecordJob.executeBatch(workItems));
        
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor =
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        assertThat(requestCaptor.getValue().transactItems().size(), equalTo(25));
    }
    
    @Test
    void shouldVerifyTransactionContainsCorrectUpdateExpression() {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY);
        var workItem = new BatchWorkItem(dynamoDbKey, "REINDEX_RECORD");
        
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(TransactWriteItemsResponse.builder().build());
        
        reindexRecordJob.executeBatch(List.of(workItem));
        
        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor =
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());
        
        var update = requestCaptor.getValue().transactItems().getFirst().update();
        
        assertThat(update.tableName(), equalTo(TEST_TABLE_NAME));
        assertThat(update.key().size(), equalTo(2));
        assertThat(update.key().containsKey("PK0"), equalTo(true));
        assertThat(update.key().containsKey("SK0"), equalTo(true));
        assertThat(update.updateExpression(), notNullValue());
        assertThat(update.expressionAttributeNames(), notNullValue());
        assertThat(update.expressionAttributeValues(), notNullValue());
        assertThat(update.conditionExpression(), equalTo("attribute_exists(PK0) AND attribute_exists(SK0)"));
        assertThat(update.returnValuesOnConditionCheckFailureAsString(), equalTo("ALL_OLD"));
        
        var newVersion = update.expressionAttributeValues().get(":newVersion").s();
        assertThat(newVersion, notNullValue());
        assertThat(newVersion.length(), equalTo(36));
    }

    @Test
    void shouldThrowExceptionWhenRecordsDoNotExist() {
        var workItem1 = createWorkItem("Resource:non-existent-1", "Resource");
        var workItem2 = createWorkItem("Resource:non-existent-2", "Resource");

        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(ConditionalCheckFailedException.builder().message("ConditionalCheckFailed").build());

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
            .thenReturn(TransactWriteItemsResponse.builder().build());

        reindexRecordJob.executeBatch(List.of(workItem));

        ArgumentCaptor<TransactWriteItemsRequest> requestCaptor =
            ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(mockDynamoDbClient).transactWriteItems(requestCaptor.capture());

        var update = requestCaptor.getValue().transactItems().getFirst().update();

        assertThat(update.returnValuesOnConditionCheckFailureAsString(), equalTo("ALL_OLD"));
    }
}
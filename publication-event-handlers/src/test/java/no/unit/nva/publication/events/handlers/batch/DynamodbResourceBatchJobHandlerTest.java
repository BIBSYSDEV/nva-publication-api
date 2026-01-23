package no.unit.nva.publication.events.handlers.batch;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobHandler;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamodbResourceBatchJobHandlerTest {

    private static final String TEST_PARTITION_KEY = "USER#123";
    private static final String TEST_SORT_KEY = "PROFILE";
    private static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private static final String TEST_JOB_NAME = "TEST_JOB_NAME";
    private static final String TABLE_NAME_ENV = "TABLE_NAME";
    private static final String TEST_TABLE_NAME = "test-table";

    @Mock
    private DynamodbResourceBatchJobExecutor mockExecutor;
    
    @Mock
    private DynamoDbClient mockDynamoDbClient;
    
    @Mock
    private Context context;
    
    private DynamodbResourceBatchJobHandler handler;
    
    @BeforeEach
    void setUp() {
        var handlers = new HashMap<String, DynamodbResourceBatchJobExecutor>();
        handlers.put(TEST_JOB_NAME, mockExecutor);
        handler = new DynamodbResourceBatchJobHandler(handlers, mockDynamoDbClient, TEST_TABLE_NAME);
    }
    
    private static Map<String, AttributeValue> createDynamoItem(String pk, String sk) {
        return Map.of(
            "PK0", AttributeValue.builder().s(pk).build(),
            "SK0", AttributeValue.builder().s(sk).build()
        );
    }
    
    @Test
    void shouldProcessCreateRecordSuccessfully() throws Exception {
        var workItem = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "John", "email", "john@example.com")
        );
        var sqsEvent = createSqsEvent(workItem);
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        verify(mockExecutor).executeBatch(any(List.class));
    }
    
    @Test
    void shouldReportBatchItemFailureWhenExecutorThrows() throws Exception {
        var workItem = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "John")
        );
        var sqsEvent = createSqsEvent(workItem);
        
        doThrow(new RuntimeException("Execution failed"))
            .when(mockExecutor).executeBatch(any(List.class));
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("message-id-1"));
    }
    
    @Test
    void shouldReportBatchItemFailureWhenUnsupportedJobType() throws JsonProcessingException {
        handler = new DynamodbResourceBatchJobHandler(new HashMap<>(), DynamoDbClient.create(),
                                                      new Environment().readEnv(TABLE_NAME_ENV));
        
        var workItem = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "John")
        );
        var sqsEvent = createSqsEvent(workItem);
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("message-id-1"));
    }
    
    @Test
    void shouldReportBatchItemFailureWhenInvalidJson() {
        var sqsEvent = new SQSEvent();
        var message = new SQSMessage();
        message.setMessageId("invalid-message");
        message.setBody("{ invalid json }");
        sqsEvent.setRecords(List.of(message));
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("invalid-message"));
    }
    
    @Test
    void shouldProcessMultipleMessagesInBatch() throws JsonProcessingException {
        var workItem1 = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "John")
        );
        var workItem2 = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "Jane")
        );
        
        var sqsEvent = createSqsEventWithMultipleMessages(
            List.of(workItem1, workItem2)
        );
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        verify(mockExecutor, times(1)).executeBatch(any(List.class));
    }
    
    @Test
    void shouldProcessWorkItemWithGsiKeys() throws JsonProcessingException {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey("AUTHOR#123", "PUBLICATION#2024", BY_CUSTOMER_RESOURCE_INDEX_NAME);
        var workItem = new BatchWorkItem(dynamoDbKey,
                                         TEST_JOB_NAME,
                                         objectMapper.valueToTree(Map.of("title", "Test Publication")));
        
        var queryResponse = QueryResponse.builder()
                                .items(List.of(createDynamoItem("Resource:123", "Resource")))
                                .build();
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        var sqsEvent = createSqsEvent(workItem);
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        
        var queryCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(mockDynamoDbClient).query(queryCaptor.capture());
        assertThat(queryCaptor.getValue().indexName(), equalTo(BY_CUSTOMER_RESOURCE_INDEX_NAME));
        
        // Verify the executor was called with resolved primary keys
        ArgumentCaptor<List<BatchWorkItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockExecutor).executeBatch(captor.capture());
        
        var capturedItems = captor.getValue();
        assertThat(capturedItems, hasSize(1));
        assertFalse(capturedItems.get(0).dynamoDbKey().isGsiQuery());
    }
    
    @Test
    void shouldIncludeErrorMetadataInFailure() {
        var sqsEvent = new SQSEvent();
        var message = new SQSMessage();
        message.setMessageId("error-message");
        message.setBody("{ invalid json }");
        
        var attributes = new HashMap<String, String>();
        attributes.put("ApproximateReceiveCount", "2");
        message.setAttributes(attributes);
        
        sqsEvent.setRecords(List.of(message));
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("error-message"));
    }
    
    @Test
    void shouldHandlePartialBatchFailure() throws JsonProcessingException {
        var successWorkItem = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "John")
        );
        
        var sqsEvent = new SQSEvent();
        var successMessage = new SQSMessage();
        successMessage.setMessageId("success-message");
        successMessage.setBody(objectMapper.writeValueAsString(successWorkItem));
        
        var failureMessage = new SQSMessage();
        failureMessage.setMessageId("failure-message");
        failureMessage.setBody("{ invalid json }");
        
        sqsEvent.setRecords(List.of(successMessage, failureMessage));
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("failure-message"));
    }
    
    @Test
    void shouldProcessWorkItemWithKeyValueParameters() throws JsonProcessingException {
        var parameters = objectMapper.createObjectNode();
        parameters.put("action", "reindex");
        parameters.put("priority", 1);
        parameters.put("dryRun", false);
        parameters.put("timestamp", System.currentTimeMillis());
        
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY);
        var workItem = new BatchWorkItem(dynamoDbKey, TEST_JOB_NAME, parameters);
        
        var sqsEvent = createSqsEvent(workItem);
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(mockExecutor).executeBatch(captor.capture());
        
        var capturedItems = (List<BatchWorkItem>) captor.getValue();
        assertThat(capturedItems, hasSize(1));
        
        var capturedItem = capturedItems.getFirst();
        assertThat(capturedItem.parameters(), notNullValue());
        assertThat(capturedItem.parameters().get("action").asText(), equalTo("reindex"));
        assertThat(capturedItem.parameters().get("priority").asInt(), equalTo(1));
        assertThat(capturedItem.parameters().get("dryRun").asBoolean(), equalTo(false));
        assertThat(capturedItem.parameters().has("timestamp"), equalTo(true));
    }
    
    @Test
    void shouldProcessWorkItemWithArrayParameters() throws JsonProcessingException {
        var parameters = objectMapper.createObjectNode();
        parameters.put("operation", "bulk-update");
        
        var idsArray = parameters.putArray("resourceIds");
        idsArray.add("resource-001");
        idsArray.add("resource-002");
        idsArray.add("resource-003");
        
        var tagsArray = parameters.putArray("tags");
        tagsArray.add("published");
        tagsArray.add("verified");
        
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY);
        var workItem = new BatchWorkItem(dynamoDbKey, TEST_JOB_NAME, parameters);
        
        var sqsEvent = createSqsEvent(workItem);
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(mockExecutor).executeBatch(captor.capture());
        
        var capturedItems = (List<BatchWorkItem>) captor.getValue();
        assertThat(capturedItems, hasSize(1));
        
        var capturedItem = capturedItems.getFirst();
        assertThat(capturedItem.parameters(), notNullValue());
        assertThat(capturedItem.parameters().get("operation").asText(), equalTo("bulk-update"));
        
        var resourceIds = capturedItem.parameters().get("resourceIds");
        assertThat(resourceIds.isArray(), equalTo(true));
        assertThat(resourceIds.size(), equalTo(3));
        assertThat(resourceIds.get(0).asText(), equalTo("resource-001"));
        assertThat(resourceIds.get(1).asText(), equalTo("resource-002"));
        assertThat(resourceIds.get(2).asText(), equalTo("resource-003"));
        
        var tags = capturedItem.parameters().get("tags");
        assertThat(tags.isArray(), equalTo(true));
        assertThat(tags.size(), equalTo(2));
        assertThat(tags.get(0).asText(), equalTo("published"));
        assertThat(tags.get(1).asText(), equalTo("verified"));
    }
    
    @Test
    void shouldResolveGsiQueryToPrimaryKeys() throws JsonProcessingException {
        var gsiKey = new DynamodbResourceBatchDynamoDbKey("Customer:123", "Resource:2024", BY_CUSTOMER_RESOURCE_INDEX_NAME);
        var workItem = new BatchWorkItem(gsiKey, TEST_JOB_NAME);
        
        var queryResponse = QueryResponse.builder()
                                .items(List.of(
                                    createDynamoItem("Resource:123", "Resource"),
                                    createDynamoItem("Resource:456", "Resource")))
                                .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        var sqsEvent = createSqsEvent(workItem);
        handler.handleRequest(sqsEvent, context);
        
        var queryCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(mockDynamoDbClient).query(queryCaptor.capture());
        assertThat(queryCaptor.getValue().indexName(), equalTo(BY_CUSTOMER_RESOURCE_INDEX_NAME));
        assertThat(queryCaptor.getValue().tableName(), equalTo(TEST_TABLE_NAME));

        ArgumentCaptor<List<BatchWorkItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockExecutor).executeBatch(captor.capture());
        
        var capturedItems = captor.getValue();
        assertThat(capturedItems, hasSize(2));
        assertThat(capturedItems.get(0).dynamoDbKey().partitionKey(), equalTo("Resource:123"));
        assertThat(capturedItems.get(1).dynamoDbKey().partitionKey(), equalTo("Resource:456"));
    }
    
    @Test
    void shouldHandleMixedPrimaryAndGsiKeys() throws JsonProcessingException {
        var primaryWorkItem = new BatchWorkItem(
            new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY),
            TEST_JOB_NAME
        );
        var gsiWorkItem = new BatchWorkItem(
            new DynamodbResourceBatchDynamoDbKey("CristinIdentifier#12345", "Resource:2024", RESOURCE_BY_CRISTIN_ID_INDEX_NAME),
            TEST_JOB_NAME
        );
        
        var queryResponse = QueryResponse.builder()
                                .items(List.of(createDynamoItem("Resource:789", "Resource")))
                                .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        var sqsEvent = createSqsEventWithMultipleMessages(List.of(primaryWorkItem, gsiWorkItem));
        handler.handleRequest(sqsEvent, context);
        
        verify(mockDynamoDbClient).query(any(QueryRequest.class));
        
        ArgumentCaptor<List<BatchWorkItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockExecutor).executeBatch(captor.capture());
        
        var capturedItems = captor.getValue();
        assertThat(capturedItems, hasSize(2));
        assertThat(capturedItems.get(0).dynamoDbKey().partitionKey(), equalTo(TEST_PARTITION_KEY));
        assertThat(capturedItems.get(1).dynamoDbKey().partitionKey(), equalTo("Resource:789"));
    }
    
    @Test
    void shouldPropagateExceptionWhenGsiQueryFails() throws JsonProcessingException {
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:999",
            "Resource:Failed",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, TEST_JOB_NAME);
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenThrow(DynamoDbException.builder().message("Query failed").build());
        
        var sqsEvent = createSqsEvent(workItem);
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response.getBatchItemFailures(), hasSize(1));
        verify(mockExecutor, never()).executeBatch(any(List.class));
    }
    
    @Test
    void shouldHandleGsiQueryWithPagination() throws JsonProcessingException {
        var workItem = new BatchWorkItem(
            new DynamodbResourceBatchDynamoDbKey("Customer:456", "Resource:Article", BY_CUSTOMER_RESOURCE_INDEX_NAME),
            TEST_JOB_NAME
        );
        
        var firstResponse = QueryResponse.builder()
                                .items(List.of(createDynamoItem("Resource:001", "Resource")))
                                .lastEvaluatedKey(Map.of("dummy", AttributeValue.builder().s("key").build()))
                                .build();

        var secondResponse = QueryResponse.builder()
                                 .items(List.of(createDynamoItem("Resource:002", "Resource")))
                                 .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(firstResponse)
            .thenReturn(secondResponse);
        
        var sqsEvent = createSqsEvent(workItem);
        handler.handleRequest(sqsEvent, context);
        
        verify(mockDynamoDbClient, times(2)).query(any(QueryRequest.class));
        
        ArgumentCaptor<List<BatchWorkItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockExecutor).executeBatch(captor.capture());
        
        var capturedItems = captor.getValue();
        assertThat(capturedItems, hasSize(2));
    }
    
    @Test
    void shouldHandleMultipleGsiQueries() throws JsonProcessingException {
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
        
        var workItem1 = new BatchWorkItem(gsiKey1, TEST_JOB_NAME);
        var workItem2 = new BatchWorkItem(gsiKey2, TEST_JOB_NAME);
        
        var queryResponse1 = QueryResponse.builder()
                                 .items(List.of(
                                     Map.of(
                                         "PK0", AttributeValue.builder().s("Resource:AAA").build(),
                                         "SK0", AttributeValue.builder().s("Resource").build())))
                                 .build();

        var queryResponse2 = QueryResponse.builder()
                                 .items(List.of(
                                     Map.of(
                                         "PK0", AttributeValue.builder().s("Resource:BBB").build(),
                                         "SK0", AttributeValue.builder().s("Resource").build())))
                                 .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenAnswer(invocation -> {
                QueryRequest request = invocation.getArgument(0);
                if (BY_CUSTOMER_RESOURCE_INDEX_NAME.equals(request.indexName())) {
                    return queryResponse1;
                } else if (RESOURCE_BY_CRISTIN_ID_INDEX_NAME.equals(request.indexName())) {
                    return queryResponse2;
                }
                return QueryResponse.builder().build();
            });
        
        var sqsEvent = createSqsEventWithMultipleMessages(List.of(workItem1, workItem2));
        handler.handleRequest(sqsEvent, context);
        
        verify(mockDynamoDbClient, times(2)).query(any(QueryRequest.class));
        
        ArgumentCaptor<List<BatchWorkItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockExecutor).executeBatch(captor.capture());
        
        var capturedItems = captor.getValue();
        assertThat(capturedItems, hasSize(2));
        assertThat(capturedItems.get(0).dynamoDbKey().partitionKey(), equalTo("Resource:AAA"));
        assertThat(capturedItems.get(1).dynamoDbKey().partitionKey(), equalTo("Resource:BBB"));
    }
    
    @Test
    void shouldThrowExceptionWhenGsiQueryReturnsNoResults() throws JsonProcessingException {
        var gsiKey = new DynamodbResourceBatchDynamoDbKey(
            "Customer:NoResults",
            "Resource:Empty",
            BY_CUSTOMER_RESOURCE_INDEX_NAME
        );
        var workItem = new BatchWorkItem(gsiKey, TEST_JOB_NAME);
        
        var queryResponse = QueryResponse.builder()
                                .items(List.of())
                                .build();

        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenReturn(queryResponse);
        
        var sqsEvent = createSqsEvent(workItem);
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response.getBatchItemFailures(), hasSize(1));
        verify(mockDynamoDbClient).query(any(QueryRequest.class));
        verify(mockExecutor, never()).executeBatch(any(List.class));
    }
    
    private BatchWorkItem createWorkItem(String jobType, Map<String, Object> parameters) {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY);

        return new BatchWorkItem(
            dynamoDbKey,
            jobType,
            nonNull(parameters) ? objectMapper.valueToTree(parameters) : null
        );
    }
    
    private SQSEvent createSqsEvent(BatchWorkItem workItem)
            throws JsonProcessingException {
        var sqsEvent = new SQSEvent();
        var message = new SQSMessage();
        message.setMessageId("message-id-1");
        message.setBody(objectMapper.writeValueAsString(workItem));
        sqsEvent.setRecords(List.of(message));
        return sqsEvent;
    }
    
    private SQSEvent createSqsEventWithMultipleMessages(
            List<BatchWorkItem> workItems)
            throws JsonProcessingException {
        
        var sqsEvent = new SQSEvent();
        var messages = new ArrayList<SQSMessage>();
        
        for (int i = 0; i < workItems.size(); i++) {
            var message = new SQSMessage();
            message.setMessageId("message-id-" + (i + 1));
            message.setBody(objectMapper.writeValueAsString(workItems.get(i)));
            messages.add(message);
        }
        
        sqsEvent.setRecords(messages);
        return sqsEvent;
    }
}
package no.unit.nva.publication.events.handlers.batch;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamodbResourceBatchJobHandlerTest {

    private static final String TEST_PARTITION_KEY = "USER#123";
    private static final String TEST_SORT_KEY = "PROFILE";
    private static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private static final String TEST_JOB_NAME = "TEST_JOB_NAME";

    @Mock
    private DynamodbResourceBatchJobExecutor mockExecutor;
    
    @Mock
    private Context context;
    
    private DynamodbResourceBatchJobHandler handler;
    
    @BeforeEach
    void setUp() {
        var handlers = new HashMap<String, DynamodbResourceBatchJobExecutor>();
        handlers.put(TEST_JOB_NAME, mockExecutor);
        handler = new DynamodbResourceBatchJobHandler(handlers);
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
        handler = new DynamodbResourceBatchJobHandler(new HashMap<>());
        
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
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey("AUTHOR#123", "PUBLICATION#2024", "AuthorIndex");
        var workItem = new BatchWorkItem(dynamoDbKey,
                                         TEST_JOB_NAME,
                                         objectMapper.valueToTree(Map.of("title", "Test Publication")));
        
        var sqsEvent = createSqsEvent(workItem);
        
        var response = handler.handleRequest(sqsEvent, context);
        
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        verify(mockExecutor).executeBatch(any(List.class));
        
        assertThat(workItem.dynamoDbKey().isGsiQuery(), equalTo(true));
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
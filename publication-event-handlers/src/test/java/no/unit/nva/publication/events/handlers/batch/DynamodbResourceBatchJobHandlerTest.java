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
        // Create handler with mocked executor for testing
        Map<String, DynamodbResourceBatchJobExecutor> handlers = new HashMap<>();
        handlers.put(TEST_JOB_NAME, mockExecutor);
        handler = new DynamodbResourceBatchJobHandler(handlers);
    }
    
    @Test
    void shouldProcessCreateRecordSuccessfully() throws Exception {
        // Given
        var workItem = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "John", "email", "john@example.com")
        );
        var sqsEvent = createSqsEvent(workItem);
        
        // When
        var response = handler.handleRequest(sqsEvent, context);
        
        // Then
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        verify(mockExecutor).executeBatch(any(List.class));
    }
    
    @Test
    void shouldReportBatchItemFailureWhenExecutorThrows() throws Exception {
        // Given
        var workItem = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "John")
        );
        var sqsEvent = createSqsEvent(workItem);
        
        doThrow(new RuntimeException("Execution failed"))
            .when(mockExecutor).executeBatch(any(List.class));
        
        // When
        var response = handler.handleRequest(sqsEvent, context);
        
        // Then
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("message-id-1"));
    }
    
    @Test
    void shouldReportBatchItemFailureWhenUnsupportedJobType() throws JsonProcessingException {
        // Given - handler with no executors registered
        handler = new DynamodbResourceBatchJobHandler(new HashMap<>());
        
        var workItem = createWorkItem(
            TEST_JOB_NAME,
            Map.of("name", "John")
        );
        var sqsEvent = createSqsEvent(workItem);
        
        // When
        var response = handler.handleRequest(sqsEvent, context);
        
        // Then
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("message-id-1"));
    }
    
    @Test
    void shouldReportBatchItemFailureWhenInvalidJson() {
        // Given
        var sqsEvent = new SQSEvent();
        var message = new SQSMessage();
        message.setMessageId("invalid-message");
        message.setBody("{ invalid json }");
        sqsEvent.setRecords(List.of(message));
        
        // When
        var response = handler.handleRequest(sqsEvent, context);
        
        // Then
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("invalid-message"));
    }
    
    @Test
    void shouldProcessMultipleMessagesInBatch() throws JsonProcessingException {
        // Given
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
        
        // When
        var response = handler.handleRequest(sqsEvent, context);
        
        // Then
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        // Verify batch was processed as single transaction
        verify(mockExecutor, times(1)).executeBatch(any(List.class));
    }
    
    @Test
    void shouldProcessWorkItemWithGsiKeys() throws JsonProcessingException {
        // Given
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey("AUTHOR#123", "PUBLICATION#2024", "AuthorIndex");
        var workItem = new BatchWorkItem(dynamoDbKey,
                                         TEST_JOB_NAME,
                                         objectMapper.valueToTree(Map.of("title", "Test Publication")));
        
        var sqsEvent = createSqsEvent(workItem);
        
        // When
        var response = handler.handleRequest(sqsEvent, context);
        
        // Then
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(0));
        verify(mockExecutor).executeBatch(any(List.class));
        
        // Verify the key is recognized as GSI query
        assertThat(workItem.dynamoDbKey().isGsiQuery(), equalTo(true));
    }
    
    @Test
    void shouldIncludeErrorMetadataInFailure() {
        // Given
        var sqsEvent = new SQSEvent();
        var message = new SQSMessage();
        message.setMessageId("error-message");
        message.setBody("{ invalid json }");
        
        // Add message attributes
        Map<String, String> attributes = new HashMap<>();
        attributes.put("ApproximateReceiveCount", "2");
        message.setAttributes(attributes);
        
        sqsEvent.setRecords(List.of(message));
        
        // When
        var response = handler.handleRequest(sqsEvent, context);
        
        // Then
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("error-message"));
    }
    
    @Test
    void shouldHandlePartialBatchFailure() throws JsonProcessingException {
        // Given
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
        
        // When
        var response = handler.handleRequest(sqsEvent, context);
        
        // Then
        assertThat(response, notNullValue());
        assertThat(response.getBatchItemFailures(), hasSize(1));
        assertThat(response.getBatchItemFailures().getFirst().getItemIdentifier(),
                  equalTo("failure-message"));
    }
    
    private BatchWorkItem createWorkItem(String jobType, Map<String, Object> parameters) {
        var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(TEST_PARTITION_KEY, TEST_SORT_KEY, null);

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
        var messages = new java.util.ArrayList<SQSMessage>();
        
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
package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;

@ExtendWith(MockitoExtension.class)
class LoadDynamodbResourceBatchJobHandlerTest {

    private static final String TEST_TABLE_NAME = "test-table";
    private static final String TEST_QUEUE_URL = "https://sqs.test.amazonaws.com/test-queue";
    private static final String TEST_JOB_TYPE = "TEST_JOB";
    private static final String TEST_FUNCTION_ARN = "arn:aws:lambda:region:account:function:test";
    private static final String PARALLEL_THREADS = "2";
    private static final String PROCESSING_ENABLED = "true";
    private static final String PROCESSING_DISABLED = "false";

    private AmazonDynamoDB dynamoDbClient;
    private SqsClient sqsClient;
    private EventBridgeClient eventBridgeClient;
    private LoadDynamodbResourceBatchJobHandler handler;
    private Context context;
    private AwsEventBridgeEvent<LoadDynamodbRequest> event;

    @BeforeEach
    void setup() {
        dynamoDbClient = mock(AmazonDynamoDB.class);
        sqsClient = mock(SqsClient.class);
        eventBridgeClient = mock(EventBridgeClient.class);
        context = mock(Context.class);
        event = mock(AwsEventBridgeEvent.class);

        handler = new LoadDynamodbResourceBatchJobHandler(
            dynamoDbClient, sqsClient, eventBridgeClient, TEST_TABLE_NAME, TEST_QUEUE_URL,
            PROCESSING_ENABLED, PARALLEL_THREADS);
    }

    @Test
    void shouldThrowExceptionWhenJobTypeIsMissing() {
        var request = new LoadDynamodbRequest(null);
        
        var exception = assertThrows(IllegalArgumentException.class, 
            () -> handler.processInput(request, event, context));
        
        assertThat(exception.getMessage(), containsString("Missing required field 'jobType'"));
        assertThat(exception.getMessage(), containsString("Example input"));
        
        verifyNoInteractions(dynamoDbClient);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldProcessBatchAndSendToQueue() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);
        
        var items = createTestItems(12);
        
        var scanResult = new ScanResult()
            .withItems(items);
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResult);
        
        var successResponse = createSuccessResponse(10);
        var successResponse2 = createSuccessResponse(2);
        
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(successResponse)
            .thenReturn(successResponse2);
        
        var response = handler.processInput(request, event, context);
        
        assertThat(response.itemsProcessed(), is(equalTo(12)));
        assertThat(response.messagesQueued(), is(equalTo(12)));
        assertThat(response.jobType(), is(equalTo(TEST_JOB_TYPE)));
        
        verify(sqsClient, times(2)).sendMessageBatch(any(SendMessageBatchRequest.class));
        verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void shouldTriggerNextBatchWhenMoreItemsExist() {
        when(context.getInvokedFunctionArn()).thenReturn(TEST_FUNCTION_ARN);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);
        
        var items = createTestItems(5);
        
        var lastKey = createDynamoDbKey("pk4", "sk4");
        
        var scanResult = new ScanResult()
            .withItems(items)
            .withLastEvaluatedKey(lastKey);
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResult);
        
        var successResponse = createSuccessResponse(5);
        
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(successResponse);
        
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(PutEventsResponse.builder().build());
        
        var response = handler.processInput(request, event, context);
        
        assertThat(response.itemsProcessed(), is(equalTo(5)));
        assertThat(response.messagesQueued(), is(equalTo(5)));
        
        verify(sqsClient, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
        verify(eventBridgeClient, times(1)).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void shouldHandleKillSwitchWhenProcessingDisabled() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);
        
        var disabledHandler = new LoadDynamodbResourceBatchJobHandler(
            dynamoDbClient, sqsClient, eventBridgeClient, TEST_TABLE_NAME, TEST_QUEUE_URL, PROCESSING_DISABLED, PARALLEL_THREADS);
        
        var response = disabledHandler.processInput(request, event, context);
        
        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        assertThat(response.jobType(), is(equalTo(TEST_JOB_TYPE)));
        
        verifyNoInteractions(dynamoDbClient);
        verifyNoInteractions(sqsClient);
        verifyNoInteractions(eventBridgeClient);
    }

    @Test
    void shouldHandleEmptyScanResult() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);
        
        var emptyScanResult = new ScanResult()
            .withItems(Collections.emptyList());
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(emptyScanResult);
        
        var response = handler.processInput(request, event, context);
        
        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        
        verify(sqsClient, never()).sendMessageBatch(any(SendMessageBatchRequest.class));
        verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void shouldHandlePartialSqsBatchFailures() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);
        
        var items = createTestItems(5);
        
        var scanResult = new ScanResult()
            .withItems(items);
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResult);
        
        var partialFailureResponse = SendMessageBatchResponse.builder()
            .successful(List.of(
                SendMessageBatchResultEntry.builder().id("0").build(),
                SendMessageBatchResultEntry.builder().id("1").build(),
                SendMessageBatchResultEntry.builder().id("2").build()
            ))
            .failed(List.of(
                BatchResultErrorEntry.builder()
                    .id("3")
                    .code("ServiceUnavailable")
                    .message("Service temporarily unavailable")
                    .build(),
                BatchResultErrorEntry.builder()
                    .id("4")
                    .code("ThrottlingException")
                    .message("Rate exceeded")
                    .build()
            ))
            .build();
        
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(partialFailureResponse);
        
        var response = handler.processInput(request, event, context);
        
        assertThat(response.itemsProcessed(), is(equalTo(5)));
        assertThat(response.messagesQueued(), is(equalTo(3)));
        
        verify(sqsClient, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldHandleSqsExceptionGracefully() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);
        
        var items = createTestItems(3);
        
        var scanResult = new ScanResult()
            .withItems(items);
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResult);
        
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenThrow(new RuntimeException("SQS service error"));
        
        var response = handler.processInput(request, event, context);
        
        assertThat(response.itemsProcessed(), is(equalTo(3)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        
        verify(sqsClient, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldHandleDynamoDbScanException() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);
        
        when(dynamoDbClient.scan(any(ScanRequest.class)))
            .thenThrow(new RuntimeException("DynamoDB scan failed"));
        
        assertThrows(RuntimeException.class, 
            () -> handler.processInput(request, event, context));
        
        verify(sqsClient, never()).sendMessageBatch(any(SendMessageBatchRequest.class));
        verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void shouldProcessWithExistingStartMarker() {
        var startMarker = createDynamoDbKey("pk-start", "sk-start");
        
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, startMarker);
        
        var items = createTestItems(3);
        
        var scanResult = new ScanResult()
            .withItems(items);
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResult);
        
        var successResponse = createSuccessResponse(3);
        
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(successResponse);
        
        var response = handler.processInput(request, event, context);
        
        assertThat(response.itemsProcessed(), is(equalTo(3)));
        assertThat(response.messagesQueued(), is(equalTo(3)));
        
        verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void shouldProcessWhenKillSwitchEnabledByDefault() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);
        
        var items = createTestItems(1);
        
        var scanResult = new ScanResult()
            .withItems(items);
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResult);
        
        var successResponse = createSuccessResponse(1);
        
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(successResponse);
        
        var response = handler.processInput(request, event, context);
        
        assertThat(response.itemsProcessed(), is(equalTo(1)));
        assertThat(response.messagesQueued(), is(equalTo(1)));
        
        verify(dynamoDbClient, times(1)).scan(any(ScanRequest.class));
        verify(sqsClient, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldHandleNullInputGracefully() {
        assertThrows(IllegalArgumentException.class, 
            () -> handler.processInput(null, event, context));
        
        verifyNoInteractions(dynamoDbClient);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldHandleBlankJobTypeAsInvalid() {
        var request = new LoadDynamodbRequest("   ");
        
        var exception = assertThrows(IllegalArgumentException.class, 
            () -> handler.processInput(request, event, context));
        
        assertThat(exception.getMessage(), containsString("Missing required field 'jobType'"));
        
        verifyNoInteractions(dynamoDbClient);
        verifyNoInteractions(sqsClient);
    }
    
    private static SendMessageBatchResponse createSuccessResponse(int count) {
        var successfulEntries = IntStream.range(0, count)
            .mapToObj(i -> SendMessageBatchResultEntry.builder().id(String.valueOf(i)).build())
            .toList();
        
        return SendMessageBatchResponse.builder()
            .successful(successfulEntries)
            .failed(List.of())
            .build();
    }
    
    private static List<Map<String, AttributeValue>> createTestItems(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createDynamoDbKey("pk" + i, "sk" + i))
            .toList();
    }
    
    private static Map<String, AttributeValue> createDynamoDbKey(String partitionKey, String sortKey) {
        return Map.of(
            PRIMARY_KEY_PARTITION_KEY_NAME, new AttributeValue(partitionKey),
            PRIMARY_KEY_SORT_KEY_NAME, new AttributeValue(sortKey)
        );
    }
}
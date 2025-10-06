package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Collections.emptyList;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.IntStream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.KeyField;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
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
class LoadDynamodbResourceBatchJobHandlerTest extends ResourcesLocalTest {

    private static final String TEST_QUEUE_URL = "https://sqs.test.amazonaws.com/test-queue";
    private static final String TEST_JOB_TYPE = "TEST_JOB";
    private static final String TEST_FUNCTION_ARN = "arn:aws:lambda:region:account:function:test";
    private static final String PROCESSING_ENABLED = "true";
    private static final String PROCESSING_DISABLED = "false";
    private static final int SQS_BATCH_SIZE = 10;
    private static final int SCAN_PAGE_SIZE = 1000;

    private SqsClient sqsClient;
    private EventBridgeClient eventBridgeClient;
    private LoadDynamodbResourceBatchJobHandler handler;
    private Context context;
    private AwsEventBridgeEvent<LoadDynamodbRequest> event;
    private ResourceService resourceService;

    @BeforeEach
    void setup() {
        super.init();
        resourceService = spy(getResourceService(client));
        sqsClient = mock(SqsClient.class);
        eventBridgeClient = mock(EventBridgeClient.class);
        context = mock(Context.class);
        event = mock(AwsEventBridgeEvent.class);

        handler = new LoadDynamodbResourceBatchJobHandler(
            sqsClient, eventBridgeClient, TEST_QUEUE_URL,
            PROCESSING_ENABLED, resourceService,
            SCAN_PAGE_SIZE,
            SQS_BATCH_SIZE);
    }

    @Test
    void shouldThrowExceptionWhenJobTypeIsMissing() {
        var request = new LoadDynamodbRequest(null);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Missing required field 'jobType'"));
        assertThat(exception.getMessage(), containsString("Example input"));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldProcessBatchAndSendToQueue() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE));

        createTestItems(12);

        var successResponse = createSuccessResponse(10);
        var successResponse2 = createSuccessResponse(2);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(successResponse)
            .thenReturn(successResponse2);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(greaterThan(1)));
        assertThat(response.messagesQueued(),  is(greaterThan(1)));
        assertThat(response.jobType(), is(equalTo(TEST_JOB_TYPE)));

        verify(sqsClient, times(2)).sendMessageBatch(any(SendMessageBatchRequest.class));
        verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void shouldTriggerNextBatchWhenMoreItemsExist() {
        when(context.getInvokedFunctionArn()).thenReturn(TEST_FUNCTION_ARN);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE));

        createTestItems(21);

        var successResponse = createSuccessResponse(10);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(successResponse);

        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(PutEventsResponse.builder().build());

        var smallScanPAge = 10;

        handler = new LoadDynamodbResourceBatchJobHandler(
            sqsClient, eventBridgeClient, TEST_QUEUE_URL,
            PROCESSING_ENABLED, resourceService,
            smallScanPAge,
            SQS_BATCH_SIZE);

        handler.processInput(request, event, context);

        verify(eventBridgeClient, atLeastOnce()).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void shouldHandleKillSwitchWhenProcessingDisabled() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE));

        var disabledHandler = new LoadDynamodbResourceBatchJobHandler(
            sqsClient, eventBridgeClient, TEST_QUEUE_URL, PROCESSING_DISABLED,
            resourceService, SCAN_PAGE_SIZE, SQS_BATCH_SIZE);

        var response = disabledHandler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        assertThat(response.jobType(), is(equalTo(TEST_JOB_TYPE)));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
        verifyNoInteractions(eventBridgeClient);
    }

    @Test
    void shouldHandleEmptyScanResult() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));

        verify(sqsClient, never()).sendMessageBatch(any(SendMessageBatchRequest.class));
        verify(eventBridgeClient, never()).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void shouldHandlePartialSqsBatchFailures() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE));

        createTestItems(5);

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
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE));

        createTestItems(3);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenThrow(new RuntimeException("SQS service error"));

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(3)));
        assertThat(response.messagesQueued(), is(equalTo(0)));

        verify(sqsClient, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldProcessWhenKillSwitchEnabledByDefault() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE));

        createTestItems(1);

        var successResponse = createSuccessResponse(1);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(successResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(1)));
        assertThat(response.messagesQueued(), is(equalTo(1)));

        verify(resourceService, times(1)).scanResourcesRaw(anyInt(), any(), any());
        verify(sqsClient, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldHandleNullInputGracefully() {
        assertThrows(IllegalArgumentException.class,
                     () -> handler.processInput(null, event, context));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldHandleBlankJobTypeAsInvalid() {
        var request = new LoadDynamodbRequest("   ");

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Missing required field 'jobType'"));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldHandleParallelProcessingFailureGracefully() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE));

        createTestItems(15);

        var mockSqsClient = mock(SqsClient.class);
        when(mockSqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenThrow(new RuntimeException("Simulated parallel processing error"))
            .thenReturn(createSuccessResponse(10))
            .thenReturn(createSuccessResponse(5));

        var testHandler = new LoadDynamodbResourceBatchJobHandler(
            mockSqsClient, eventBridgeClient, TEST_QUEUE_URL,
            PROCESSING_ENABLED, resourceService, SCAN_PAGE_SIZE, SQS_BATCH_SIZE);

        var response = testHandler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(15)));

        verify(mockSqsClient, atLeast(2)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldReturnZeroWhenWorkItemsListIsEmpty() throws Exception {
        Method sendBatchToQueueMethod = LoadDynamodbResourceBatchJobHandler.class
                                            .getDeclaredMethod("sendBatchToQueue", List.class);
        sendBatchToQueueMethod.setAccessible(true);

        Object result = sendBatchToQueueMethod.invoke(handler, emptyList());

        assertThat((Integer) result, is(equalTo(0)));
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldHandleSerializationException() throws Exception {
        Method getSendMessageBatchRequestEntry = LoadDynamodbResourceBatchJobHandler.class
                                                     .getDeclaredMethod("getSendMessageBatchRequestEntry",
                                                                        BatchWorkItem.class);
        getSendMessageBatchRequestEntry.setAccessible(true);

        var mockKey = mock(DynamodbResourceBatchDynamoDbKey.class);
        when(mockKey.partitionKey()).thenThrow(new RuntimeException("Serialization trigger"));

        var problematicItem = new BatchWorkItem(mockKey, TEST_JOB_TYPE);

        Exception exception = assertThrows(InvocationTargetException.class,
                                           () -> getSendMessageBatchRequestEntry.invoke(handler, problematicItem));

        assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        assertThat(exception.getCause().getMessage(), containsString("Failed to serialize work item"));
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

    private void createTestItems(int count) {
        IntStream.range(0, count)
            .mapToObj(i -> randomPublication())
            .forEach(publication ->
                         attempt(() -> Resource.fromPublication(publication).persistNew(resourceService,
                                                                                        UserInstance.fromPublication(
                                                                                            publication))).orElseThrow());
    }
}
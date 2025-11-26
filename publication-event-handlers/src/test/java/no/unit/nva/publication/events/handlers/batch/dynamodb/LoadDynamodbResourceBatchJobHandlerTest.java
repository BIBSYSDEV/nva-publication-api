package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.stream.IntStream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.KeyField;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeEventBridgeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private static final int TOTAL_SEGMENTS = 2;
    private static final int SEGMENT = 0;
    private static final int ONE_TOTAL_SEGMENTS = 1;

    private SqsClient sqsClient;
    private FakeEventBridgeClient eventBridgeClient;
    private LoadDynamodbResourceBatchJobHandler handler;
    private Context context;
    private AwsEventBridgeEvent<LoadDynamodbRequest> event;
    private ResourceService resourceService;

    @BeforeEach
    void setup() {
        super.init();
        resourceService = spy(getResourceService(client));
        sqsClient = mock(SqsClient.class);
        eventBridgeClient = new FakeEventBridgeClient();
        context = mock(Context.class);
        event = mock(AwsEventBridgeEvent.class);

        handler = new LoadDynamodbResourceBatchJobHandler(
            sqsClient, eventBridgeClient, TEST_QUEUE_URL,
            PROCESSING_ENABLED, resourceService, new BatchFilterService(),
            SCAN_PAGE_SIZE,
            SQS_BATCH_SIZE,
            TOTAL_SEGMENTS);
    }

    @Test
    void shouldThrowExceptionWhenJobTypeIsMissing() {
        var request = new LoadDynamodbRequest(null, null, List.of(KeyField.RESOURCE), SEGMENT, ONE_TOTAL_SEGMENTS, null);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Missing required field 'jobType'"));
        assertThat(exception.getMessage(), containsString("Example input"));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldProcessBatchAndSendToQueue() {
        var request = getRequest();

        createTestItems(1);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(greaterThanOrEqualTo(1)));
        assertThat(response.messagesQueued(),  is(greaterThanOrEqualTo(1)));
        assertThat(response.jobType(), is(equalTo(TEST_JOB_TYPE)));

        verify(sqsClient, atLeast(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
        assertThat(eventBridgeClient.getRequestEntries().size(), is(greaterThanOrEqualTo(0)));
    }

    @Test
    void shouldTriggerNextBatchWhenMoreItemsExist() {
        when(context.getInvokedFunctionArn()).thenReturn(TEST_FUNCTION_ARN);
        var request = getRequest();

        createTestItems(21);

        lenient().when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var smallScanPAge = 10;

        handler = new LoadDynamodbResourceBatchJobHandler(
            sqsClient, eventBridgeClient, TEST_QUEUE_URL,
            PROCESSING_ENABLED, resourceService, new BatchFilterService(),
            smallScanPAge,
            SQS_BATCH_SIZE,
            TOTAL_SEGMENTS);

        handler.processInput(request, event, context);

        assertThat(eventBridgeClient.getRequestEntries().size(), is(greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldHandleKillSwitchWhenProcessingDisabled() {
        var request = getRequest();

        var disabledHandler = new LoadDynamodbResourceBatchJobHandler(
            sqsClient, eventBridgeClient, TEST_QUEUE_URL, PROCESSING_DISABLED,
            resourceService, new BatchFilterService(), SCAN_PAGE_SIZE, SQS_BATCH_SIZE, TOTAL_SEGMENTS);

        var response = disabledHandler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        assertThat(response.jobType(), is(equalTo(TEST_JOB_TYPE)));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);

        assertThat(eventBridgeClient.getRequestEntries().size(), is(equalTo(0)));
    }

    @Test
    void shouldHandleEmptyScanResult() {
        var request = getRequest();

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));

        verify(sqsClient, never()).sendMessageBatch(any(SendMessageBatchRequest.class));

        assertThat(eventBridgeClient.getRequestEntries().size(), is(equalTo(0)));
    }

    @Test
    void shouldInitiateParallelScan() {
        when(context.getInvokedFunctionArn()).thenReturn(TEST_FUNCTION_ARN);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), null, null, null);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        assertThat(response.jobType(), is(equalTo(TEST_JOB_TYPE)));

        assertThat(eventBridgeClient.getRequestEntries().size(), is(equalTo(TOTAL_SEGMENTS)));
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldHandlePartialSqsBatchFailures() {
        var request = getRequest();

        createTestItems(50);

        var partialFailureResponse = SendMessageBatchResponse.builder()
                                         .successful(List.of(
                                             SendMessageBatchResultEntry.builder().id("0").build()
                                         ))
                                         .failed(List.of(
                                             BatchResultErrorEntry.builder()
                                                 .id("1")
                                                 .code("ServiceUnavailable")
                                                 .message("Service temporarily unavailable")
                                                 .build()
                                         ))
                                         .build();

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(partialFailureResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(greaterThan(0)));
        assertThat(response.messagesQueued(), is(greaterThan(0)));

        verify(sqsClient, atLeast(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldHandleSqsExceptionGracefully() {
        var request = getRequest();

        createTestItems(50);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenThrow(new RuntimeException("SQS service error"));

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(greaterThan(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));

        verify(sqsClient, atLeast(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldProcessWhenKillSwitchEnabledByDefault() {
        var request = getRequest();

        createTestItems(50);

        var successResponse = createSendMessageBatchResponse(10);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenReturn(successResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(greaterThan(0)));
        assertThat(response.messagesQueued(), is(greaterThan(0)));

        verify(resourceService, times(1)).scanResourcesRaw(anyInt(), any(), any(), any(), any());
        verify(sqsClient, atLeast(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
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
        var request = new LoadDynamodbRequest("   ", null, List.of(KeyField.RESOURCE), SEGMENT, ONE_TOTAL_SEGMENTS, null);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Missing required field 'jobType'"));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldHandleParallelProcessingFailureGracefully() {
        var request = getRequest();

        createTestItems(50);

        var mockSqsClient = mock(SqsClient.class);
        when(mockSqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenThrow(new RuntimeException("Simulated parallel processing error"))
            .thenAnswer(this::createSendMessageBatchResponse)
            .thenReturn(createSendMessageBatchResponse(5));

        var testHandler = new LoadDynamodbResourceBatchJobHandler(
            mockSqsClient, eventBridgeClient, TEST_QUEUE_URL,
            PROCESSING_ENABLED, resourceService, new BatchFilterService(), SCAN_PAGE_SIZE, SQS_BATCH_SIZE, TOTAL_SEGMENTS);

        var response = testHandler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(greaterThan(0)));

        verify(mockSqsClient, atLeast(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    private static LoadDynamodbRequest getRequest() {
        return new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT, ONE_TOTAL_SEGMENTS, null);
    }

    private SendMessageBatchResponse createSendMessageBatchResponse(InvocationOnMock invocation) {
        SendMessageBatchRequest batchRequest = invocation.getArgument(0);
        return createSendMessageBatchResponse(batchRequest.entries().size());
    }

    private static SendMessageBatchResponse createSendMessageBatchResponse(int count) {
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

    private void createTestItemsWithYear(int count, String year) {
        IntStream.range(0, count)
            .mapToObj(i -> {
                var publication = randomPublication();
                var publicationDate = new PublicationDate.Builder().withYear(year).build();
                publication.getEntityDescription().setPublicationDate(publicationDate);
                return publication;
            })
            .forEach(publication ->
                         attempt(() -> Resource.fromPublication(publication).persistNew(resourceService,
                                                                                        UserInstance.fromPublication(
                                                                                            publication))).orElseThrow());
    }

    private void createPublishedTestItems(int count) {
        IntStream.range(0, count)
            .mapToObj(i -> randomPublication())
            .forEach(publication -> Resource.fromPublication(publication)
                         .importResource(resourceService, ImportSource.fromSource(Source.CRISTIN)));
    }

    @Test
    void shouldFilterByPublicationYear() {
        var targetYear = "2025";
        var filter = new BatchFilter(targetYear, null, null, null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        createTestItemsWithYear(3, targetYear);
        createTestItemsWithYear(2, "2024");

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(3)));
    }

    @Test
    void shouldFilterByMultiplePublicationYears() {
        var filter = new BatchFilter(null, List.of("2024", "2025"), null, null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        createTestItemsWithYear(2, "2025");
        createTestItemsWithYear(3, "2024");
        createTestItemsWithYear(1, "2023");

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(5)));
    }

    @Test
    void shouldFilterByStatus() {
        var filter = new BatchFilter(null, null, "DRAFT", null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        createTestItems(3);
        createPublishedTestItems(2);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(3)));
    }

    @Test
    void shouldFilterByMultipleStatuses() {
        var filter = new BatchFilter(null, null, null, List.of("DRAFT", "PUBLISHED"));
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        createTestItems(3);
        createPublishedTestItems(2);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(5)));
    }

    @Test
    void shouldFilterByCombinedYearAndStatus() {
        var filter = new BatchFilter("2025", null, "DRAFT", null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        createTestItemsWithYear(3, "2025");
        createTestItemsWithYear(2, "2024");

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(3)));
    }

    @Test
    void shouldReturnAllItemsWhenFilterIsNull() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, null);

        createTestItems(5);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(5)));
    }

    @Test
    void shouldReturnAllItemsWhenFilterIsEmpty() {
        var filter = new BatchFilter(null, null, null, null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        createTestItems(5);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(5)));
    }

    @Test
    void shouldThrowExceptionForInvalidYearFormat() {
        var filter = new BatchFilter("invalid", null, null, null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Invalid publicationYear format"));
        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldThrowExceptionForInvalidYearInList() {
        var filter = new BatchFilter(null, List.of("2025", "invalid"), null, null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Invalid publicationYear format"));
        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldThrowExceptionForInvalidStatus() {
        var filter = new BatchFilter(null, null, "INVALID_STATUS", null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Invalid status value"));
        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldThrowExceptionForInvalidStatusInList() {
        var filter = new BatchFilter(null, null, null, List.of("DRAFT", "INVALID_STATUS"));
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Invalid status value"));
        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldPropagateFilterToParallelSegments() {
        when(context.getInvokedFunctionArn()).thenReturn(TEST_FUNCTION_ARN);
        var filter = new BatchFilter("2025", null, "PUBLISHED", null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              null, null, filter);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(eventBridgeClient.getRequestEntries().size(), is(equalTo(TOTAL_SEGMENTS)));

        eventBridgeClient.getRequestEntries().forEach(entry -> {
            var segmentRequest = attempt(() -> JsonUtils.dtoObjectMapper.readValue(
                entry.detail(), LoadDynamodbRequest.class)).orElseThrow();
            assertThat(segmentRequest.filter().publicationYear(), is(equalTo("2025")));
            assertThat(segmentRequest.filter().status(), is(equalTo("PUBLISHED")));
        });
    }

    @Test
    void shouldFilterOutItemsWithNoMatchingYear() {
        var filter = new BatchFilter("9999", null, null, null);
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE),
                                              SEGMENT, ONE_TOTAL_SEGMENTS, filter);

        createTestItems(5);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        verify(sqsClient, never()).sendMessageBatch(any(SendMessageBatchRequest.class));
    }
}
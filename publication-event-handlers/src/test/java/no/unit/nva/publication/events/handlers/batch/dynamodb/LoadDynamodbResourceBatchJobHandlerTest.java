package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.model.storage.DynamoEntry.CONTAINED_DATA_FIELD_NAME;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
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
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import no.unit.nva.publication.model.ScanResultWrapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeEvent;
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
            PROCESSING_ENABLED, resourceService,
            SCAN_PAGE_SIZE,
            SQS_BATCH_SIZE,
            TOTAL_SEGMENTS);
    }

    @Test
    void shouldThrowExceptionWhenJobTypeIsMissing() {
        var request = new LoadDynamodbRequest(null, null, List.of(KeyField.RESOURCE), SEGMENT, ONE_TOTAL_SEGMENTS,
                                              null);

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
            PROCESSING_ENABLED, resourceService,
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
            resourceService, SCAN_PAGE_SIZE, SQS_BATCH_SIZE, TOTAL_SEGMENTS);

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
        var request = new LoadDynamodbRequest("   ", null, List.of(KeyField.RESOURCE), SEGMENT, ONE_TOTAL_SEGMENTS,
                                              null);

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
            PROCESSING_ENABLED, resourceService, SCAN_PAGE_SIZE, SQS_BATCH_SIZE, TOTAL_SEGMENTS);

        var response = testHandler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(greaterThan(0)));

        verify(mockSqsClient, atLeast(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldFilterItemsWithValidJsonPathExpression() {
        var filter = "$[?(@.status == 'PUBLISHED')]";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, filter);

        createTestItems(10);

        lenient().when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(lessThan(10)));
        assertThat(response.messagesQueued(), is(greaterThanOrEqualTo(0)));
    }

    @Test
    void shouldReturnAllItemsWhenFilterIsNull() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, null);

        createTestItems(5);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(5)));
        assertThat(response.messagesQueued(), is(equalTo(5)));
    }

    @Test
    void shouldReturnAllItemsWhenFilterIsBlank() {
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, "   ");

        createTestItems(5);

        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(5)));
        assertThat(response.messagesQueued(), is(equalTo(5)));
    }

    @Test
    void shouldHandleFilterWithNonExistentPathsGracefully() {
        var filter = "$[?(@.entityDescription.publicationDate.year >= 2099)]";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, filter);

        createTestItems(10);

        lenient().when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        verify(sqsClient, never()).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldThrowExceptionForInvalidFilterSyntax() {
        var invalidFilter = "$[invalid syntax without closing bracket";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, invalidFilter);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Invalid JsonPath filter expression"));
        assertThat(exception.getMessage(), containsString(invalidFilter));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
        assertThat(eventBridgeClient.getRequestEntries().size(), is(equalTo(0)));
    }

    @Test
    void shouldThrowExceptionForInvalidFilterBeforeInitiatingParallelScan() {
        var invalidFilter = "this is not valid jsonpath";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), null, null,
                                              invalidFilter);

        var exception = assertThrows(IllegalArgumentException.class,
                                     () -> handler.processInput(request, event, context));

        assertThat(exception.getMessage(), containsString("Invalid JsonPath filter expression"));
        assertThat(exception.getMessage(), containsString(invalidFilter));

        verifyNoInteractions(resourceService);
        verifyNoInteractions(sqsClient);
        assertThat(eventBridgeClient.getRequestEntries().size(), is(equalTo(0)));
    }

    @Test
    void shouldPropagateFilterToParallelSegments() {
        when(context.getInvokedFunctionArn()).thenReturn(TEST_FUNCTION_ARN);
        var filter = "$[?(@.status == 'PUBLISHED')]";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), null, null, filter);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        assertThat(eventBridgeClient.getRequestEntries().size(), is(equalTo(TOTAL_SEGMENTS)));

        eventBridgeClient.getRequestEntries().forEach(entry -> {
            var segmentRequest = attempt(() -> JsonUtils.dtoObjectMapper.readValue(entry.detail(),
                                                                                    LoadDynamodbRequest.class))
                                     .orElseThrow();
            assertThat(segmentRequest.filter(), is(equalTo(filter)));
        });

        verifyNoInteractions(sqsClient);
    }

    private static LoadDynamodbRequest getRequest() {
        return new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT, ONE_TOTAL_SEGMENTS,
                                       null);
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

    @Test
    void shouldConvertCompressedDataToJson() {
        createTestItems(1);

        var scan = resourceService.scanResourcesRaw(10, null, List.of(KeyField.RESOURCE), 0, 1);

        assertThat(scan.items().size(), is(greaterThan(0)));

        var firstItem = scan.items().iterator().next();
        var filter = "$[?(@.data.status)]";

        var compiledPath = com.jayway.jsonpath.JsonPath.compile(filter);
        var configuration = com.jayway.jsonpath.Configuration.defaultConfiguration();

        var jsonString = LoadDynamodbResourceBatchJobHandler.isCompressedData(firstItem)
                             ? LoadDynamodbResourceBatchJobHandler.convertCompressedItemToJson(firstItem)
                             : LoadDynamodbResourceBatchJobHandler.convertUncompressedItemToJson(firstItem);

        assertThat(jsonString, containsString("status"));
        assertThat(jsonString, containsString("data"));

        var result = compiledPath.read(jsonString, configuration);
        assertThat(result, is(not(equalTo("[]"))));
    }

    @Test
    void shouldConvertUncompressedDataToJson() {
        var uncompressedData = createUncompressedItem("test-id", "PublicationLogEntry", "PublicationUpdated");

        assertThat(LoadDynamodbResourceBatchJobHandler.isCompressedData(uncompressedData), is(false));

        var jsonString = LoadDynamodbResourceBatchJobHandler.convertUncompressedItemToJson(uncompressedData);

        assertThat(jsonString, containsString("identifier"));
        assertThat(jsonString, containsString("test-id"));
        assertThat(jsonString, containsString("PublicationLogEntry"));

        var filter = "$[?(@.data.type == 'PublicationLogEntry')]";
        var compiledPath = com.jayway.jsonpath.JsonPath.compile(filter);
        var configuration = com.jayway.jsonpath.Configuration.defaultConfiguration();

        var result = compiledPath.read(jsonString, configuration);
        assertThat(result, is(not(equalTo("[]"))));
    }

    @Test
    void shouldDetectCompressedVsUncompressedData() {
        createTestItems(1);

        var scan = resourceService.scanResourcesRaw(10, null, List.of(KeyField.RESOURCE), 0, 1);
        var compressedItem = scan.items().iterator().next();
        var uncompressedItem = createUncompressedItem("test-id", "TestType", null);

        assertThat(LoadDynamodbResourceBatchJobHandler.isCompressedData(compressedItem), is(true));
        assertThat(LoadDynamodbResourceBatchJobHandler.isCompressedData(uncompressedItem), is(false));
    }

    @Test
    void shouldHandlePathNotFoundExceptionWhenFilteringItems() {
        var filter = "$.nonExistent.deeply.nested.path";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, filter);

        createTestItems(5);

        lenient().when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
    }

    @Test
    void shouldHandleExceptionDuringJsonConversionWithCorruptedCompressedData() {
        var corruptedItem = createCorruptedCompressedItem();
        var corruptedScanResult = new ScanResultWrapper(List.of(corruptedItem), null, false);

        org.mockito.Mockito.doReturn(corruptedScanResult)
            .when(resourceService).scanResourcesRaw(anyInt(), any(), any(), anyInt(), anyInt());

        var filter = "$[?(@.data)]";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, filter);

        lenient().when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
    }

    @Test
    void shouldHandleEmptyWorkItemsListWhenAllItemsFilteredOut() {
        var filter = "$[?(@.nonExistentFieldThatNoItemHas == 'impossible-value')]";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, filter);

        createTestItems(3);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
        verify(sqsClient, never()).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldFilterUncompressedItemsCorrectly() {
        var uncompressedItem = createUncompressedItemWithMatchField("test-id", "TestType", "targetValue");

        var scanResult = new ScanResultWrapper(List.of(uncompressedItem), null, false);

        org.mockito.Mockito.doReturn(scanResult)
            .when(resourceService).scanResourcesRaw(anyInt(), any(), any(), anyInt(), anyInt());

        var filter = "$[?(@.data.matchField == 'targetValue')]";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, filter);

        lenient().when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(this::createSendMessageBatchResponse);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(1)));
        verify(sqsClient, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void shouldDetectDataAttributeWithoutBinaryAsUncompressed() {
        var itemWithDataMapNotBinary = createItemWithDataFieldAsMap();

        assertThat(LoadDynamodbResourceBatchJobHandler.isCompressedData(itemWithDataMapNotBinary), is(false));
    }

    @Test
    void shouldHandleJsonPathReturningNullForDefinitePath() {
        var uncompressedItem = createUncompressedItem("test-id", "TestType", null);
        var scanResult = new ScanResultWrapper(List.of(uncompressedItem), null, false);

        org.mockito.Mockito.doReturn(scanResult)
            .when(resourceService).scanResourcesRaw(anyInt(), any(), any(), anyInt(), anyInt());

        var filter = "$.nonExistent";
        var request = new LoadDynamodbRequest(TEST_JOB_TYPE, null, List.of(KeyField.RESOURCE), SEGMENT,
                                              ONE_TOTAL_SEGMENTS, filter);

        var response = handler.processInput(request, event, context);

        assertThat(response.itemsProcessed(), is(equalTo(0)));
        assertThat(response.messagesQueued(), is(equalTo(0)));
    }

    private static Map<String, AttributeValue> createItemWithDataFieldAsMap() {
        return Map.of(
            CONTAINED_DATA_FIELD_NAME, new AttributeValue().withM(Map.of(
                "someKey", new AttributeValue().withS("someValue")
            )),
            "PK0", new AttributeValue().withS("Resource#customer#owner"),
            "SK0", new AttributeValue().withS("Resource#identifier")
        );
    }

    private static Map<String, AttributeValue> createUncompressedItem(String identifier, String type,
                                                                       String additionalField) {
        var dataContent = new java.util.HashMap<String, AttributeValue>();
        dataContent.put("identifier", new AttributeValue().withS(identifier));
        dataContent.put("type", new AttributeValue().withS(type));
        if (additionalField != null) {
            dataContent.put("additionalField", new AttributeValue().withS(additionalField));
        }
        return Map.of(
            "data", new AttributeValue().withM(dataContent),
            "PK0", new AttributeValue().withS("Resource#customer#owner"),
            "SK0", new AttributeValue().withS("Resource#identifier")
        );
    }

    private static Map<String, AttributeValue> createUncompressedItemWithMatchField(String identifier, String type,
                                                                                     String matchFieldValue) {
        return Map.of(
            "data", new AttributeValue().withM(Map.of(
                "identifier", new AttributeValue().withS(identifier),
                "type", new AttributeValue().withS(type),
                "matchField", new AttributeValue().withS(matchFieldValue)
            )),
            "PK0", new AttributeValue().withS("Resource#customer#owner"),
            "SK0", new AttributeValue().withS("Resource#identifier")
        );
    }

    private static Map<String, AttributeValue> createCorruptedCompressedItem() {
        return Map.of(
            CONTAINED_DATA_FIELD_NAME, new AttributeValue().withB(ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4})),
            "PK0", new AttributeValue().withS("Resource#customer#owner"),
            "SK0", new AttributeValue().withS("Resource#identifier")
        );
    }
}
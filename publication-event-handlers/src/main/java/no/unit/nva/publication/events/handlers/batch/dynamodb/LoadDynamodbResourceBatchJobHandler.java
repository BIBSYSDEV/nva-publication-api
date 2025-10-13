package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.lang.Integer.parseInt;
import static no.unit.nva.publication.events.handlers.ConfigurationForPushingDirectlyToEventBridge.EVENT_BUS_NAME;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.defaultEventBridgeClient;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.model.ScanResultWrapper;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

public class LoadDynamodbResourceBatchJobHandler extends EventHandler<LoadDynamodbRequest, LoadDynamodbResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LoadDynamodbResourceBatchJobHandler.class);
    private static final String WORK_QUEUE_URL_ENV = "WORK_QUEUE_URL";
    private static final String PROCESSING_ENABLED_ENV = "PROCESSING_ENABLED";
    private static final String SCAN_PAGE_SIZE_ENV = "SCAN_PAGE_SIZE";
    private static final String SQS_BATCH_SIZE_ENV = "SQS_BATCH_SIZE";
    private static final String TOTAL_SEGMENTS_ENV = "TOTAL_SEGMENTS";
    public static final String DETAIL_TYPE = "PublicationService.DataEntry.LoadDynamodbResourceBatchJob";

    private final SqsClient sqsClient;
    private final EventBridgeClient eventBridgeClient;
    private final String queueUrl;
    private final String processingEnabled;
    private final ResourceService resourceService;
    private final int scanPageSize;
    private final int sqsBatchSize;
    private final int totalSegments;

    @JacocoGenerated
    public LoadDynamodbResourceBatchJobHandler() {
        this(SqsClient.create(), defaultEventBridgeClient(), new Environment().readEnv(WORK_QUEUE_URL_ENV),
             new Environment().readEnv(PROCESSING_ENABLED_ENV), ResourceService.defaultService(),
             parseInt(new Environment().readEnv(SCAN_PAGE_SIZE_ENV)),
             parseInt(new Environment().readEnv(SQS_BATCH_SIZE_ENV)),
             parseInt(new Environment().readEnv(TOTAL_SEGMENTS_ENV)));
    }

    public LoadDynamodbResourceBatchJobHandler(SqsClient sqsClient,
                                               EventBridgeClient eventBridgeClient, String queueUrl,
                                               String processingEnabled, ResourceService resourceService,
                                               int scanPageSize,
                                               int sqsBatchSize,
                                               int totalSegments) {
        super(LoadDynamodbRequest.class);
        this.resourceService = resourceService;
        this.sqsClient = sqsClient;
        this.eventBridgeClient = eventBridgeClient;
        this.queueUrl = queueUrl;
        this.processingEnabled = processingEnabled;
        this.scanPageSize = scanPageSize;
        this.sqsBatchSize = sqsBatchSize;
        this.totalSegments = totalSegments;
    }

    @Override
    protected LoadDynamodbResponse processInput(LoadDynamodbRequest input,
                                                AwsEventBridgeEvent<LoadDynamodbRequest> event, Context context) {
        validateInput(input);

        if (!isProcessingEnabled()) {
            logger.warn("Processing is disabled via {} environment variable. Stopping execution.",
                        PROCESSING_ENABLED_ENV);
            return new LoadDynamodbResponse(0, 0, input.jobType());
        }

        if (!input.isSegmentedScan()) {
            return initiateParallelScan(input, context);
        }

        return processSegment(input, context);
    }

    private LoadDynamodbResponse initiateParallelScan(LoadDynamodbRequest input, Context context) {
        logger.info("Initiating parallel scan with {} segments for job type: {}", totalSegments,
                    input.jobType());

        for (int segment = 0; segment < totalSegments; segment++) {
            var segmentRequest = new LoadDynamodbRequest(
                input.jobType(),
                null,
                input.types(),
                segment,
                totalSegments
            );

            var segmentEvent = segmentRequest.createNewEventEntry(EVENT_BUS_NAME, DETAIL_TYPE,
                                                                  context.getInvokedFunctionArn());
            var putEventsRequest = PutEventsRequest.builder().entries(segmentEvent).build();
            eventBridgeClient.putEvents(putEventsRequest);

            logger.info("Created segment {} of {} for job type: {}", segment, totalSegments, input.jobType());
        }

        logger.info("Initiated {} parallel segment scans for job type: {}", totalSegments, input.jobType());

        return new LoadDynamodbResponse(0, 0, input.jobType());
    }

    private LoadDynamodbResponse processSegment(LoadDynamodbRequest input, Context context) {
        logger.info("Processing segment {} of {} for job type: {}, starting from marker: {}",
                    input.segment(), input.totalSegments(), input.jobType(), input.startMarker());

        var scan = resourceService.scanResourcesRaw(scanPageSize, input.startMarker(), input.types(),
                                                    input.segment(), input.totalSegments());

        var workItems = createWorkItems(input, scan);

        var messagesQueued = queueWorkItems(workItems);

        logger.info("Segment {} processed {} items, queued {} messages", input.segment(), workItems.size(),
                    messagesQueued);

        if (scan.isTruncated()) {
            sendEventForNextBatch(input, scan.nextKey(), context);
        } else {
            logger.info("Completed segment {} of {} for job type: {}", input.segment(), input.totalSegments(),
                        input.jobType());
        }

        return new LoadDynamodbResponse(workItems.size(), messagesQueued, input.jobType());
    }

    private int queueWorkItems(List<BatchWorkItem> workItems) {
        var batches = Lists.partition(workItems, sqsBatchSize);

        return batches.stream().mapToInt(this::sendBatchToQueue).sum();
    }

    private static List<BatchWorkItem> createWorkItems(LoadDynamodbRequest input, ScanResultWrapper scan) {
        return scan.items().stream().map(item -> {
            var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(item.get(PRIMARY_KEY_PARTITION_KEY_NAME).getS(),
                                                                   item.get(PRIMARY_KEY_SORT_KEY_NAME).getS());
            return new BatchWorkItem(dynamoDbKey, input.jobType());
        }).toList();
    }

    private void validateInput(LoadDynamodbRequest input) {
        Optional.ofNullable(input)
            .map(LoadDynamodbRequest::jobType)
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new IllegalArgumentException(
                "Missing required field 'jobType'. This lambda requires a jobType to be specified in the input. "
                + "Example input: {\"jobType\": \"REINDEX_RECORD\"}"));
    }

    private boolean isProcessingEnabled() {
        return Optional.ofNullable(processingEnabled)
                   .map(Boolean::parseBoolean)
                   .orElse(true);
    }

    private int sendBatchToQueue(List<BatchWorkItem> workItems) {
        if (workItems.isEmpty()) {
            return 0;
        }

        try {
            var entries = workItems.stream().map(this::getSendMessageBatchRequestEntry).toList();

            var batchRequest = SendMessageBatchRequest.builder().queueUrl(queueUrl).entries(entries).build();

            var response = sqsClient.sendMessageBatch(batchRequest);

            if (!response.failed().isEmpty()) {
                logger.error("Failed to send {} messages to SQS", response.failed().size());
                response.failed()
                    .forEach(failure -> logger.error("Failed message ID: {}, Code: {}, Message: {}", failure.id(),
                                                     failure.code(), failure.message()));
            }

            return response.successful().size();
        } catch (Exception e) {
            logger.error("Failed to send batch of {} items to queue", workItems.size(), e);
            return 0;
        }
    }

    private SendMessageBatchRequestEntry getSendMessageBatchRequestEntry(BatchWorkItem workItem) {
        try {
            var messageBody = JsonUtils.dtoObjectMapper.writeValueAsString(workItem);
            return SendMessageBatchRequestEntry.builder()
                       .id(UUID.randomUUID().toString())
                       .messageBody(messageBody)
                       .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize work item", e);
        }
    }

    private void sendEventForNextBatch(LoadDynamodbRequest input, Map<String, AttributeValue> lastEvaluatedKey,
                                       Context context) {
        var nextRequest = input.withStartMarker(lastEvaluatedKey);

        var nextEvent = nextRequest.createNewEventEntry(EVENT_BUS_NAME, DETAIL_TYPE, context.getInvokedFunctionArn());

        var putEventsRequest = PutEventsRequest.builder().entries(nextEvent).build();

        eventBridgeClient.putEvents(putEventsRequest);
        logger.info("Sent event for next batch processing");
    }
}
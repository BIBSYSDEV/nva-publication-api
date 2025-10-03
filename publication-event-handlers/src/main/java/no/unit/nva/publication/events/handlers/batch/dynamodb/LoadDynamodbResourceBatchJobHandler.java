package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static no.unit.nva.publication.events.handlers.ConfigurationForPushingDirectlyToEventBridge.EVENT_BUS_NAME;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.defaultEventBridgeClient;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

public class LoadDynamodbResourceBatchJobHandler extends EventHandler<LoadDynamodbRequest, LoadDynamodbResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LoadDynamodbResourceBatchJobHandler.class);
    private static final String TABLE_NAME_ENV = "TABLE_NAME";
    private static final String WORK_QUEUE_URL_ENV = "WORK_QUEUE_URL";
    private static final String PROCESSING_ENABLED_ENV = "PROCESSING_ENABLED";
    private static final int SCAN_PAGE_SIZE = 1000;
    private static final int SQS_BATCH_SIZE = 10;
    public static final String DETAIL_TYPE = "PublicationService.DataEntry.LoadDynamodbResourceBatchJob";

    private final AmazonDynamoDB dynamoDbClient;
    private final SqsClient sqsClient;
    private final EventBridgeClient eventBridgeClient;
    private final String tableName;
    private final String queueUrl;
    private final String processingEnabled;

    @JacocoGenerated
    public LoadDynamodbResourceBatchJobHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient(),
             SqsClient.create(),
             defaultEventBridgeClient(),
             new Environment().readEnv(TABLE_NAME_ENV),
             new Environment().readEnv(WORK_QUEUE_URL_ENV),
             new Environment().readEnv(PROCESSING_ENABLED_ENV));
    }

    public LoadDynamodbResourceBatchJobHandler(AmazonDynamoDB dynamoDbClient,
                                               SqsClient sqsClient,
                                               EventBridgeClient eventBridgeClient,
                                               String tableName,
                                               String queueUrl,
                                               String processingEnabled) {
        super(LoadDynamodbRequest.class);
        this.dynamoDbClient = dynamoDbClient;
        this.sqsClient = sqsClient;
        this.eventBridgeClient = eventBridgeClient;
        this.tableName = tableName;
        this.queueUrl = queueUrl;
        this.processingEnabled = processingEnabled;
    }

    @Override
    protected LoadDynamodbResponse processInput(LoadDynamodbRequest input,
                                                AwsEventBridgeEvent<LoadDynamodbRequest> event,
                                                Context context) {
        validateInput(input);

        if (!isProcessingEnabled()) {
            logger.warn("Processing is disabled via {} environment variable. Stopping execution.", 
                       PROCESSING_ENABLED_ENV);
            return new LoadDynamodbResponse(0, 0, input.getJobType());
        }

        logger.info("Processing batch with job type: {}, starting from marker: {}",
                    input.getJobType(), input.getStartMarker());

        var scanResult = dynamoDbClient.scan(getScanRequest(input.getStartMarker()));

        var workItems = scanResult.getItems().stream()
            .map(item -> {
                var dynamoDbKey = new DynamodbResourceBatchDynamoDbKey(
                    item.get(PRIMARY_KEY_PARTITION_KEY_NAME).getS(),
                    item.get(PRIMARY_KEY_SORT_KEY_NAME).getS());
                return new BatchWorkItem(dynamoDbKey, input.getJobType());
            }).toList();

        var messagesQueued = Lists.partition(workItems, SQS_BATCH_SIZE)
            .stream()
            .mapToInt(this::sendBatchToQueue)
            .sum();

        logger.info("Processed {} items, queued {} messages", workItems.size(), messagesQueued);

        var lastEvaluatedKey = scanResult.getLastEvaluatedKey();
        if (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
            sendEventForNextBatch(input, lastEvaluatedKey, context);
        } else {
            logger.info("Completed loading entire table for job type: {}", input.getJobType());
        }

        return new LoadDynamodbResponse(workItems.size(), messagesQueued, input.getJobType());
    }

    private void validateInput(LoadDynamodbRequest input) {
        if (input == null || input.getJobType() == null || input.getJobType().isBlank()) {
            throw new IllegalArgumentException(
                "Missing required field 'jobType'. This lambda requires a jobType to be specified in the input. " +
                "Example input: {\"jobType\": \"REINDEX_RECORD\"}"
            );
        }
    }

    private boolean isProcessingEnabled() {
        var enabled = Optional.of(processingEnabled).orElse("true");
        return "true".equalsIgnoreCase(enabled);
    }

    private ScanRequest getScanRequest(Map<String, AttributeValue> lastEvaluatedKey) {
        var scanRequest = new ScanRequest()
                                      .withTableName(tableName)
                                      .withLimit(SCAN_PAGE_SIZE)
                                      .withAttributesToGet(PRIMARY_KEY_PARTITION_KEY_NAME, PRIMARY_KEY_SORT_KEY_NAME);

        if (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty()) {
            scanRequest.withExclusiveStartKey(lastEvaluatedKey);
        }

        return scanRequest;
    }

    private int sendBatchToQueue(List<BatchWorkItem> workItems) {
        if (workItems.isEmpty()) {
            return 0;
        }

        try {
            var entries = workItems.stream()
                .map(this::getSendMessageBatchRequestEntry)
                .toList();

            var batchRequest = SendMessageBatchRequest.builder()
                                                       .queueUrl(queueUrl)
                                                       .entries(entries)
                                                       .build();

            var response = sqsClient.sendMessageBatch(batchRequest);

            if (!response.failed().isEmpty()) {
                logger.error("Failed to send {} messages to SQS", response.failed().size());
                response.failed().forEach(failure ->
                                              logger.error("Failed message ID: {}, Code: {}, Message: {}",
                                                           failure.id(), failure.code(), failure.message()));
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

    private void sendEventForNextBatch(LoadDynamodbRequest input,
                                       Map<String, AttributeValue> lastEvaluatedKey,
                                       Context context) {
        var nextRequest = input.withStartMarker(lastEvaluatedKey);

        var nextEvent = nextRequest.createNewEventEntry(
            EVENT_BUS_NAME,
            DETAIL_TYPE,
            context.getInvokedFunctionArn()
        );

        var putEventsRequest = PutEventsRequest.builder()
                                                .entries(nextEvent)
                                                .build();

        eventBridgeClient.putEvents(putEventsRequest);
        logger.info("Sent event for next batch processing");
    }
}
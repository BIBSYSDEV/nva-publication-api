package no.unit.nva.publication.events.handlers.dynamodbstream;

import static no.unit.nva.publication.events.handlers.ConfigurationForPushingDirectlyToEventBridge.EVENT_BUS_NAME;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.defaultEventBridgeClient;
import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toEntity;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.expandresources.RecoveryEntry;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.ResourceQueueClient;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Listens on DynamodbEvents from DynamoDB Stream trigger and forwards the DynamoDbStreamRecords to EventBridge.
 *
 * <p>Notice a DynamoDB stream can only have two streams attached before it can lead into throttling and performance
 * issues with DynamodDB, this is why we have this handler to publish it to EventBridge.
 */
public class DynamodbStreamToEventBridgeHandler implements RequestHandler<DynamodbEvent, EventReference> {

    public static final String PROCEEDING_EVENT_MESSAGE = "Proceeding event for identifier: {}";
    public static final String DETAIL_TYPE_NOT_IMPORTANT = "See event topic";
    public static final String EMITTED_EVENT_MESSAGE = "Emitted Event:{}";
    public static final String SENT_TO_RECOVERY_QUEUE_MESSAGE = "DateEntry has been sent to recovery queue: {}";
    public static final EventReference DO_NOT_EMIT_EVENT = null;
    public static final String DYNAMO_DB_STREAM_SOURCE = "DynamoDbStream";
    public static final String RECOVERY_QUEUE = new Environment().readEnv("RECOVERY_QUEUE");
    private static final Logger logger = LoggerFactory.getLogger(DynamodbStreamToEventBridgeHandler.class);
    private final S3Driver s3Driver;
    private final QueueClient sqsClient;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public DynamodbStreamToEventBridgeHandler() {
        this(S3Driver.defaultS3Client().build(), defaultEventBridgeClient(),
             ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE));
    }

    protected DynamodbStreamToEventBridgeHandler(S3Client s3Client, EventBridgeClient eventBridgeClient,
                                                 QueueClient sqsClient) {
        this.s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        this.eventBridgeClient = eventBridgeClient;
        this.sqsClient = sqsClient;
    }

    @Override
    public EventReference handleRequest(DynamodbEvent inputEvent, Context context) {
        var dynamodbStreamRecord = inputEvent.getRecords().getFirst();
        var dataEntryUpdateEvent = convertToDataEntryUpdateEvent(dynamodbStreamRecord);
        return dataEntryUpdateEvent.notEmpty() ? sendEvent(dataEntryUpdateEvent, context) : DO_NOT_EMIT_EVENT;
    }

    private static SortableIdentifier getIdentifier(DataEntryUpdateEvent blobObject) {
        return Optional.ofNullable(blobObject.getOldData())
                   .map(Entity::getIdentifier)
                   .orElseGet(() -> blobObject.getNewData().getIdentifier());
    }

    private static String toEvenBridgeDetail(EventReference eventReference) throws JsonProcessingException {
        var detail = AwsEventBridgeDetail.newBuilder().withResponsePayload(eventReference).build();
        return JsonUtils.dtoObjectMapper.writeValueAsString(detail);
    }

    private EventReference processRecoveryMessage(Failure<EventReference> failure, DataEntryUpdateEvent event) {
        var identifier = getIdentifier(event);
        RecoveryEntry.fromDataEntryUpdateEvent(event)
            .withIdentifier(identifier)
            .withException(failure.getException())
            .persist(sqsClient);
        logger.error(SENT_TO_RECOVERY_QUEUE_MESSAGE, identifier);
        return null;
    }

    private EventReference sendEvent(DataEntryUpdateEvent blob, Context context) {
        logger.info(PROCEEDING_EVENT_MESSAGE, getIdentifier(blob));
        return attempt(() -> saveBlobToS3(blob)).map(blobUri -> new EventReference(blob.getTopic(), blobUri))
                   .map(eventReference -> sendEvent(eventReference, context))
                   .orElse(failure -> processRecoveryMessage(failure, blob));
    }

    private EventReference sendEvent(EventReference eventReference, Context context) throws JsonProcessingException {
        var eventRequest = createPutEventRequest(context, eventReference);
        eventBridgeClient.putEvents(eventRequest);
        logger.info(EMITTED_EVENT_MESSAGE, eventReference.toJsonString());
        return eventReference;
    }

    private PutEventsRequest createPutEventRequest(Context context, EventReference eventReference)
        throws JsonProcessingException {
        var entry = PutEventsRequestEntry.builder()
                        .eventBusName(EVENT_BUS_NAME)
                        .time(Instant.now())
                        .source(DYNAMO_DB_STREAM_SOURCE)
                        .detailType(DETAIL_TYPE_NOT_IMPORTANT)
                        .resources(context.getInvokedFunctionArn())
                        .detail(toEvenBridgeDetail(eventReference))
                        .build();
        return PutEventsRequest.builder().entries(entry).build();
    }

    private URI saveBlobToS3(DataEntryUpdateEvent blob) throws IOException {
        return s3Driver.insertFile(UnixPath.of(UUID.randomUUID().toString()), blob.toJsonString());
    }

    private DataEntryUpdateEvent convertToDataEntryUpdateEvent(DynamodbStreamRecord dynamoDbRecord) {
        return new DataEntryUpdateEvent(dynamoDbRecord.getEventName(),
                                        getEntity(dynamoDbRecord.getDynamodb().getOldImage()),
                                        getEntity(dynamoDbRecord.getDynamodb().getNewImage()));
    }

    private Entity getEntity(Map<String, AttributeValue> image) {
        return attempt(() -> toEntity(image)).toOptional().flatMap(Function.identity()).orElse(null);
    }
}

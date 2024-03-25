package no.unit.nva.publication.events.handlers.fanout;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toEntity;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
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
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class DataEntryUpdateHandler extends EventHandler<EventReference, EventReference> {

    public static final String SENT_TO_RECOVERY_QUEUE_MESSAGE = "DateEntry has been sent to recovery queue: {}";
    public static final String RECOVERY_QUEUE = new Environment().readEnv("RECOVERY_QUEUE");
    public static final Entity NO_VALUE = null;
    public static final EventReference DO_NOT_EMIT_EVENT = null;
    private static final Logger logger = LoggerFactory.getLogger(DataEntryUpdateHandler.class);
    private final QueueClient sqsClient;
    private final S3Driver s3Driver;
    
    @JacocoGenerated
    public DataEntryUpdateHandler() {
        this(S3Driver.defaultS3Client().build(), ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE));
    }
    
    public DataEntryUpdateHandler(S3Client s3Client, QueueClient sqsClient) {
        super(EventReference.class);
        this.s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        this.sqsClient = sqsClient;
    }
    
    @Override
    protected EventReference processInput(
        EventReference input,
        AwsEventBridgeEvent<EventReference> event,
        Context context) {
        
        var s3Content = readBlobFromS3(input);
        var dynamoDbRecord = parseDynamoDbRecord(s3Content);
        var blob = convertToDataEntryUpdateEvent(dynamoDbRecord);
        return blob.notEmpty() ? proceedBlob(blob) : DO_NOT_EMIT_EVENT;
    }

    private EventReference proceedBlob(DataEntryUpdateEvent blob) {
        return attempt(() -> saveBlobToS3(blob))
                   .map(blobUri -> new EventReference(blob.getTopic(), blobUri))
                   .map(this::logEvent)
                   .orElse(failure -> processRecoveryMessage(failure, blob));
    }

    private URI saveBlobToS3(DataEntryUpdateEvent blob) {
        throw new RuntimeException();
//        return s3Driver.insertFile(UnixPath.of(UUID.randomUUID().toString()), blob.toJsonString());
    }

    private EventReference processRecoveryMessage(Failure<EventReference> failure, DataEntryUpdateEvent event) {
        var identifier = getIdentifier(event);
        RecoveryEntry.fromIdentifier(identifier)
            .resourceType(getType(event))
            .withException(failure.getException())
            .persist(sqsClient);
        logger.error(SENT_TO_RECOVERY_QUEUE_MESSAGE, identifier);
        return null;
    }

    private static String getType(DataEntryUpdateEvent blobObject) {
        return Optional.ofNullable(blobObject.getOldData())
                   .map(Entity::getType)
                   .orElseGet(() -> blobObject.getNewData().getType());
    }

    private static SortableIdentifier getIdentifier(DataEntryUpdateEvent blobObject) {
        return Optional.ofNullable(blobObject.getOldData())
                   .map(Entity::getIdentifier)
                   .orElseGet(() -> blobObject.getNewData().getIdentifier());
    }

    private EventReference logEvent(EventReference event) {
        logger.debug("Emitted Event:{}", event.toJsonString());
        return event;
    }
    
    private DataEntryUpdateEvent convertToDataEntryUpdateEvent(DynamodbStreamRecord dynamoDbRecord) {
        return new DataEntryUpdateEvent(
            dynamoDbRecord.getEventName(),
            getEntity(dynamoDbRecord.getDynamodb().getOldImage()),
            getEntity(dynamoDbRecord.getDynamodb().getNewImage())
        );
    }
    
    private String readBlobFromS3(EventReference input) {
        var filePath = UriWrapper.fromUri(input.getUri()).toS3bucketPath();
        return s3Driver.getFile(filePath);
    }
    
    private DynamodbStreamRecord parseDynamoDbRecord(String s3Content) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(s3Content, DynamodbStreamRecord.class)).orElseThrow();
    }
    
    private Entity getEntity(Map<String, AttributeValue> image) {
        return attempt(() -> toEntity(image))
                   .toOptional(this::logFailureInDebugging)
                   .flatMap(Function.identity()).orElse(NO_VALUE);
    }
    
    private void logFailureInDebugging(Failure<Optional<Entity>> fail) {
        logger.debug(ExceptionUtils.stackTraceInSingleLine(fail.getException()));
    }
}

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
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

public class ImportCandidateDynamoDbStreamToEventBridgeHandler
    implements RequestHandler<DynamodbEvent, EventReference> {

    private static final String PROCESSING_EVENT_MESSAGE = "Processing event for identifier: {}";
    private static final String DETAIL_TYPE_NOT_IMPORTANT = "See event topic";
    private static final String EMITTED_EVENT_MESSAGE = "Emitted Event:{}";
    private static final String DYNAMO_DB_STREAM_SOURCE = "DynamoDbStream";
    private static final Logger logger = LoggerFactory.getLogger(
        ImportCandidateDynamoDbStreamToEventBridgeHandler.class);
    private static final EventReference EMPTY_EVENT = null;
    private final S3Driver s3Driver;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public ImportCandidateDynamoDbStreamToEventBridgeHandler() {
        this(S3Driver.defaultS3Client().build(), defaultEventBridgeClient());
    }

    protected ImportCandidateDynamoDbStreamToEventBridgeHandler(S3Client s3Client,
                                                                EventBridgeClient eventBridgeClient) {
        this.s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    public EventReference handleRequest(DynamodbEvent inputEvent, Context context) {
        var dynamodbStreamRecord = inputEvent.getRecords().getFirst();
        var dataEntryUpdateEvent = convertToUpdateEvent(dynamodbStreamRecord);
        return dataEntryUpdateEvent.isResource()
                   ? sendEvent(dataEntryUpdateEvent, context)
                   : EMPTY_EVENT;
    }

    private static SortableIdentifier getImportCandidateIdentifier(ImportCandidateDataEntryUpdate blobObject) {
        return blobObject.getOldData()
                   .map(Resource.class::cast)
                   .map(Resource::getIdentifier)
                   .orElseGet(() -> getIdentifierFromNewData(blobObject));
    }

    private static SortableIdentifier getIdentifierFromNewData(ImportCandidateDataEntryUpdate blobObject) {
        return blobObject.getNewData()
                   .map(Resource.class::cast)
                   .map(Resource::getIdentifier)
                   .orElseThrow();
    }

    private static String toEvenBridgeDetail(EventReference eventReference) {
        var detail = AwsEventBridgeDetail.newBuilder().withResponsePayload(eventReference).build();
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(detail))
                   .orElseThrow();
    }

    private EventReference sendEvent(ImportCandidateDataEntryUpdate blob, Context context) {
        logger.info(PROCESSING_EVENT_MESSAGE, getImportCandidateIdentifier(blob));
        var uri = saveBlobToS3(blob);
        var eventReference = new EventReference(blob.getTopic(), uri);
        sendEvent(eventReference, context);
        return eventReference;
    }

    private void sendEvent(EventReference eventReference, Context context) {
        var eventRequest = createPutEventRequest(context, eventReference);
        eventBridgeClient.putEvents(eventRequest);
        logger.info(EMITTED_EVENT_MESSAGE, eventReference.toJsonString());
    }

    private PutEventsRequest createPutEventRequest(Context context, EventReference eventReference) {
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

    private URI saveBlobToS3(ImportCandidateDataEntryUpdate blob) {
        return attempt(() -> s3Driver.insertFile(UnixPath.of(UUID.randomUUID().toString()), blob.toJsonString()))
                   .orElseThrow(failure -> new RuntimeException("Failed to save blob to S3: " + blob.toJsonString()));
    }

    private ImportCandidateDataEntryUpdate convertToUpdateEvent(DynamodbStreamRecord dynamoDbRecord) {
        return new ImportCandidateDataEntryUpdate(dynamoDbRecord.getEventName(),
                                                  getImportCandidate(dynamoDbRecord.getDynamodb().getOldImage()),
                                                  getImportCandidate(dynamoDbRecord.getDynamodb().getNewImage()));
    }

    private Entity getImportCandidate(Map<String, AttributeValue> image) {
        return attempt(() -> toEntity(image)).toOptional(this::logFailureInDebugging)
                   .flatMap(Function.identity())
                   .orElse(null);
    }

    private void logFailureInDebugging(Failure<Optional<Entity>> fail) {
        logger.debug(ExceptionUtils.stackTraceInSingleLine(fail.getException()));
    }
}

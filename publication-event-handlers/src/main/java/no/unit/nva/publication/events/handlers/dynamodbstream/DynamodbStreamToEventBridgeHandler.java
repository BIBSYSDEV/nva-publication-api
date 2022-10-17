package no.unit.nva.publication.events.handlers.dynamodbstream;

import static no.unit.nva.publication.events.handlers.ConfigurationForPushingDirectlyToEventBridge.EVENT_BUS_NAME;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.defaultEventBridgeClient;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UnixPath;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Listens on DynamodbEvents from DynamoDB Stream trigger and forwards the DynamoDbStreamRecords to EventBridge.
 *
 * <p>Notice a DynamoDB stream can only have two streams attached before it can lead into throttling and performance
 * issues with DynamodDB, this is why we have this handler to publish it to EventBridge.
 */
public class DynamodbStreamToEventBridgeHandler implements RequestHandler<DynamodbEvent, Set<PutEventsResponse>> {
    
    public static final String DYNAMODB_UPDATE_EVENT_TOPIC = "PublicationService.Database.Update";
    public static final String DETAIL_TYPE_NOT_IMPORTANT = "See event topic";
    public static final String DYNAMO_DB_STREAM_SOURCE = "DynamoDbStream";
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;
    
    @JacocoGenerated
    public DynamodbStreamToEventBridgeHandler() {
        this(defaultS3Client(), defaultEventBridgeClient());
    }
    
    protected DynamodbStreamToEventBridgeHandler(S3Client s3Client, EventBridgeClient eventBridgeClient) {
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }
    
    @Override
    public Set<PutEventsResponse> handleRequest(DynamodbEvent inputEvent, Context context) {
        return inputEvent.getRecords()
                   .stream()
                   .map(attempt(JsonUtils.dtoObjectMapper::writeValueAsString))
                   .map(attempt -> attempt.map(this::storeFileInS3Bucket))
                   .map(attempt -> attempt.map(this::createEvent))
                   .map(attempt -> attempt.map(eventReference -> sendEvent(eventReference, context)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toSet());
    }
    
    @JacocoGenerated
    private static S3Client defaultS3Client() {
        return S3Driver.defaultS3Client().build();
    }
    
    private PutEventsResponse sendEvent(EventReference eventReference, Context context) {
        var eventRequest = createPutEventRequest(context, eventReference);
        return eventBridgeClient.putEvents(eventRequest);
    }
    
    private URI storeFileInS3Bucket(String json) throws IOException {
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        return s3Driver.insertFile(UnixPath.of(UUID.randomUUID().toString()), json);
    }
    
    private EventReference createEvent(URI uri) {
        return new EventReference(DYNAMODB_UPDATE_EVENT_TOPIC, uri);
    }
    
    private PutEventsRequest createPutEventRequest(Context context, EventReference eventReference) {
        var entry = PutEventsRequestEntry.builder()
                        .eventBusName(EVENT_BUS_NAME)
                        .time(Instant.now())
                        .source(DYNAMO_DB_STREAM_SOURCE)
                        .detailType(DETAIL_TYPE_NOT_IMPORTANT)
                        .resources(context.getInvokedFunctionArn())
                        .detail(eventReference.toJsonString())
                        .build();
        return PutEventsRequest.builder()
                   .entries(entry)
                   .build();
    }
}

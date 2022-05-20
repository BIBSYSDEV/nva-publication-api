package no.unit.nva.publication.events.handlers.dynamodbstream;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENT_BUS_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UnixPath;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Listens on DynamodbEvents from DynamoDB Stream trigger and forwards the DynamoDbStreamRecords to EventBridge.
 *
 * <p>Notice a DynamoDB stream can only have two streams attached before we it can lead into throttling and performance
 * issues with DynamodDB, this is why we have this handler to publish it to EventBridge.
 */
public class DynamodbStreamToEventBridgeHandler implements RequestHandler<DynamodbEvent, EventReference> {

    public static final String AWS_REGION = "AWS_REGION";
    public static final String DYNAMODB_UPDATE_EVENT_TOPIC = "PublicationService.Database.Update";
    public static final String DYNAMO_EVENTS_FOLDER_IN_BUCKET = "dynamoEvents";
    public static final String DETAIL_TYPE_NOT_IMPORTANT = "See event topic";
    public static final String DYNAMO_DB_STREAM_SOURCE = "DynamoDbStream";
    private final EventPublisher eventPublisher;
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public DynamodbStreamToEventBridgeHandler() {
        this(defaultS3Client(), null, defaultEventBridgePublisher());
    }

    protected DynamodbStreamToEventBridgeHandler(S3Client s3Client,
                                                 EventBridgeClient eventBridgeClient,
                                                 EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    public EventReference handleRequest(DynamodbEvent inputEvent, Context context) {
        var newEvent = createNewEvent(inputEvent);
        publishNewEventDirectlyToEventBridge(context, newEvent);
        publishLegacyEvent(inputEvent);
        return publishNewEventAsLambdaDestination(newEvent);
    }

    private Try<EventReference> createNewEvent(DynamodbEvent inputEvent) {
        return storeEventInS3(inputEvent).map(this::createEvent);
    }

    private EventReference publishNewEventAsLambdaDestination(Try<EventReference> newEvent) {
        return newEvent.orElse(fail -> null);
    }

    private void publishLegacyEvent(DynamodbEvent inputEvent) {
        eventPublisher.publish(inputEvent);
    }

    private void publishNewEventDirectlyToEventBridge(Context context, Try<EventReference> newEvent) {
        newEvent.forEach(outputEvent->publishEventWithFileUri(context, outputEvent));
    }

    private void publishEventWithFileUri(Context context, EventReference eventReference) {
        var putEventRequest = createPutEventRequest(context,eventReference);
        eventBridgeClient.putEvents(putEventRequest);
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

    @JacocoGenerated
    private static S3Client defaultS3Client() {
        return S3Driver.defaultS3Client().build();
    }

    @JacocoGenerated
    private static EventPublisher defaultEventBridgePublisher() {
        return new EventBridgePublisher(
            defaultEventBridgeRetryClient(),
            defaultFailedEventPublisher(),
            EVENT_BUS_NAME,
            DYNAMODB_UPDATE_EVENT_TOPIC
        );
    }

    @JacocoGenerated
    private static EventPublisher defaultFailedEventPublisher() {
        return new SqsEventPublisher(defaultSqsClient(), DynamoDbStreamEventsConstants.getDlqUrl());
    }

    @JacocoGenerated
    private static SqsClient defaultSqsClient() {
        return SqsClient.builder()
            .region(Region.of(System.getenv(AWS_REGION)))
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();
    }

    @JacocoGenerated
    private static EventBridgeRetryClient defaultEventBridgeRetryClient() {
        return new EventBridgeRetryClient(defaultEventBridgeClient(), DynamoDbStreamEventsConstants.getMaxAttempt());
    }

    @JacocoGenerated
    private static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
            .region(Region.of(System.getenv(AWS_REGION)))
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                                       .apiCallAttemptTimeout(Duration.ofSeconds(1))
                                       .retryPolicy(RetryPolicy.builder().numRetries(10).build())
                                       .build())
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();
    }

    private Try<URI> storeEventInS3(DynamodbEvent event) {
        var filename = UnixPath.of(DYNAMO_EVENTS_FOLDER_IN_BUCKET, UUID.randomUUID().toString());
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(event))
            .map(json -> s3Driver.insertFile(filename, json));
    }
}

package no.unit.nva.publication.events.handlers.dynamodbstream;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Listens on DynamodbEvents from DynamoDB Stream trigger and forwards the DynamoDbStreamRecords to EventBridge.
 *
 * <p>Notice a DynamoDB stream can only have two streams attached before we it can lead into throttling and performance
 * issues with DynamodDB, this is why we have this handler to publish it to EventBridge.
 */
public class DynamodbStreamToEventBridgeHandler implements RequestHandler<DynamodbEvent, URI> {

    public static final String AWS_REGION = "AWS_REGION";
    public static final String DYNAMODB_UPDATE_EVENT_TOPIC = "PublicationService.Database.Update";
    public static final String EVENTS_BUCKET = new Environment().readEnv("EVENTS_BUCKET");
    public static final String DYNAMO_EVENTS_FOLDER_IN_BUCKET = "dynamoEvents";
    private final EventPublisher eventPublisher;
    private final S3Client s3Client;

    @JacocoGenerated
    public DynamodbStreamToEventBridgeHandler() {
        this(defaultS3Client(), defaultEventBridgePublisher());
    }

    protected DynamodbStreamToEventBridgeHandler(S3Client s3Client, EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.s3Client = s3Client;
    }

    @Override
    public URI handleRequest(DynamodbEvent event, Context context) {
        URI savedFile = storeEventInS3(event);
        eventPublisher.publish(event);
        return savedFile;
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
            DynamoDbStreamEventsConstants.getEventBusName(),
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

    private URI storeEventInS3(DynamodbEvent event) {
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        var json = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(event)).orElseThrow();
        var filename = UnixPath.of(DYNAMO_EVENTS_FOLDER_IN_BUCKET, UUID.randomUUID().toString());
        return attempt(() -> s3Driver.insertFile(filename, json)).orElseThrow();
    }
}

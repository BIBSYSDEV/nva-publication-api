package no.unit.nva.publication.events.handlers.dynamodbstream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.time.Duration;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Listens on DynamodbEvents from DynamoDB Stream trigger and forwards the DynamoDbStreamRecords to EventBridge.
 *
 * <p>Notice a DynamoDB stream can only have two streams attached before we it can lead into throttling and performance
 * issues with DynamodDB, this is why we have this handler to publish it to EventBridge.
 */
public class DynamodbStreamToEventBridgeHandler implements RequestHandler<DynamodbEvent, Void> {

    public static final String AWS_REGION = "AWS_REGION";
    private final EventPublisher eventPublisher;
    public static final String EVENT_DETAIL_TYPE = "PublicationService.Database.Update";


    @JacocoGenerated
    public DynamodbStreamToEventBridgeHandler() {
        this(defaultEventBridgePublisher());
    }

    protected DynamodbStreamToEventBridgeHandler(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @JacocoGenerated
    private static EventPublisher defaultEventBridgePublisher() {
        return new EventBridgePublisher(
            defaultEventBridgeRetryClient(),
            defaultFailedEventPublisher(),
            DynamoDbStreamEventsConstants.getEventBusName(),
            EVENT_DETAIL_TYPE
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

    @Override
    public Void handleRequest(DynamodbEvent event, Context context) {
        eventPublisher.publish(event);
        return null;
    }
}

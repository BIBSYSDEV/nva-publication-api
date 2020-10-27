package no.unit.nva.doi.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.time.Duration;
import no.unit.nva.doi.publisher.EventBridgePublisher;
import no.unit.nva.doi.publisher.EventBridgeRetryClient;
import no.unit.nva.doi.publisher.EventPublisher;
import no.unit.nva.doi.publisher.SqsEventPublisher;
import nva.commons.utils.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Listens on DynamodbEvents from DynamoDB Stream trigger and forwards the DynamoDbStreamRecords to EventBridge.
 */
public class FanoutHandler implements RequestHandler<DynamodbEvent, Void> {

    public static final String AWS_REGION = "AWS_REGION";
    private final EventPublisher eventPublisher;

    @JacocoGenerated
    public FanoutHandler() {
        this(defaultEventBridgePublisher());
    }

    @JacocoGenerated
    private static EventPublisher defaultEventBridgePublisher() {
        return new EventBridgePublisher(
            defaultEventBridgeRetryClient(),
            defaultFailedEventPublisher(),
            Env.getEventBusName()
        );
    }

    @JacocoGenerated
    private static EventPublisher defaultFailedEventPublisher() {
        return new SqsEventPublisher(defaultSqsClient(), Env.getDlqUrl());
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
        return new EventBridgeRetryClient(defaultEventBridgeClient(), Env.getMaxAttempt());
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

    public FanoutHandler(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Void handleRequest(DynamodbEvent event, Context context) {

        eventPublisher.publish(event);

        return null;
    }
}

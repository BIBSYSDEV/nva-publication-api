package no.unit.nva.doi.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.time.Duration;
import no.unit.nva.doi.publisher.EventBridgePublisher;
import no.unit.nva.doi.publisher.EventBridgeRetryClient;
import no.unit.nva.doi.publisher.EventPublisher;
import no.unit.nva.doi.publisher.SqsEventPublisher;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class DynamodbEventFanoutStreamRecordsEventBridgeHandler implements RequestHandler<DynamodbEvent, Void> {

    public static final String AWS_REGION = "AWS_REGION";
    private final EventPublisher eventPublisher;
    private static final Logger logger  = LoggerFactory
        .getLogger(DynamodbEventFanoutStreamRecordsEventBridgeHandler.class);

    @JacocoGenerated
    public DynamodbEventFanoutStreamRecordsEventBridgeHandler() {
        this(defaultEventBridgePublisher());
    }

    /**
     * Constructor for CreatePublicationHandler.
     */
    protected DynamodbEventFanoutStreamRecordsEventBridgeHandler(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @JacocoGenerated
    private static EventPublisher defaultEventBridgePublisher() {
        return new EventBridgePublisher(
            defaultEventBridgeRetryClient(),
            defaultFailedEventPublisher(),
            AppEnv.getEventBusName()
        );
    }

    @JacocoGenerated
    private static EventPublisher defaultFailedEventPublisher() {
        return new SqsEventPublisher(defaultSqsClient(), AppEnv.getDlqUrl());
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
        return new EventBridgeRetryClient(defaultEventBridgeClient(), AppEnv.getMaxAttempt());
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
        String json = attempt(()->JsonUtils.objectMapper.writeValueAsString(event)).orElseThrow();
        logger.info("event:"+json);
        eventPublisher.publish(event);
        return null;
    }
}

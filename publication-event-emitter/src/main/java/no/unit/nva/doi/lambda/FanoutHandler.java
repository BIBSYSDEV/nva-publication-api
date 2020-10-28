package no.unit.nva.doi.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.doi.publisher.EventBridgePublisher;
import no.unit.nva.doi.publisher.EventBridgeRetryClient;
import no.unit.nva.doi.publisher.EventPublisher;
import no.unit.nva.doi.publisher.SqsEventPublisher;
import no.unit.nva.publication.doi.PublicationMapper;
import no.unit.nva.publication.doi.dto.Publication;
import no.unit.nva.publication.doi.dto.PublicationMapping;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Listens on DynamodbEvents from DynamoDB Stream trigger and forwards the DynamoDbStreamRecords to EventBridge.
 */
public class FanoutHandler implements RequestHandler<DynamodbEvent, Void> {

    public static final String AWS_REGION = "AWS_REGION";
    private final EventPublisher eventPublisher;

    private final PublicationMapper publicationMapper;

    @JacocoGenerated
    public FanoutHandler() {
        this(defaultEventBridgePublisher(), defaultPublicationMapper());
    }

    private static PublicationMapper defaultPublicationMapper() {
        return new PublicationMapper("http://example.ns");
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

    public FanoutHandler(EventPublisher eventPublisher, PublicationMapper publicationMapper) {
        this.eventPublisher = eventPublisher;
        this.publicationMapper = publicationMapper;
    }

    @Override
    public Void handleRequest(DynamodbEvent event, Context context) {

        eventPublisher.publish(fromDynamodbEvent(event));

        return null;
    }


    private List<String> fromDynamodbEvent(DynamodbEvent event) {
        return event.getRecords()
            .stream()
            .map(this::fromDynamodbStreamRecord)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(this::toString)
            .collect(Collectors.toList());
    }

    private String toString(Publication publication) {
        try {
            return JsonUtils.objectMapper.writeValueAsString(publication);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Optional<Publication> fromDynamodbStreamRecord(DynamodbStreamRecord record) {
        PublicationMapping publicationMapping = publicationMapper.fromDynamodbStreamRecord(record);

        if (isEffectiveChange(publicationMapping)) {
            Publication publication = publicationMapping.getNewPublication().get();
            return Optional.of(publication);
        } else {
            return Optional.empty();
        }
    }

    private boolean isEffectiveChange(PublicationMapping publicationMapping) {
        Optional<Publication> newPublication = publicationMapping.getNewPublication();
        Optional<Publication> oldPublication = publicationMapping.getOldPublication();

        boolean isEffectiveChange = false;
        if (newPublication.isPresent()) {
            if (oldPublication.isPresent()) {
                isEffectiveChange = !newPublication.get().equals(oldPublication.get());
            } else {
                isEffectiveChange = true;
            }
        }
        return isEffectiveChange;
    }
}

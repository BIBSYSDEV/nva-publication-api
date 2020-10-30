package no.unit.nva.doi.publisher;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Publishes a event on a event bus, easily consumable for event bridge consumers.
 */
public class EventBridgePublisher implements EventPublisher {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final String EVENT_SOURCE = "aws-dynamodb-stream-eventbridge-fanout";
    public static final String EVENT_DETAIL_TYPE = "dynamodb-stream-event";

    private final EventBridgeRetryClient eventBridge;
    private final EventPublisher failedEventPublisher;
    private final String eventBusName;
    private final Clock clock;

    private static final Logger logger = LoggerFactory.getLogger(EventBridgePublisher.class);

    @JacocoGenerated
    public EventBridgePublisher(EventBridgeRetryClient eventBridge,
                                EventPublisher failedEventPublisher,
                                String eventBusName) {
        this(eventBridge, failedEventPublisher, eventBusName, Clock.systemUTC());
    }

    /**
     * Constructor for EventBridgePublisher.
     *
     * @param eventBridge          eventBridge
     * @param failedEventPublisher failedEventPublisher
     * @param eventBusName         eventBusName
     * @param clock                clock
     */
    public EventBridgePublisher(EventBridgeRetryClient eventBridge,
                                EventPublisher failedEventPublisher, String eventBusName, Clock clock) {
        this.eventBridge = eventBridge;
        this.failedEventPublisher = failedEventPublisher;
        this.eventBusName = eventBusName;
        this.clock = clock;
    }

    @Override
    public void publish(final DynamodbEvent event) {
        List<PutEventsRequestEntry> requestEntries = createPutEventsRequestEntries(event);
        List<PutEventsRequestEntry> failedEntries = putEventsToEventBus(
                requestEntries);
        publishFailedEventsToDlq(failedEntries);
    }

    private void publishFailedEventsToDlq(List<PutEventsRequestEntry> failedEntries) {
        if (!failedEntries.isEmpty()) {
            logger.debug("Sending failed events {} to failed event publisher", failedEntries);
            failedEntries.forEach(this::publishFailedEvent);
        }
    }

    private List<PutEventsRequestEntry> putEventsToEventBus(List<PutEventsRequestEntry> requestEntries) {
        PutEventsRequest putEventsRequest = PutEventsRequest.builder()
                .entries(requestEntries)
                .build();
        List<PutEventsRequestEntry> failedEntries = eventBridge.putEvents(putEventsRequest);
        return failedEntries;
    }

    private List<PutEventsRequestEntry> createPutEventsRequestEntries(DynamodbEvent event) {
        return Collections.singletonList(createPutEventRequestEntry(event));
    }

    private PutEventsRequestEntry createPutEventRequestEntry(DynamodbEvent event) {
        Instant time = Instant.now(clock);
        return PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .time(time)
                .source(EVENT_SOURCE)
                .detailType(EVENT_DETAIL_TYPE)
                .detail(toString(event))
                .resources(createResourceCollection(event))
                .build();
    }

    private List<String> createResourceCollection(DynamodbEvent event) {
        return event.getRecords().stream()
                .map(DynamodbEvent.DynamodbStreamRecord::getEventSourceARN)
                .collect(Collectors.toList());
    }

    private void publishFailedEvent(PutEventsRequestEntry entry) {
        var failedEvent = parseDynamodbEvent(entry);
        failedEventPublisher.publish(failedEvent);
    }

    @JacocoGenerated
    private String toString(DynamodbEvent record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @JacocoGenerated
    private DynamodbEvent parseDynamodbEvent(PutEventsRequestEntry entry) {
        try {
            return objectMapper.readValue(entry.detail(), DynamodbEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

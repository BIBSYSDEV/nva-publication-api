package no.unit.nva.doi.publisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry.Builder;

public class EventBridgePublisherTest {

    public static final String EXPECTED_DETAIL_TEMPLATE = "{\"eventSourceARN\":\"%s\"}";
    public static final String FAILED_EVENT_NAME = "Failed";
    public static final String SUCCESS_EVENT_NAME = "Success";
    public static final String RECORD_STRING_TEMPLATE = "{\"eventName\":\"%s\",\"eventSourceARN\":\"%s\"}";
    private static final String EVENT_BUS = UUID.randomUUID().toString();
    private static final Instant NOW = Instant.now();
    private static final String EVENT_SOURCE_ARN = UUID.randomUUID().toString();
    public static final Builder PUT_EVENT_REQUEST_BUILDER = putEventRequestBuilder();

    @Mock
    private EventBridgeRetryClient eventBridge;
    @Mock
    private EventPublisher failedEventPublisher;
    private EventPublisher publisher;

    /**
     * Set up environment for test.
     */
    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        publisher = new EventBridgePublisher(eventBridge, failedEventPublisher,
            EVENT_BUS, Clock.fixed(NOW, ZoneId.systemDefault()));
    }

    @Test
    public void publishCanPutEventsToEventBridge() {
        DynamodbEvent event = createDynamodbEvent();
        prepareMocksWithSuccessfulPutEvents();

        publisher.publish(event);

        PutEventsRequest expected = createPutEventsRequest();
        verify(eventBridge).putEvents(expected);
        verifyNoMoreInteractions(failedEventPublisher);
    }

    @Test
    public void publishFailedEventWhenPutEventsToEventBridgeHasFailures() {

        prepareMocksWithFailingPutEventEntries();

        DynamodbEvent.DynamodbStreamRecord failedRecord = createDynamodbStreamRecord(FAILED_EVENT_NAME);
        DynamodbEvent event = createDynamodbEvent(failedRecord);

        publisher.publish(event);

        PutEventsRequest partiallyFailingRequest = createFailingPutEventsRequest();
        verify(eventBridge).putEvents(partiallyFailingRequest);
        DynamodbEvent failedEvent = createDynamodbEvent(failedRecord);
        verify(failedEventPublisher).publish(failedEvent);
    }

    private static Builder putEventRequestBuilder() {
        return PutEventsRequestEntry.builder()
            .eventBusName(EVENT_BUS)
            .time(NOW)
            .source(EventBridgePublisher.EVENT_SOURCE)
            .detailType(EventBridgePublisher.EVENT_DETAIL_TYPE)
            .resources(EVENT_SOURCE_ARN);
    }

    private PutEventsRequest createFailingPutEventsRequest() {
        String failedRecordString = String.format(RECORD_STRING_TEMPLATE, FAILED_EVENT_NAME, EVENT_SOURCE_ARN);
        return PutEventsRequest.builder()
            .entries(PUT_EVENT_REQUEST_BUILDER.detail(failedRecordString).build())
            .build();
    }

    private List<PutEventsRequestEntry> createFailedEntries() {
        String failedRecordString = String.format(RECORD_STRING_TEMPLATE, FAILED_EVENT_NAME,
            EventBridgePublisherTest.EVENT_SOURCE_ARN);
        return Collections.singletonList(
            PUT_EVENT_REQUEST_BUILDER
                .detail(failedRecordString)
                .build());
    }

    private DynamodbEvent.DynamodbStreamRecord createDynamodbStreamRecord(String eventName) {
        DynamodbEvent.DynamodbStreamRecord record = new DynamodbEvent.DynamodbStreamRecord();
        record.setEventSourceARN(EVENT_SOURCE_ARN);
        record.setEventName(eventName);
        return record;
    }

    private PutEventsRequest createPutEventsRequest() {
        String expectedDetail = String.format(EXPECTED_DETAIL_TEMPLATE, EVENT_SOURCE_ARN);
        return PutEventsRequest.builder()
            .entries(PutEventsRequestEntry.builder()
                .eventBusName(EVENT_BUS)
                .time(NOW)
                .source(EventBridgePublisher.EVENT_SOURCE)
                .detailType(EventBridgePublisher.EVENT_DETAIL_TYPE)
                .detail(expectedDetail)
                .resources(EventBridgePublisherTest.EVENT_SOURCE_ARN)
                .build())
            .build();
    }

    private DynamodbEvent createDynamodbEvent() {
        DynamodbEvent.DynamodbStreamRecord record = new DynamodbEvent.DynamodbStreamRecord();
        record.setEventSourceARN(EVENT_SOURCE_ARN);
        return createDynamodbEvent(record);
    }

    private DynamodbEvent createDynamodbEvent(DynamodbStreamRecord... records) {
        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(Arrays.asList(records));
        return event;
    }

    private void prepareMocksWithSuccessfulPutEvents() {
        when(eventBridge.putEvents(any(PutEventsRequest.class))).thenReturn(Collections.emptyList());
    }

    private void prepareMocksWithFailingPutEventEntries() {
        List<PutEventsRequestEntry> failedEntries = createFailedEntries();
        when(eventBridge.putEvents(any(PutEventsRequest.class))).thenReturn(failedEntries);
    }
}

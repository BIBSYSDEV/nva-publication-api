package no.unit.nva.publication.events.handlers.dynamodbstream;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry.Builder;

public class EventBridgePublisherTest {
    public static final String DYNAMODB_UPDATE_EVENT_TOPIC = "PublicationService.Database.Update";
    public static final String EXPECTED_DETAIL_TEMPLATE = "{\"eventSourceARN\":\"%s\"}";
    public static final String FAILED_EVENT_NAME = "Failed";
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
    private AutoCloseable closeable;

    /**
     * Set up environment for test.
     */
    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        
        publisher = new EventBridgePublisher(eventBridge, failedEventPublisher,
            EVENT_BUS,
            DYNAMODB_UPDATE_EVENT_TOPIC,
            Clock.fixed(NOW, ZoneId.systemDefault()));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }
    
    @Test
    void publishCanPutEventsToEventBridge() {
        var event = createDynamodbEvent();
        prepareMocksWithSuccessfulPutEvents();
        
        publisher.publish(event);
        
        var expected = createPutEventsRequest();
        verify(eventBridge).putEvents(expected);
        verifyNoMoreInteractions(failedEventPublisher);
    }
    
    @Test
    void publishFailedEventWhenPutEventsToEventBridgeHasFailures() {
        
        prepareMocksWithFailingPutEventEntries();
        
        var failedRecord = createDynamodbStreamRecord(FAILED_EVENT_NAME);
        var event = createDynamodbEvent(failedRecord);
        
        publisher.publish(event);
        
        var partiallyFailingRequest = createFailingPutEventsRequest();
        verify(eventBridge).putEvents(partiallyFailingRequest);
        var failedEvent = createDynamodbEvent(failedRecord);
        verify(failedEventPublisher).publish(failedEvent);
    }
    
    private static Builder putEventRequestBuilder() {
        return PutEventsRequestEntry.builder()
                   .eventBusName(EVENT_BUS)
                   .time(NOW)
                   .source(EventBridgePublisher.EVENT_SOURCE)
                   .detailType(DYNAMODB_UPDATE_EVENT_TOPIC)
                   .resources(EVENT_SOURCE_ARN);
    }
    
    private PutEventsRequest createFailingPutEventsRequest() {
        var failedRecordString = String.format(RECORD_STRING_TEMPLATE, FAILED_EVENT_NAME, EVENT_SOURCE_ARN);
        return PutEventsRequest.builder()
                   .entries(PUT_EVENT_REQUEST_BUILDER.detail(failedRecordString).build())
                   .build();
    }
    
    private List<PutEventsRequestEntry> createFailedEntries() {
        var failedRecordString = String.format(RECORD_STRING_TEMPLATE, FAILED_EVENT_NAME,
            EventBridgePublisherTest.EVENT_SOURCE_ARN);
        return Collections.singletonList(
            PUT_EVENT_REQUEST_BUILDER
                .detail(failedRecordString)
                .build());
    }
    
    private DynamodbEvent.DynamodbStreamRecord createDynamodbStreamRecord(String    eventName) {
        var streamRecord = new DynamodbEvent.DynamodbStreamRecord();
        streamRecord.setEventSourceARN(EVENT_SOURCE_ARN);
        streamRecord.setEventName(eventName);
        return streamRecord;
    }
    
    private PutEventsRequest createPutEventsRequest() {
        var expectedDetail = String.format(EXPECTED_DETAIL_TEMPLATE, EVENT_SOURCE_ARN);
        return PutEventsRequest.builder()
                   .entries(PutEventsRequestEntry.builder()
                                .eventBusName(EVENT_BUS)
                                .time(NOW)
                                .source(EventBridgePublisher.EVENT_SOURCE)
                                .detailType(DYNAMODB_UPDATE_EVENT_TOPIC)
                                .detail(expectedDetail)
                                .resources(EventBridgePublisherTest.EVENT_SOURCE_ARN)
                                .build())
                   .build();
    }
    
    private DynamodbEvent createDynamodbEvent() {
        var streamRecord = new DynamodbEvent.DynamodbStreamRecord();
        streamRecord.setEventSourceARN(EVENT_SOURCE_ARN);
        return createDynamodbEvent(streamRecord);
    }
    
    private DynamodbEvent createDynamodbEvent(DynamodbStreamRecord... records) {
        var event = new DynamodbEvent();
        event.setRecords(Arrays.asList(records));
        return event;
    }
    
    private void prepareMocksWithSuccessfulPutEvents() {
        when(eventBridge.putEvents(any(PutEventsRequest.class))).thenReturn(Collections.emptyList());
    }
    
    private void prepareMocksWithFailingPutEventEntries() {
        var failedEntries = createFailedEntries();
        when(eventBridge.putEvents(any(PutEventsRequest.class))).thenReturn(failedEntries);
    }
}
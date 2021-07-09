package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static no.unit.nva.publication.s3imports.EventEmitter.NUMBER_OF_EVENTS_SENT_PER_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBus;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class EventEmitterTest {

    private EventBridgeClient eventBridgeClient;
    private AtomicInteger sleepingCounter;

    @BeforeEach
    public void init() {
        sleepingCounter = new AtomicInteger();
        sleepingCounter.set(0);
        eventBridgeClient = setupEventBridgeClient();
    }

    @Test
    public void emitEventsEmitsAllEventsWhenCalledWithoutArguments() {
        EventEmitter<String> eventEmitter = newEventEmitter();
        List<String> eventBodies = generateInputBiggerThanEventEmittersRequestSize();
        eventEmitter.addEvents(eventBodies);
        eventEmitter.emitEvents();
        int expectedNumberOfPutEventRequests = eventBodies.size() / NUMBER_OF_EVENTS_SENT_PER_REQUEST;
        verify(eventBridgeClient, times(expectedNumberOfPutEventRequests)).putEvents(any(PutEventsRequest.class));
        assertThat(sleepingCounter.get(), is(equalTo(1)));
    }

    @Test
    public void emitEventsEmitsAllEventsWithMinimalDelay() {
        EventEmitter<String> eventEmitter = newEventEmitter();
        List<String> eventBodies = generateInputBiggerThanEventEmittersRequestSize();
        eventEmitter.addEvents(eventBodies);
        eventEmitter.emitEvents();
        int expectedNumberOfPutEventRequests = eventBodies.size() / NUMBER_OF_EVENTS_SENT_PER_REQUEST;
        verify(eventBridgeClient, times(expectedNumberOfPutEventRequests)).putEvents(any(PutEventsRequest.class));
        assertThat(sleepingCounter.get(), is(equalTo(1)));
    }

    @Test
    public void emitEventsEmitsAllEventsWhenCalledWithArguments() {
        EventEmitter<String> eventEmitter = newEventEmitter();
        List<String> eventBodies = generateInputBiggerThanEventEmittersRequestSize();
        eventEmitter.addEvents(eventBodies);
        int desiredBatchSize = 20;
        eventEmitter.emitEvents(desiredBatchSize);
        int expectedNumberOfPutEventRequests = eventBodies.size() / NUMBER_OF_EVENTS_SENT_PER_REQUEST;
        verify(eventBridgeClient, times(expectedNumberOfPutEventRequests)).putEvents(any(PutEventsRequest.class));
        int expectedEmissionGroups = eventBodies.size() / desiredBatchSize;
        assertThat(sleepingCounter.get(), is(equalTo(expectedEmissionGroups)));
    }

    @Test
    public void emitEventsEmitsAllEventInEmissionGroupsPausingAfterEachEmissionGroup() {

        EventEmitter<String> eventEmitter = newEventEmitter();
        List<String> eventBodies = generateInputBiggerThanEventEmittersRequestSize();
        eventEmitter.addEvents(eventBodies);
        //The actual batch size will always be a multiple of the events that we send per request
        int desiredBatchSize = 2 * NUMBER_OF_EVENTS_SENT_PER_REQUEST;
        eventEmitter.emitEvents(desiredBatchSize);

        int expectedEmissionGroups = eventBodies.size() / desiredBatchSize;
        assertThat(sleepingCounter.get(), is(equalTo(expectedEmissionGroups)));
    }

    private EventEmitter<String> newEventEmitter() {
        return new EventEmitter<>(randomString(),
                                  randomString(),
                                  randomString(),
                                  eventBridgeClient) {
            @Override
            protected void reduceEmittingRate() {
                sleepingCounter.incrementAndGet();
            }
        };
    }

    private EventBridgeClient setupEventBridgeClient() {
        EventBridgeClient client = mock(EventBridgeClient.class);
        when(client.listEventBuses(any(ListEventBusesRequest.class)))
            .thenReturn(mockListEventBusesResponse());
        when(client.putEvents(any(PutEventsRequest.class)))
            .thenReturn(mockPutEventResult());
        return client;
    }

    private PutEventsResponse mockPutEventResult() {
        return PutEventsResponse.builder().failedEntryCount(0).build();
    }

    private ListEventBusesResponse mockListEventBusesResponse() {
        EventBus eventBus = EventBus.builder().name(ApplicationConstants.EVENT_BUS_NAME).build();
        return ListEventBusesResponse.builder()
                   .eventBuses(eventBus)
                   .build();
    }

    private List<String> generateInputBiggerThanEventEmittersRequestSize() {
        return IntStream.range(0, NUMBER_OF_EVENTS_SENT_PER_REQUEST * 100)
                   .boxed().map(i -> randomString())
                   .collect(Collectors.toList());
    }
}
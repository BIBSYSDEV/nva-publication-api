package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static no.unit.nva.publication.s3imports.EventEmitter.NUMBER_OF_EVENTS_SENT_PER_REQUEST;
import static no.unit.nva.publication.s3imports.EventEmitter.NUMBER_OF_REQUEST_ENTRIES;
import static no.unit.nva.publication.s3imports.EventEmitter.REQUEST_ENTRY_SET_MAX_BYTE_SIZE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.github.javafaker.Faker;
import com.github.javafaker.Lorem;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBridgeException;
import software.amazon.awssdk.services.eventbridge.model.EventBus;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class EventEmitterTest {

    public static final int NUMBER_OF_EMITTED_ENTRIES_PER_BATCH = 10;
    public static final Lorem FAKER = Faker.instance().lorem();
    private EventBridgeClient eventBridgeClient;
    private AtomicInteger sleepingCounter;

    @BeforeEach
    public void init() {
        sleepingCounter = new AtomicInteger();
        sleepingCounter.set(0);
        eventBridgeClient = setupEventBridgeClient();
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
        int desiredEmittedEntriesPetBatch = 2 * NUMBER_OF_EVENTS_SENT_PER_REQUEST;
        eventEmitter.emitEvents(desiredEmittedEntriesPetBatch);

        int expectedEmissionGroups = eventBodies.size() / desiredEmittedEntriesPetBatch;
        assertThat(sleepingCounter.get(), is(equalTo(expectedEmissionGroups)));
    }

    @Test
    public void emitEventLogsNumberOfEntriesInRequestWhenEventEmissionFails() {
        eventBridgeClient = eventBridgeClientThrowsExceptionWhenPuttingRequests();
        TestAppender logAppender = LogUtils.getTestingAppender(EventEmitter.class);
        EventEmitter<String> eventEmitter = newEventEmitter();
        List<String> eventBodies = generateInputBiggerThanEventEmittersRequestSize();
        eventEmitter.addEvents(eventBodies);
        assertThrows(EventBridgeException.class, () -> eventEmitter.emitEvents(NUMBER_OF_EVENTS_SENT_PER_REQUEST));
        assertThat(logAppender.getMessages(),
                   containsString(NUMBER_OF_REQUEST_ENTRIES + NUMBER_OF_EVENTS_SENT_PER_REQUEST));
    }

    @ParameterizedTest
    @CsvSource({"250000,10", "120000,5"})
    public void eventEmitterCreatesPutEventRequestsThatDoNotExceedAmazonsLimits(String individualEntrySize,
                                                                                String numberOfExpectedRequests) {

        EventEmitter<String> eventEmitter = newEventEmitter();
        List<String> eventBodies = generateEventsOfSpecificSize(Integer.parseInt(individualEntrySize));
        eventEmitter.addEvents(eventBodies);
        double expectedNumberOfRequests = Integer.parseInt(numberOfExpectedRequests);
        double acceptableError = 1.0;
        double actualSize = eventEmitter.getPutEventsRequests().size();
        assertThat(actualSize, is(closeTo(expectedNumberOfRequests, acceptableError)));
    }

    @Test
    public void eventEmitterThrowsExceptionWhenDataEntryIsTooBigForAwsEventBridgeEvent() {
        EventEmitter<String> eventEmitter = newEventEmitter();
        int sizeBiggerThanAcceptable = REQUEST_ENTRY_SET_MAX_BYTE_SIZE + 100;
        List<String> eventBodies = generateEventsOfSpecificSize(sizeBiggerThanAcceptable);
        assertThrows(EntryTooBigException.class, () -> eventEmitter.addEvents(eventBodies));
    }

    private EventBridgeClient eventBridgeClientThrowsExceptionWhenPuttingRequests() {
        EventBridgeClient client = mock(EventBridgeClient.class);
        when(client.listEventBuses(any(ListEventBusesRequest.class)))
            .thenReturn(mockListEventBusesResponse());
        when(client.putEvents(any(PutEventsRequest.class)))
            .thenThrow(EventBridgeException.builder().message("Unimportant message").build());
        return client;
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

    private List<String> generateEventsOfSpecificSize(int requestSize) {
        return IntStream.range(0, NUMBER_OF_EVENTS_SENT_PER_REQUEST)
            .boxed()
            .map(ignored -> FAKER.characters(requestSize))
            .collect(Collectors.toList());
    }
}
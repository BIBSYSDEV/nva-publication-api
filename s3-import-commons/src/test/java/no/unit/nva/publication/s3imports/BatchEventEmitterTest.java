package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.BatchEventEmitter.NUMBER_OF_EVENTS_SENT_PER_REQUEST;
import static no.unit.nva.publication.s3imports.BatchEventEmitter.NUMBER_OF_REQUEST_ENTRIES;
import static no.unit.nva.publication.s3imports.BatchEventEmitter.REQUEST_ENTRY_SET_MAX_BYTE_SIZE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.datafaker.providers.base.BaseFaker;
import net.datafaker.providers.base.Lorem;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBridgeException;
import software.amazon.awssdk.services.eventbridge.model.EventBus;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

@ExtendWith(MockitoExtension.class)
public class BatchEventEmitterTest {

    public static final Lorem FAKER = new BaseFaker().lorem();
    private EventBridgeClient eventBridgeClient;

    @BeforeEach
    public void init(@Mock EventBridgeClient client) {
        eventBridgeClient = client;
    }
    
    @Test
    void emitEventsEmitsAllEventsWhenCalledWithArguments() {
        mockEventBridgeListEventBuses(eventBridgeClient);
        BatchEventEmitter<String> batchEventEmitter = newEventEmitter();
        List<String> manyEvents = generateInputBiggerThanEventEmittersRequestSize();
        mockEventBridgePutEvents(eventBridgeClient);
        batchEventEmitter.addEvents(manyEvents);
        int desiredBatchSize = 20;
        batchEventEmitter.emitEvents(desiredBatchSize);
        int expectedNumberOfPutEventRequests = manyEvents.size() / NUMBER_OF_EVENTS_SENT_PER_REQUEST;
        verify(eventBridgeClient, times(expectedNumberOfPutEventRequests)).putEvents(any(PutEventsRequest.class));
    }
    
    @Test
    void emitEventLogsNumberOfEntriesInRequestWhenEventEmissionFails(@Mock EventBridgeClient client) {
        eventBridgeClient = eventBridgeClientThrowsExceptionWhenPuttingRequests(client);
        TestAppender logAppender = LogUtils.getTestingAppender(BatchEventEmitter.class);
        BatchEventEmitter<String> batchEventEmitter = newEventEmitter();
        List<String> eventBodies = generateInputBiggerThanEventEmittersRequestSize();
        batchEventEmitter.addEvents(eventBodies);
        assertThrows(EventBridgeException.class, () -> batchEventEmitter.emitEvents(NUMBER_OF_EVENTS_SENT_PER_REQUEST));
        assertThat(logAppender.getMessages(),
            containsString(NUMBER_OF_REQUEST_ENTRIES + NUMBER_OF_EVENTS_SENT_PER_REQUEST));
    }
    
    @ParameterizedTest
    @CsvSource({"250000,10", "120000,5"})
    void eventEmitterCreatesPutEventRequestsThatDoNotExceedAmazonsLimits(String individualEntrySize,
                                                                                String numberOfExpectedRequests) {
        
        BatchEventEmitter<String> batchEventEmitter = newEventEmitter();
        List<String> eventBodies = generateEventsOfSpecificSize(Integer.parseInt(individualEntrySize));
        batchEventEmitter.addEvents(eventBodies);
        double expectedNumberOfRequests = Integer.parseInt(numberOfExpectedRequests);
        double acceptableError = 1.0;
        double actualSize = batchEventEmitter.getPutEventsRequests().size();
        assertThat(actualSize, is(closeTo(expectedNumberOfRequests, acceptableError)));
    }
    
    @Test
    void eventEmitterThrowsExceptionWhenDataEntryIsTooBigForAwsEventBridgeEvent() {
        BatchEventEmitter<String> batchEventEmitter = newEventEmitter();
        int sizeBiggerThanAcceptable = REQUEST_ENTRY_SET_MAX_BYTE_SIZE + 100;
        List<String> eventBodies = generateEventsOfSpecificSize(sizeBiggerThanAcceptable);
        assertThrows(EntryTooBigException.class, () -> batchEventEmitter.addEvents(eventBodies));
    }
    
    private EventBridgeClient eventBridgeClientThrowsExceptionWhenPuttingRequests( EventBridgeClient client) {
        mockEventBridgeListEventBuses(client);
        when(client.putEvents(any(PutEventsRequest.class)))
            .thenThrow(EventBridgeException.builder().message("Unimportant message").build());
        return client;
    }
    
    private BatchEventEmitter<String> newEventEmitter() {
        return new BatchEventEmitter<>(randomString(),
            randomString(),
            eventBridgeClient);
    }

    private void mockEventBridgePutEvents(EventBridgeClient client) {
        when(client.putEvents(any(PutEventsRequest.class)))
            .thenReturn(mockPutEventResult());
    }

    private void mockEventBridgeListEventBuses(EventBridgeClient client) {
        when(client.listEventBuses(any(ListEventBusesRequest.class)))
            .thenReturn(mockListEventBusesResponse());
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
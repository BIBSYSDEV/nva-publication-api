package no.unit.nva.doi.publisher;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

public class EventBridgeRetryClientTest {

    private static final int MAX_ATTEMPT = 3;
    @Mock
    private EventBridgeClient eventBridge;

    private EventBridgeRetryClient client;

    /**
     * Set up environment for test.
     */
    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        client = new EventBridgeRetryClient(eventBridge, MAX_ATTEMPT);
    }

    @Test
    public void putEventsIsSuccessReturnsNoFailingEntries() {
        prepareMocksWithSuccessfulResponse();

        PutEventsRequest request = PutEventsRequest.builder()
            .build();
        List<PutEventsRequestEntry> result = client.putEvents(request);

        assertEquals(0, result.size());
        verify(eventBridge).putEvents(request);
    }

    @Test
    public void putEventsFailureReturnsFailingEntry() {
        var failedResponseEntry = createFailedPutEventsResultEntry();
        List<PutEventsResultEntry> resultEntries = asList(failedResponseEntry);
        var response = createPutEventsResponse(1, resultEntries);

        when(eventBridge.putEvents(any(PutEventsRequest.class))).thenReturn(response);

        var failedEntry = createPutEventsRequestEntry("failed entry");
        List<PutEventsRequestEntry> requestEntries = asList(failedEntry);
        var request = createPutEventsRequest(requestEntries);

        List<PutEventsRequestEntry> result = client.putEvents(request);

        assertEquals(1, result.size());
        verify(eventBridge, times(3)).putEvents(request);
    }

    @Test
    public void putEventsPartialFailureReturnsFailingEntry() {
        var responseEntry = createPutEventsResultEntry();
        var failedResponseEntry = createFailedPutEventsResultEntry();
        List<PutEventsResultEntry> resultEntries = asList(responseEntry, failedResponseEntry);

        var firstResponse = createPutEventsResponse(1, resultEntries);
        var secondResponse = createPutEventsResponse(1, singletonList(failedResponseEntry));
        var thirdResponse = createPutEventsResponse(1, singletonList(failedResponseEntry));

        prepareMocksWithConsecutiveResponses(firstResponse, secondResponse, thirdResponse);

        var successEntry = createPutEventsRequestEntry("success entry");
        var failedEntry = createPutEventsRequestEntry("failed entry");
        List<PutEventsRequestEntry> requestEntries = asList(successEntry, failedEntry);
        var request = createPutEventsRequest(requestEntries);

        List<PutEventsRequestEntry> result = client.putEvents(request);

        assertEquals(1, result.size());
        verify(eventBridge).putEvents(request);
    }


    private void prepareMocksWithSuccessfulResponse() {
        PutEventsResponse response = PutEventsResponse.builder()
            .failedEntryCount(0)
            .build();
        when(eventBridge.putEvents(any(PutEventsRequest.class))).thenReturn(response);
    }

    @Test
    public void putEventsRetriesOnFailure() {
        var successEntry = createPutEventsRequestEntry("success entry");
        var failedEntry = createPutEventsRequestEntry("failed entry");
        List<PutEventsRequestEntry> requestEntries = asList(successEntry, failedEntry);

        var responseEntry = createPutEventsResultEntry();
        var failedResponseEntry = createFailedPutEventsResultEntry();
        List<PutEventsResultEntry> resultEntries = asList(responseEntry, failedResponseEntry);

        var firstResponse = createPutEventsResponse(1, resultEntries);
        var secondResponse = createPutEventsResponse(0, emptyList());

        prepareMocksWithConsecutiveResponses(firstResponse, secondResponse);

        var request = createPutEventsRequest(requestEntries);
        List<PutEventsRequestEntry> result = client.putEvents(request);

        assertEquals(0, result.size());
        ArgumentCaptor<PutEventsRequest> putEventsRequestArgumentCaptor = ArgumentCaptor.forClass(
            PutEventsRequest.class);
        verify(eventBridge, times(2)).putEvents(putEventsRequestArgumentCaptor.capture());

        var secondRequest = createPutEventsRequest(singletonList(failedEntry));
        List<PutEventsRequest> expected = asList(request, secondRequest);
        assertEquals(putEventsRequestArgumentCaptor.getAllValues(), expected);
    }

    @Test
    public void putEventsRetriesAndReachesMaxAttempt() {
        var successEntry = createPutEventsRequestEntry("success entry");
        var failedEntry = createPutEventsRequestEntry("failed entry");
        List<PutEventsRequestEntry> requestEntries = asList(successEntry, failedEntry);

        var responseEntry = createPutEventsResultEntry();
        var failedResponseEntry = createFailedPutEventsResultEntry();
        List<PutEventsResultEntry> resultEntries = asList(responseEntry, failedResponseEntry);

        var response = createPutEventsResponse(1, resultEntries);
        var failedResponse = createPutEventsResponse(1, singletonList(failedResponseEntry));
        prepareMocksWithConsecutiveResponses(response, failedResponse, failedResponse);

        var request = createPutEventsRequest(requestEntries);
        List<PutEventsRequestEntry> result = client.putEvents(request);

        var failedRequest = createPutEventsRequest(singletonList(failedEntry));
        assertEquals(result, failedRequest.entries());
        ArgumentCaptor<PutEventsRequest> putEventsRequestArgumentCaptor = ArgumentCaptor.forClass(
            PutEventsRequest.class);
        verify(eventBridge, times(3)).putEvents(putEventsRequestArgumentCaptor.capture());
        List<PutEventsRequest> expected = asList(request, failedRequest, failedRequest);
        assertEquals(putEventsRequestArgumentCaptor.getAllValues(), expected);
    }

    private PutEventsRequest createPutEventsRequest(List<PutEventsRequestEntry> requestEntries) {
        return PutEventsRequest.builder()
            .entries(requestEntries)
            .build();
    }

    private void prepareMocksWithConsecutiveResponses(PutEventsResponse firstResponse,
                                                      PutEventsResponse secondResponse) {
        when(eventBridge.putEvents(any(PutEventsRequest.class)))
            .thenReturn(firstResponse)
            .thenReturn(secondResponse);
    }

    private void prepareMocksWithConsecutiveResponses(PutEventsResponse firstResponse,
                                                      PutEventsResponse secondResponse,
                                                      PutEventsResponse thirdResponse) {
        when(eventBridge.putEvents(any(PutEventsRequest.class)))
            .thenReturn(firstResponse)
            .thenReturn(secondResponse)
            .thenReturn(thirdResponse);
    }

    private PutEventsResponse createPutEventsResponse(int failedEntryCount, List<PutEventsResultEntry> resultEntries) {
        return PutEventsResponse.builder()
            .failedEntryCount(failedEntryCount)
            .entries(resultEntries)
            .build();
    }

    private PutEventsResultEntry createPutEventsResultEntry() {
        return PutEventsResultEntry.builder().build();
    }

    private PutEventsRequestEntry createPutEventsRequestEntry(String s) {
        return PutEventsRequestEntry.builder()
            .detail(s)
            .build();
    }

    private PutEventsResultEntry createFailedPutEventsResultEntry() {
        PutEventsResultEntry failedResponseEntry = PutEventsResultEntry.builder()
            .errorCode("failed").build();
        return failedResponseEntry;
    }
}

package no.unit.nva.cristin.lambda;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class EventEmitter<T extends JsonSerializable> {

    private static final int BATCH_SIZE = 10;
    private static final int MAX_ATTEMPTS = 10;
    private final String detailType;
    private final String invokingFunctionArn;
    private final EventBridgeClient client;
    private List<PutEventsRequest> putEventsRequests;

    public EventEmitter(String detailType, String invokingFunctionArn, EventBridgeClient eventBridgeClient) {
        this.detailType = detailType;
        this.invokingFunctionArn = invokingFunctionArn;
        this.client = eventBridgeClient;
    }

    public void addEvents(Collection<T> events) {

        List<PutEventsRequestEntry> entries = events.stream()
                                                  .map(this::createPutEventRequesEntry)
                                                  .collect(Collectors.toList());
        putEventsRequests = createBatchesOfPutEventsRequests(entries);
    }

    public List<PutEventsResult> emitEvents() {
        List<PutEventsResult> failedEvents = emitEventsAndCollectFailures(putEventsRequests);
        int attempts = 0;
        while (!failedEvents.isEmpty() && attempts < MAX_ATTEMPTS) {
            List<PutEventsRequest> requestsToResend = collectRequestsForResending(failedEvents);
            failedEvents = emitEventsAndCollectFailures(requestsToResend);
            attempts++;
        }
        if (!failedEvents.isEmpty()) {
            return failedEvents;
        }

        return Collections.emptyList();
    }

    private PutEventsRequestEntry createPutEventRequesEntry(T filenameEvent) {

        return PutEventsRequestEntry.builder()
                   .eventBusName(ApplicationConstants.EVENT_BUS_NAME)
                   .resources(invokingFunctionArn)
                   .detailType(detailType)
                   .time(Instant.now())
                   .detail(filenameEvent.toJsonString())
                   .source(invokingFunctionArn)
                   .build();
    }

    private List<PutEventsRequest> createBatchesOfPutEventsRequests(List<PutEventsRequestEntry> entries) {
        List<PutEventsRequest> eventsRequests = new ArrayList<>();

        for (int subListStartIndex = 0; subListStartIndex < entries.size(); subListStartIndex += BATCH_SIZE) {
            int subListEndIndex = endIndex(subListStartIndex, entries);
            List<PutEventsRequestEntry> sublist = entries.subList(subListStartIndex, subListEndIndex);
            PutEventsRequest putEventsRequest = PutEventsRequest.builder().entries(sublist).build();
            eventsRequests.add(putEventsRequest);
        }
        return eventsRequests;
    }

    private int endIndex(int index, List<PutEventsRequestEntry> originalList) {
        return Math.min(index + BATCH_SIZE, originalList.size());
    }

    @JacocoGenerated
    private List<PutEventsRequest> collectRequestsForResending(List<PutEventsResult> failedEvents) {
        return failedEvents.stream().map(PutEventsResult::getRequest).collect(Collectors.toList());
    }

    private List<PutEventsResult> emitEventsAndCollectFailures(
        List<PutEventsRequest> eventRequests) {
        return eventRequests.stream()
                   .map(this::emitEvent)
                   .filter(PutEventsResult::hasFailures)
                   .collect(Collectors.toList());
    }

    private PutEventsResult emitEvent(PutEventsRequest request) {
        PutEventsResponse result = client.putEvents(request);
        return new PutEventsResult(request, result);
    }
}

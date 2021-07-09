package no.unit.nva.publication.s3imports;

import static nva.commons.core.attempt.Try.attempt;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;
import org.apache.commons.collections4.ListUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBus;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/**
 * This class accepts a set of colletion of {@link JsonSerializable} objects and emits one event per object in
 * EventBridge. The events have the following format:
 *
 * <p><pre>{@code
 * {
 *   "version": "0", // set by AWS automatically
 *   "id": "6a7e8feb-b491-4cf7-a9f1-bf3703467718", // set by AWS automatically
 *   "detail-type": "<supplied by user>", //all events have the same detail-type
 *   "source": "<supplied_invoked_function_arn>",
 *   "account": "111122223333", //set by AWS.
 *   "time": "2017-12-22T18:43:48Z", // Instant.now()
 *   "region": "us-west-1",//set by AWS.
 *   "resources": [
 *     <supplied_invoked_function_arn>
 *   ],
 *   "detail": {<supplied_body>}
 * }
 * }</pre>
 *
 * @param <T> the type of the event detail.
 */
public class EventEmitter<T> {

    public static final String BUS_NOT_FOUND_ERROR = "EventBridge bus not found: ";
    protected static final int NUMBER_OF_EVENTS_SENT_PER_REQUEST = 10;
    private static final int MAX_ATTEMPTS = 10;
    private final String detailType;
    private final String invokingFunctionArn;
    private final EventBridgeClient client;
    private final String eventSource;
    private List<PutEventsRequest> putEventsRequests;

    public EventEmitter(String detailType,
                        String eventSource,
                        String invokingFunctionArn,
                        EventBridgeClient eventBridgeClient) {
        this.detailType = detailType;
        this.invokingFunctionArn = invokingFunctionArn;
        this.client = eventBridgeClient;
        this.eventSource = eventSource;
    }

    /**
     * Create one event for every element in the "eventDetails" list.
     *
     * @param eventDetails A collection of details for the respective events.
     */
    public void addEvents(Collection<T> eventDetails) {
        addEvents(eventDetails.stream());
    }

    /**
     * Create one event for every element in the "eventDetails" list.
     *
     * @param eventDetails A collection of details for the respective events.
     */
    public void addEvents(Stream<T> eventDetails) {
        List<PutEventsRequestEntry> eventRequestEntries = eventDetails
                                                              .map(this::createPutEventRequestEntry)
                                                              .collect(Collectors.toList());
        putEventsRequests = createBatchesOfPutEventsRequests(eventRequestEntries);
    }



    public List<PutEventsResult> emitEvents(int numberOfEmittedEntriesPerBatch) {
        checkBus();
        List<PutEventsResult> failedEvents = tryManyTimesToEmitTheEntriesInTheFile(numberOfEmittedEntriesPerBatch);
        if (failedEvents != null) {
            return failedEvents;
        }
        return Collections.emptyList();
    }

    protected void reduceEmittingRate() {
        try {
            Thread.sleep(ApplicationConstants.WAIT_TIME_IN_MILLIS_FOR_EMITTING_BATCHES_OF_FILENAMES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkBus() {
        List<String> busNames =
            client.listEventBuses(
                ListEventBusesRequest.builder().namePrefix(ApplicationConstants.EVENT_BUS_NAME).build())
                .eventBuses()
                .stream()
                .map(EventBus::name)
                .collect(Collectors.toList());
        if (!busNames.contains(ApplicationConstants.EVENT_BUS_NAME)) {
            throw new IllegalStateException(BUS_NOT_FOUND_ERROR + ApplicationConstants.EVENT_BUS_NAME);
        }
    }

    private PutEventsRequestEntry createPutEventRequestEntry(T eventDetail) {

        return PutEventsRequestEntry.builder()
                   .eventBusName(ApplicationConstants.EVENT_BUS_NAME)
                   .resources(invokingFunctionArn)
                   .detailType(detailType)
                   .time(Instant.now())
                   .detail(toJson(eventDetail))
                   .source(eventSource)
                   .build();
    }

    private String toJson(T eventDetail) {
        return attempt(() -> JsonUtils.objectMapperNoEmpty.writeValueAsString(eventDetail)).orElseThrow();
    }

    private List<PutEventsResult> tryManyTimesToEmitTheEntriesInTheFile(int numberOfEntriesEmittedPerBatch) {
        List<PutEventsResult> failedEvents = emitEventsAndCollectFailures(putEventsRequests,
                                                                          numberOfEntriesEmittedPerBatch);
        int attempts = 0;
        while (!failedEvents.isEmpty() && attempts < MAX_ATTEMPTS) {
            List<PutEventsRequest> requestsToResend = collectRequestsForResending(failedEvents);
            failedEvents = emitEventsAndCollectFailures(requestsToResend, numberOfEntriesEmittedPerBatch);
            attempts++;
        }
        if (!failedEvents.isEmpty()) {
            return failedEvents;
        }
        return null;
    }

    private List<PutEventsRequest> collectRequestsForResending(List<PutEventsResult> failedEvents) {
        return failedEvents.stream().map(PutEventsResult::getRequest).collect(Collectors.toList());
    }

    private List<PutEventsRequest> createBatchesOfPutEventsRequests(List<PutEventsRequestEntry> entries) {
        return ListUtils.partition(entries, NUMBER_OF_EVENTS_SENT_PER_REQUEST)
                   .stream()
                   .map(batch -> PutEventsRequest.builder().entries(batch).build())
                   .collect(Collectors.toList());
    }

    private List<PutEventsResult> emitEventsAndCollectFailures(List<PutEventsRequest> eventRequests,
                                                               int numberOfEntriesEmittedPerBatch) {
        List<PutEventsResult> putEventsResults = new ArrayList<>();
        List<List<PutEventsRequest>> requestBatches = createRequestBatches(eventRequests,
                                                                           numberOfEntriesEmittedPerBatch);
        for (List<PutEventsRequest> batch : requestBatches) {
            List<PutEventsResult> batchResults = emitBatch(batch);
            reduceEmittingRate();
            putEventsResults.addAll(batchResults);
        }

        return putEventsResults;
    }

    private List<PutEventsResult> emitBatch(List<PutEventsRequest> batch) {
        return batch.stream()
                   .map(this::emitEvent)
                   .filter(PutEventsResult::hasFailures)
                   .collect(Collectors.toList());
    }

    private List<List<PutEventsRequest>> createRequestBatches(List<PutEventsRequest> eventRequests,
                                                              int numberOfEntriesEmittedPerBatch) {
        ArrayList<List<PutEventsRequest>> result = new ArrayList<>();

        int numberOfRequests = calculateNumberOfRequestsSentPerBatch(numberOfEntriesEmittedPerBatch);

        for (int startIndex = 0; startIndex < eventRequests.size(); startIndex = startIndex + numberOfRequests) {
            List<PutEventsRequest> batch =
                eventRequests.subList(startIndex, endIndex(eventRequests, startIndex + numberOfRequests));
            result.add(batch);
        }

        return result;
    }

    private int calculateNumberOfRequestsSentPerBatch(int numberOfEntriesEmittedPerBatch) {
        int numberOfRequests = numberOfEntriesEmittedPerBatch / NUMBER_OF_EVENTS_SENT_PER_REQUEST;
        return numberOfRequests == 0 ? 1 : numberOfRequests;
    }

    private int endIndex(List<PutEventsRequest> eventRequests, int overfloadingEndIndex) {
        return Math.min(overfloadingEndIndex, eventRequests.size());
    }

    private PutEventsResult emitEvent(PutEventsRequest request) {
        PutEventsResponse result = client.putEvents(request);
        return new PutEventsResult(request, result);
    }
}

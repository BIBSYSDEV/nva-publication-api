package no.unit.nva.publication.s3imports;

import static nva.commons.core.attempt.Try.attempt;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Failure;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public static final String NUMBER_OF_REQUEST_ENTRIES = "Number of request entries:";
    public static final int TIMESTAMP_SIZE_IN_BYTES = 14;
    protected static final int NUMBER_OF_EVENTS_SENT_PER_REQUEST = 10;
    protected static final int REQUEST_ENTRY_SET_MAX_BYTE_SIZE = 256_000; // 256KB with some slack
    private static final int MAX_ATTEMPTS = 10;
    private static final Logger logger = LoggerFactory.getLogger(EventEmitter.class);
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
        putEventsRequests = createPutEventsRequests(eventRequestEntries);
    }

    /**
     * The emitted entries are sent in batches and there is a waiting time between each batch, so that EventBridge is
     * not overwhelmed with messages and starts rejecting PutEventRequests.
     *
     * @param numberOfEmittedEventsPerRequest Number of data entries emitted per batch
     * @return a list of PutEventResults, one for each PutEventRequest. (A request may contain more than one data
     *     entries).
     */
    public List<PutEventsResult> emitEvents(int numberOfEmittedEventsPerRequest) {
        checkBus();
        List<PutEventsResult> failedEvents = tryManyTimesToEmitTheEventRequests(numberOfEmittedEventsPerRequest);
        if (failedEvents != null) {
            return failedEvents;
        }
        return Collections.emptyList();
    }

    protected List<PutEventsRequest> getPutEventsRequests() {
        return putEventsRequests;
    }

    protected void reduceEmittingRate() {
        try {
            Thread.sleep(ApplicationConstants.WAIT_TIME_IN_MILLIS_FOR_EMITTING_BATCHES_OF_FILENAMES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkBus() {
        List<String> busNames = listAllBusNames();
        if (!busNames.contains(ApplicationConstants.EVENT_BUS_NAME)) {
            throw new IllegalStateException(BUS_NOT_FOUND_ERROR + ApplicationConstants.EVENT_BUS_NAME);
        }
    }

    private List<String> listAllBusNames() {
        return client.listEventBuses(
                ListEventBusesRequest.builder().namePrefix(ApplicationConstants.EVENT_BUS_NAME).build())
            .eventBuses()
            .stream()
            .map(EventBus::name)
            .collect(Collectors.toList());
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

    private List<PutEventsResult> tryManyTimesToEmitTheEventRequests(int numberOfEntriesEmittedPerBatch) {
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

    private List<PutEventsRequest> createPutEventsRequests(List<PutEventsRequestEntry> entries) {
        return ListUtils.partition(entries, NUMBER_OF_EVENTS_SENT_PER_REQUEST)
            .stream()
            .map(batch -> PutEventsRequest.builder().entries(batch).build())
            .flatMap(this::splitBigEventRequests)
            .collect(Collectors.toList());
    }

    private Stream<PutEventsRequest> splitBigEventRequests(PutEventsRequest eventRequest) {
        Integer totalRequestSize = calculateTotalRequestSize(eventRequest);
        if (totalRequestSize > REQUEST_ENTRY_SET_MAX_BYTE_SIZE) {
            return splitRequestRecursively(eventRequest);
        } else {
            return Stream.of(eventRequest);
        }
    }

    private Integer calculateTotalRequestSize(PutEventsRequest eventsRequest) {
        return eventsRequest.entries()
            .stream()
            .map(this::requestEntrySize)
            .reduce(Integer::sum)
            .orElse(0);
    }

    private Stream<PutEventsRequest> splitRequestRecursively(PutEventsRequest putEventsRequest) {
        List<PutEventsRequestEntry> requestEntries = putEventsRequest.entries();
        int splitPoint = requestEntries.size() / 2;
        List<PutEventsRequestEntry> leftPartition = requestEntries.subList(0, splitPoint);
        List<PutEventsRequestEntry> rightPartition = requestEntries.subList(splitPoint, requestEntries.size());
        PutEventsRequest leftPartitionRequest = PutEventsRequest.builder().entries(leftPartition).build();
        PutEventsRequest rightPartitionRequest = PutEventsRequest.builder().entries(rightPartition).build();
        return Stream.of(splitBigEventRequests(leftPartitionRequest),
                         splitBigEventRequests(rightPartitionRequest))
            .flatMap(stream -> stream);
    }

    private List<PutEventsResult> emitEventsAndCollectFailures(List<PutEventsRequest> eventRequests,
                                                               int numberOfEntriesEmittedPerBatch) {
        List<List<PutEventsRequest>> requestBatches = createRequestBatches(eventRequests,
                                                                           numberOfEntriesEmittedPerBatch);

        return sendRequestBatchesAndPauseAfterSendingEachBatch(requestBatches);
    }

    private List<PutEventsResult> sendRequestBatchesAndPauseAfterSendingEachBatch(
        List<List<PutEventsRequest>> requestBatches) {
        List<PutEventsResult> putEventsResults = new ArrayList<>();

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
            int endIndex = endIndex(eventRequests, startIndex + numberOfRequests);
            List<PutEventsRequest> batch = eventRequests.subList(startIndex, endIndex);
            if (batch.isEmpty()) {
                logger.warn(String.format("Batch is empty:startIndex:%s endIndex:%s", startIndex, endIndex));
            }
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
        request.entries().forEach(this::log);
        PutEventsResponse result = attempt(() -> client.putEvents(request))
            .orElseThrow(fail -> logEmissionFailureDetails(fail, request));
        return new PutEventsResult(request, result);
    }

    private void log(PutEventsRequestEntry eventEntry) {
        logger.info("EmittingEvent: " + eventEntry.detail());
    }

    private int requestEntrySize(PutEventsRequestEntry entry) {
        int size = entry.source().getBytes(StandardCharsets.UTF_8).length
                   + entry.detail().getBytes(StandardCharsets.UTF_8).length
                   + entry.detailType().getBytes(StandardCharsets.UTF_8).length
                   + TIMESTAMP_SIZE_IN_BYTES;
        if (size > REQUEST_ENTRY_SET_MAX_BYTE_SIZE) {
            throw new EntryTooBigException(entry.detail());
        }
        return size;
    }

    private RuntimeException logEmissionFailureDetails(Failure<PutEventsResponse> fail, PutEventsRequest request) {
        logger.info(NUMBER_OF_REQUEST_ENTRIES + request.entries().size());
        if (fail.getException() instanceof RuntimeException) {
            return (RuntimeException) fail.getException();
        } else {
            return new RuntimeException(fail.getException());
        }
    }
}

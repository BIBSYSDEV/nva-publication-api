package no.unit.nva.cristin.lambda;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.core.JsonSerializable;
import org.apache.commons.collections4.ListUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

/**
 * Emits events in AWS EventBridge each event has the following format:
 * <p><pre>{@code
 * {
 *   "version": "0",
 *   "id": "6a7e8feb-b491-4cf7-a9f1-bf3703467718",
 *   "detail-type": "EC2 Instance State-change Notification",
 *   "source": "aws.ec2",
 *   "account": "111122223333",
 *   "time": "2017-12-22T18:43:48Z",
 *   "region": "us-west-1",
 *   "resources": [
 *     "arn:aws:ec2:us-west-1:123456789012:instance/i-1234567890abcdef0"
 *   ],
 *   "detail": {
 *     "someField": "someValue",
 *     "someOtherField": "someOtherValue"
 *   }
 * }
 * }</pre>
 *
 * @param <T> the type of the event detail.
 */
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

    /**
     * Create one event for every element in the "eventDetails" list.
     *
     * @param eventDetails A collection of details for the respective events.
     */
    public void addEvents(Collection<T> eventDetails) {
        List<PutEventsRequestEntry> eventRequestEntries =
            eventDetails.stream().map(this::createPutEventRequestEntry).collect(Collectors.toList());
        putEventsRequests = createBatchesOfPutEventsRequests(eventRequestEntries);
    }

    /**
     * This method emits the events in AWS EventBridge.
     *
     * @return The failed requests and the respective responses from EventBridge.
     */
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

    private PutEventsRequestEntry createPutEventRequestEntry(T eventDetail) {

        return PutEventsRequestEntry.builder()
                   .eventBusName(ApplicationConstants.EVENT_BUS_NAME)
                   .resources(invokingFunctionArn)
                   .detailType(detailType)
                   .time(Instant.now())
                   .detail(eventDetail.toJsonString())
                   .source(invokingFunctionArn)
                   .build();
    }

    private List<PutEventsRequest> createBatchesOfPutEventsRequests(List<PutEventsRequestEntry> entries) {
        return ListUtils.partition(entries, BATCH_SIZE)
                   .stream()
                   .map(batch -> PutEventsRequest.builder().entries(batch).build())
                   .collect(Collectors.toList());
    }

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

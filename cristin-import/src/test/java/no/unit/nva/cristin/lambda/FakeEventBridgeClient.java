package no.unit.nva.cristin.lambda;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBus;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

public class FakeEventBridgeClient implements EventBridgeClient {

    private final String eventBusName;
    private List<PutEventsRequest> evenRequests;

    public FakeEventBridgeClient(String eventBusName) {
        this.evenRequests = new ArrayList<>();
        this.eventBusName = eventBusName;
    }

    @Override
    public String serviceName() {
        return this.getClass().getName();
    }

    @Override
    public void close() {

    }

    @Override
    public ListEventBusesResponse listEventBuses(ListEventBusesRequest listEventBusesRequest)
        throws AwsServiceException, SdkClientException {
        EventBus eventBus = EventBus.builder().name(eventBusName).build();
        return ListEventBusesResponse.builder().eventBuses(eventBus).build();
    }

    @Override
    public PutEventsResponse putEvents(PutEventsRequest putEventsRequest)
        throws AwsServiceException, SdkClientException {
        this.evenRequests.add(putEventsRequest);
        List<PutEventsResultEntry> resultEntries = createResultEntries(putEventsRequest);
        return PutEventsResponse.builder().entries(resultEntries).failedEntryCount(numberOfFailures()).build();
    }

    public List<String> listEmittedFilenames() {
        return evenRequests.stream().flatMap(events -> events.entries().stream())
                   .map(PutEventsRequestEntry::detail)
                   .map(FilenameEvent::fromJson)
                   .map(FilenameEvent::getFileUri)
                   .map(URI::toString)
                   .collect(Collectors.toList());
    }

    protected Integer numberOfFailures() {
        return 0;
    }

    private List<PutEventsResultEntry> createResultEntries(PutEventsRequest putEventsRequest) {
        return putEventsRequest.entries()
                   .stream()
                   .map(this::toResponse)
                   .collect(Collectors.toList());
    }

    private PutEventsResultEntry toResponse(PutEventsRequestEntry request) {
        FilenameEvent event = FilenameEvent.fromJson(request.detail());
        return PutEventsResultEntry.builder()
                   .eventId(event.getFileUri().toString())
                   .build();
    }
}

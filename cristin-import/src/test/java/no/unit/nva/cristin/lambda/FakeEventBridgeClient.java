package no.unit.nva.cristin.lambda;

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

    private final List<PutEventsRequest> evenRequests;
    private final String eventBusName;

    public FakeEventBridgeClient(String eventBusName) {
        this.evenRequests = new ArrayList<>();
        this.eventBusName = eventBusName;
    }

    public List<PutEventsRequest> getEvenRequests() {
        return evenRequests;
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
                   .map(ImportRequest::fromJson)
                   .map(ImportRequest::getS3Location)
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
        return PutEventsResultEntry.builder().build();
    }
}

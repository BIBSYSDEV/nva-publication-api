package no.unit.nva.cristin.lambda;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

public class FakeEventBridgeClient implements EventBridgeClient {

    private List<PutEventsRequest> evenRequests;

    public FakeEventBridgeClient() {
        this.evenRequests = new ArrayList<>();
    }

    @Override
    public String serviceName() {
        return this.getClass().getName();
    }

    @Override
    public void close() {

    }

    public List<PutEventsRequest> getEventRequests() {
        return evenRequests;
    }

    public List<String> listEmittedFilenames() {
        return evenRequests.stream().flatMap(events -> events.entries().stream())
                   .map(PutEventsRequestEntry::detail)
                   .map(FilenameEvent::fromJson)
                   .map(FilenameEvent::getFileUri)
                   .map(URI::toString)
                   .collect(Collectors.toList());
    }

    @Override
    public PutEventsResponse putEvents(PutEventsRequest putEventsRequest)
        throws AwsServiceException, SdkClientException {
        this.evenRequests.add(putEventsRequest);
        List<PutEventsResultEntry> resultEntries = createResultEntries(putEventsRequest);
        return PutEventsResponse.builder().entries(resultEntries).failedEntryCount(numberOfFailures()).build();
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

package no.unit.nva.cristin.lambda;

import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class PutEventsResult {

    private final PutEventsRequest request;
    private final PutEventsResponse response;

    public PutEventsResult(PutEventsRequest request, PutEventsResponse result) {
        this.request = request;
        this.response = result;
    }

    @JacocoGenerated
    public PutEventsRequest getRequest() {
        return request;
    }

    @JacocoGenerated
    public PutEventsResponse getResponse() {
        return response;
    }

    public boolean hasFailures() {
        return response.failedEntryCount() > 0;
    }
}

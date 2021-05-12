package no.unit.nva.publication.s3imports;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class PutEventsResult {

    public static final String TO_STRING_TEMPLATE = "Request:%s\nResponse:%s";
    public static final String EMPTY_STRING = "";
    @JsonProperty("request")
    private final PutEventsRequest request;
    @JsonProperty("response")
    private final PutEventsResponse response;

    @JsonCreator
    public PutEventsResult(@JsonProperty("request") PutEventsRequest request,
                           @JsonProperty("response") PutEventsResponse response) {
        this.request = request;
        this.response = response;
    }

    public static String toString(List<PutEventsResult> events) {
        return events.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
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

    /**
     * Override toString as a workaround for the the fact at {@link PutEventsRequest} and {@link PutEventsResult} are
     * not serializable as Json objects with Jackson, but they contain all information in their {@code toString()}
     * representations.
     *
     * @return a String representation of the object.
     */
    @Override
    public String toString() {
        String requestString = nonNull(getRequest()) ? getRequest().toString() : EMPTY_STRING;
        String responseString = nonNull(getResponse()) ? getResponse().toString() : EMPTY_STRING;
        return String.format(TO_STRING_TEMPLATE, requestString, responseString);
    }
}

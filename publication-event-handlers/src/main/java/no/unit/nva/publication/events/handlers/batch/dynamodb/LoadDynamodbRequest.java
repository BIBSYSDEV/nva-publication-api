package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Collections.emptyList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.storage.KeyField;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class LoadDynamodbRequest implements JsonSerializable {

    private static final String JOB_TYPE = "jobType";
    private static final String START_MARKER = "startMarker";
    public static final String TYPE = "types";
    private static final String SEGMENT = "segment";
    private static final String TOTAL_SEGMENTS = "totalSegments";

    @JsonProperty(JOB_TYPE)
    private final String jobType;

    @JsonProperty(START_MARKER)
    private final Map<String, AttributeValue> startMarker;

    @JsonProperty(TYPE)
    private final List<KeyField> types;

    @JsonProperty(SEGMENT)
    private final Integer segment;

    @JsonProperty(TOTAL_SEGMENTS)
    private final Integer totalSegments;

    @JsonCreator
    public LoadDynamodbRequest(@JsonProperty(JOB_TYPE) String jobType,
                               @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker,
                               @JsonProperty(TYPE) List<KeyField> types,
                               @JsonProperty(SEGMENT) Integer segment,
                               @JsonProperty(TOTAL_SEGMENTS) Integer totalSegments) {
        this.jobType = jobType;
        this.startMarker = startMarker;
        this.types = types;
        this.segment = segment;
        this.totalSegments = totalSegments;
    }

    public LoadDynamodbRequest(String jobType) {
        this(jobType, null, emptyList(), null, null);
    }

    public String getJobType() {
        return jobType;
    }

    public Map<String, AttributeValue> getStartMarker() {
        return startMarker;
    }

    public Integer getSegment() {
        return segment;
    }

    public Integer getTotalSegments() {
        return totalSegments;
    }

    public boolean isSegmentedScan() {
        return segment != null && totalSegments != null;
    }

    public LoadDynamodbRequest withStartMarker(Map<String, AttributeValue> newStartMarker) {
        return new LoadDynamodbRequest(this.jobType, newStartMarker, this.types, this.segment, this.totalSegments);
    }

    public PutEventsRequestEntry createNewEventEntry(String eventBusName,
                                                     String detailType,
                                                     String invokedFunctionArn) {
        return PutEventsRequestEntry.builder()
            .eventBusName(eventBusName)
            .detail(this.toJsonString())
            .detailType(detailType)
            .resources(invokedFunctionArn)
            .time(Instant.now())
            .source(invokedFunctionArn)
            .build();
    }

    public List<KeyField> getTypes() {
        return types;
    }
}
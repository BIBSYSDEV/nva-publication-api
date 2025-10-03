package no.unit.nva.publication.events.handlers.batch.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class LoadDynamodbRequest implements JsonSerializable {

    private static final String JOB_TYPE = "jobType";
    private static final String START_MARKER = "startMarker";
    
    @JsonProperty(JOB_TYPE)
    private final String jobType;
    
    @JsonProperty(START_MARKER)
    private final Map<String, AttributeValue> startMarker;

    @JsonCreator
    public LoadDynamodbRequest(@JsonProperty(JOB_TYPE) String jobType,
                               @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker) {
        this.jobType = jobType;
        this.startMarker = startMarker;
    }

    public LoadDynamodbRequest(String jobType) {
        this(jobType, null);
    }

    public String getJobType() {
        return jobType;
    }

    public Map<String, AttributeValue> getStartMarker() {
        return startMarker;
    }

    public LoadDynamodbRequest withStartMarker(Map<String, AttributeValue> newStartMarker) {
        return new LoadDynamodbRequest(this.jobType, newStartMarker);
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
}
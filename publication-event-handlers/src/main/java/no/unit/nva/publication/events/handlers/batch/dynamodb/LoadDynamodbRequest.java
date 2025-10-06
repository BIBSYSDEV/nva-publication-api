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

    @JsonProperty(JOB_TYPE)
    private final String jobType;
    
    @JsonProperty(START_MARKER)
    private final Map<String, AttributeValue> startMarker;

    @JsonProperty(TYPE)
    private final List<KeyField> types;



    @JsonCreator
    public LoadDynamodbRequest(@JsonProperty(JOB_TYPE) String jobType,
                               @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker,
                               @JsonProperty(TYPE) List<KeyField> types) {
        this.jobType = jobType;
        this.startMarker = startMarker;
        this.types = types;
    }

    public LoadDynamodbRequest(String jobType) {
        this(jobType, null, emptyList());
    }

    public String getJobType() {
        return jobType;
    }

    public Map<String, AttributeValue> getStartMarker() {
        return startMarker;
    }

    public LoadDynamodbRequest withStartMarker(Map<String, AttributeValue> newStartMarker) {
        return new LoadDynamodbRequest(this.jobType, newStartMarker, this.types);
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
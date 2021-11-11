package no.unit.nva.publication.events.bodies;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.Map;
import nva.commons.core.JsonSerializable;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class ScanDatabaseRequest implements JsonSerializable {

    public static final String START_MARKER = "startMarker";
    public static final String PAGE_SIZE = "pageSize";
    @JsonProperty(START_MARKER)
    private final Map<String, AttributeValue> startMarker;
    @JsonProperty(PAGE_SIZE)
    private final int pageSize;

    @JsonCreator
    public ScanDatabaseRequest(@JsonProperty(PAGE_SIZE) int pageSize,
                               @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker) {
        this.pageSize = pageSize;
        this.startMarker = startMarker;
    }

    public static ScanDatabaseRequest fromJson(String detail) throws JsonProcessingException {
        return dynamoImageSerializerRemovingEmptyFields.readValue(detail, ScanDatabaseRequest.class);
    }

    public int getPageSize() {
        return pageSize;
    }

    public Map<String, AttributeValue> getStartMarker() {
        return startMarker;
    }

    public ScanDatabaseRequest newScanDatabaseRequest(Map<String, AttributeValue> newStartMarker) {
        return new ScanDatabaseRequest(this.getPageSize(), newStartMarker);
    }

    public PutEventsRequestEntry createNewEventEntry(
        String eventBusName,
        String detailType,
        String invokedFunctionArn
    ) {
        return PutEventsRequestEntry
            .builder()
            .eventBusName(eventBusName)
            .detail(this.toJsonString())
            .detailType(detailType)
            .resources(invokedFunctionArn)
            .time(Instant.now())
            .source(invokedFunctionArn)
            .build();
    }
}

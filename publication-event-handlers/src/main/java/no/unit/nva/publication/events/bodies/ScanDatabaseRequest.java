package no.unit.nva.publication.events.bodies;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class ScanDatabaseRequest implements JsonSerializable {

    public static final String START_MARKER = "startMarker";
    public static final String PAGE_SIZE = "pageSize";
    public static final int DEFAULT_PAGE_SIZE = 700; // Choosing for safety 3/4 of max page size.
    public static final int MAX_PAGE_SIZE = 1000;
    public static final String TOPIC = "topic";
    @JsonProperty(START_MARKER)
    private final Map<String, AttributeValue> startMarker;
    @JsonProperty(PAGE_SIZE)
    private final int pageSize;
    @JsonProperty(TOPIC)
    private String topic;

    @JsonCreator
    public ScanDatabaseRequest(@JsonProperty(PAGE_SIZE) int pageSize,
                               @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker,
                               @JsonProperty(TOPIC) String topic) {
        this.pageSize = pageSize;
        this.startMarker = startMarker;
        this.topic = topic;
    }

    public static ScanDatabaseRequest fromJson(String detail) throws JsonProcessingException {
        return objectMapper.readValue(detail, ScanDatabaseRequest.class);
    }

    @JsonProperty(TOPIC)
    public String getTopic() {
        return topic;
    }

    public int getPageSize() {
        return pageSizeWithinLimits(pageSize)
                   ? pageSize
                   : DEFAULT_PAGE_SIZE;
    }

    public Map<String, AttributeValue> getStartMarker() {
        return startMarker;
    }

    public ScanDatabaseRequest newScanDatabaseRequest(Map<String, AttributeValue> newStartMarker) {
        return new ScanDatabaseRequest(this.getPageSize(), newStartMarker, topic);
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

    public void setTopic(String topic) {
        this.topic = topic;
    }

    private boolean pageSizeWithinLimits(int pageSize) {
        return pageSize > 0 && pageSize <= MAX_PAGE_SIZE;
    }
}
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
    public static final String TYPE = "type";
    @JsonProperty(START_MARKER)
    private final Map<String, AttributeValue> startMarker;
    @JsonProperty(PAGE_SIZE)
    private final int pageSize;
    @JsonProperty(TYPE)
    private String type;
    @JsonProperty(TOPIC)
    private String topic;

    @JsonCreator
    public ScanDatabaseRequest(@JsonProperty(PAGE_SIZE) int pageSize,
                               @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker,
                               @JsonProperty(TOPIC) String topic,
                               @JsonProperty(TYPE) String type) {
        this.pageSize = pageSize;
        this.startMarker = startMarker;
        this.topic = topic;
        this.type = type;
    }

    public static ScanDatabaseRequest fromJson(String detail) throws JsonProcessingException {
        return objectMapper.readValue(detail, ScanDatabaseRequest.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty(TOPIC)
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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
        return new ScanDatabaseRequest(this.getPageSize(), newStartMarker, topic, type);
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

    private boolean pageSizeWithinLimits(int pageSize) {
        return pageSize > 0 && pageSize <= MAX_PAGE_SIZE;
    }

    public static final class Builder {

        private Map<String, AttributeValue> startMarker;
        private int pageSize;
        private String type;
        private String topic;

        private Builder() {
        }

        public Builder withStartMarker(Map<String, AttributeValue> startMarker) {
            this.startMarker = startMarker;
            return this;
        }

        public Builder withPageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public ScanDatabaseRequest build() {
            return new ScanDatabaseRequest(pageSize, startMarker, topic, type);
        }
    }
}
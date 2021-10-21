package no.unit.nva.publication.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class EventPayload {

    public static final String EVENT_TYPE = "eventType";
    public static final String PAYLOAD = "payload";
    @JsonProperty(EVENT_TYPE)
    private final String eventType;
    @JsonProperty(PAYLOAD)
    private final URI payload;

    @JsonCreator
    public EventPayload(@JsonProperty(EVENT_TYPE) String eventType,
                        @JsonProperty(PAYLOAD) URI payload) {
        this.payload = payload;
        this.eventType = eventType;
    }

    public String getEventType() {
        return eventType;
    }

    public URI getPayload() {
        return payload;
    }
}

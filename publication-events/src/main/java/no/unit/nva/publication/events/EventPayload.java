package no.unit.nva.publication.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type", defaultImpl = EventPayload.class)
public class EventPayload {

    public static final String EVENT_TYPE = "eventType";
    public static final String PAYLOAD = "uri";
    private static final String RESOURCES_ENTRY_UPDATE = "resources.entry.update";
    private static final String INDEX_ENTRY_UPDATE = "index.entry.update";
    private static final String EMPTY_EVENT_TYPE = "event.empty";
    @JsonProperty(EVENT_TYPE)
    private final String eventType;
    @JsonProperty(PAYLOAD)
    private final URI payloadUri;

    @JacocoGenerated
    @JsonCreator
    public EventPayload(@JsonProperty(EVENT_TYPE) String eventType,
                        @JsonProperty(PAYLOAD) URI payloadUri) {
        this.payloadUri = payloadUri;
        this.eventType = eventType;
    }

    @JacocoGenerated
    public static EventPayload emptyEvent() {
        return new EventPayload(EMPTY_EVENT_TYPE, null);
    }

    @JacocoGenerated
    public static EventPayload resourcesUpdateEvent(URI payloadUri) {
        return new EventPayload(RESOURCES_ENTRY_UPDATE, payloadUri);
    }

    @JacocoGenerated
    public static EventPayload indexEntryEvent(URI payloadUri) {
        return new EventPayload(INDEX_ENTRY_UPDATE, payloadUri);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getEventType(), getPayloadUri());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof EventPayload)) {
            return false;
        }
        EventPayload that = (EventPayload) o;
        return Objects.equals(getEventType(), that.getEventType()) && Objects.equals(getPayloadUri(),
                                                                                     that.getPayloadUri());
    }

    @JacocoGenerated
    public String getEventType() {
        return eventType;
    }

    @JacocoGenerated
    public URI getPayloadUri() {
        return payloadUri;
    }
}

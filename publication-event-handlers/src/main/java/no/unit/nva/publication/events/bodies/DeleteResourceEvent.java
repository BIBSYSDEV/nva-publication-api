package no.unit.nva.publication.events.bodies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JacocoGenerated;

public class DeleteResourceEvent {

    public static final String EVENT_TOPIC = "PublicationService.ExpandedEntry.Deleted";

    private final String topic;
    private final SortableIdentifier identifier;

    @JsonCreator
    public DeleteResourceEvent(
        @JsonProperty("topic") String topic,
        @JsonProperty("identifier") SortableIdentifier identifier) {
        this.topic = topic;
        this.identifier = identifier;
    }

    public String getTopic() {
        return topic;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(topic, identifier);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeleteResourceEvent that = (DeleteResourceEvent) o;
        return topic.equals(that.topic)
               && identifier.equals(that.identifier);
    }
}

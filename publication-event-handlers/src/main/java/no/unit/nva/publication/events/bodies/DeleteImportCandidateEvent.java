package no.unit.nva.publication.events.bodies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JacocoGenerated;

/**
 * Event is emitted by DataEntryUpdateHandler. Event is consumed by DeleteImportCandidateEventHandler.
 */

public class DeleteImportCandidateEvent implements JsonSerializable {

    public static final String EVENT_TOPIC = "ImportCandidates.ExpandedEntry.Deleted";
    public static final String TOPIC = "topic";
    public static final String IDENTIFIER = "identifier";
    private final String topic;
    private final SortableIdentifier identifier;

    @JsonCreator
    public DeleteImportCandidateEvent(@JsonProperty(TOPIC) String topic,
                                      @JsonProperty(IDENTIFIER) SortableIdentifier identifier) {
        this.topic = topic;
        this.identifier = identifier;
    }

    @JacocoGenerated
    public String getTopic() {
        return topic;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getIdentifier());
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
        DeleteImportCandidateEvent that = (DeleteImportCandidateEvent) o;
        return topic.equals(that.topic)
               && identifier.equals(that.identifier);
    }
}


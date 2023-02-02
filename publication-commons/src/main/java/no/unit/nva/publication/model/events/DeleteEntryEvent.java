package no.unit.nva.publication.model.events;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JacocoGenerated;

//Event emitted from button lambda DeleteEntriesEventEmitter and consumed by DeletePublicationHandler
public class DeleteEntryEvent implements JsonSerializable {

    @JsonIgnore
    public static final String EVENT_TOPIC = "DeleteEntriesEvent.FileEntry.Delete";

    @JsonProperty("identifier")
    private final SortableIdentifier identifier;

    @JsonProperty("topic")
    private final String topic;

    public DeleteEntryEvent(@JsonProperty("topic") String topic,
                            @JsonProperty("identifier") SortableIdentifier identifier) {
        this.topic = topic;
        this.identifier = identifier;
    }

    public static DeleteEntryEvent fromJson(String json) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, DeleteEntryEvent.class)).orElseThrow();
    }

    @JacocoGenerated
    public String getTopic() {
        return topic;
    }

    @JacocoGenerated
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(identifier, topic);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeleteEntryEvent)) {
            return false;
        }
        DeleteEntryEvent that = (DeleteEntryEvent) o;
        return Objects.equals(identifier, that.identifier)
               && Objects.equals(topic, that.topic);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }
}

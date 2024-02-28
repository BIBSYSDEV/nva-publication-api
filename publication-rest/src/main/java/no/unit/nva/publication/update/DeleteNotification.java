package no.unit.nva.publication.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JacocoGenerated;

public class DeleteNotification implements JsonSerializable {

    @JsonProperty("topic")
    private final static String TOPIC = "PublicationService.UnpublishedResource.Ticket";

    @JsonProperty("identifier")
    private final SortableIdentifier identifier;

    @JsonCreator
    public DeleteNotification(@JsonProperty("identifier") SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    public String getTopic() {
        return TOPIC;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeleteNotification that = (DeleteNotification) o;
        return Objects.equals(identifier, that.identifier);
    }

    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}

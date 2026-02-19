package no.unit.nva.publication.events.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationStatus;
import nva.commons.core.JacocoGenerated;

public class PublicationEventReference extends EventReference {

    private final SortableIdentifier identifier;
    private final String type;
    private final PublicationStatus status;

    @JsonCreator
    public PublicationEventReference(@JsonProperty("topic") String topic, @JsonProperty("uri") URI uri,
                                     @JsonProperty("identifier") SortableIdentifier identifier,
                                     @JsonProperty("type") String type,
                                     @JsonProperty("status") PublicationStatus status) {
        super(topic, uri);
        this.identifier = identifier;
        this.type = type;
        this.status = status;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PublicationEventReference that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getType(), that.getType())
               && getStatus() == that.getStatus();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getIdentifier(), getType(), getStatus());
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public String getType() {
        return type;
    }

    public PublicationStatus getStatus() {
        return status;
    }
}

package no.unit.nva.publication.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.utils.JacocoGenerated;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

public class DeletePublicationEvent {

    public static final String DELETE_PUBLICATION = "delete.publication";

    private final String type;
    private final UUID identifier;
    private final String status;
    private final URI doi;

    /**
     * Constructor for DeletePublicationEvent.
     *
     * @param type  type
     * @param identifier    identifier
     * @param status    status
     * @param doi   doi
     */
    @JsonCreator
    public DeletePublicationEvent(
            @JsonProperty("type") String type,
            @JsonProperty("identifier") UUID identifier,
            @JsonProperty("status") String status,
            @JsonProperty("doi") URI doi) {
        this.type = type;
        this.identifier = identifier;
        this.status = status;
        this.doi = doi;
    }

    public String getType() {
        return type;
    }

    public UUID getIdentifier() {
        return identifier;
    }

    public String getStatus() {
        return status;
    }

    public URI getDoi() {
        return doi;
    }

    @JsonProperty("hasDoi")
    public boolean hasDoi() {
        return Objects.nonNull(doi);
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
        DeletePublicationEvent that = (DeletePublicationEvent) o;
        return type.equals(that.type)
                && identifier.equals(that.identifier)
                && status.equals(that.status)
                && Objects.equals(doi, that.doi);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(type, identifier, status, doi);
    }
}

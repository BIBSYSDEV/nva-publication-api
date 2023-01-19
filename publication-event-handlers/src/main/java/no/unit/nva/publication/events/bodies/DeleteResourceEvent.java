package no.unit.nva.publication.events.bodies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JacocoGenerated;

public class DeleteResourceEvent {

    public static final String EVENT_TOPIC = "PublicationService.ExpandedEntry.Deleted";

    private final String topic;
    private final SortableIdentifier identifier;
    private final String status;
    private final URI doi;
    private final URI customerId;

    @JsonCreator
    public DeleteResourceEvent(
        @JsonProperty("topic") String topic,
        @JsonProperty("identifier") SortableIdentifier identifier,
        @JsonProperty("status") String status,
        @JsonProperty("doi") URI doi,
        @JsonProperty("customerId") URI customerId) {
        this.topic = topic;
        this.identifier = identifier;
        this.status = status;
        this.doi = doi;
        this.customerId = customerId;
    }

    public String getTopic() {
        return topic;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public String getStatus() {
        return status;
    }

    public URI getDoi() {
        return doi;
    }

    public URI getCustomerId() {
        return customerId;
    }

    @JsonProperty("hasDoi")
    public boolean hasDoi() {
        return Objects.nonNull(doi);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(topic, identifier, status, doi, customerId);
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
               && identifier.equals(that.identifier)
               && status.equals(that.status)
               && Objects.equals(doi, that.doi)
               && Objects.equals(customerId, that.customerId);
    }
}

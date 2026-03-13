package no.unit.nva.publication.events.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.core.JacocoGenerated;

public class PublicationEventReference extends EventReference {

    private final SortableIdentifier identifier;
    private final String oldType;
    private final String newType;
    private final PublicationStatus oldStatus;
    private final PublicationStatus newStatus;

    @JsonCreator
    public PublicationEventReference(@JsonProperty("topic") String topic, @JsonProperty("uri") URI uri,
                                     @JsonProperty("identifier") SortableIdentifier identifier,
                                     @JsonProperty("oldType") String oldType, @JsonProperty("newType") String newType,
                                     @JsonProperty("oldStatus") PublicationStatus oldStatus,
                                     @JsonProperty("newStatus") PublicationStatus newStatus) {
        super(topic, uri);
        this.identifier = identifier;
        this.oldType = oldType;
        this.newType = newType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public static PublicationEventReference create(String topic, URI uri, DataEntryUpdateEvent event) {
        if (!(event.extractDataEntryType() instanceof Resource)) {
            throw new RuntimeException(
                "Can not construct PublicationEventReference for other Entity than Publication!");
        }
        return new PublicationEventReference(topic, uri, getIdentifier(event), getOldType(event), getNewType(event),
                                             getOldStatus(event), getNewStatus(event));
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
               && Objects.equals(getOldType(), that.getOldType())
               && Objects.equals(getNewType(), that.getNewType())
               && getOldStatus() == that.getOldStatus()
               && getNewStatus() == that.getNewStatus();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getIdentifier(), getOldType(), getNewType(), getOldStatus(),
                            getNewStatus());
    }

    public String getNewType() {
        return newType;
    }

    public PublicationStatus getNewStatus() {
        return newStatus;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public String getOldType() {
        return oldType;
    }

    public PublicationStatus getOldStatus() {
        return oldStatus;
    }

    private static SortableIdentifier getIdentifier(DataEntryUpdateEvent event) {
        var oldImage = Optional.ofNullable(event).map(DataEntryUpdateEvent::getOldData).map(Resource.class::cast);
        var newImage = Optional.ofNullable(event).map(DataEntryUpdateEvent::getNewData).map(Resource.class::cast);
        return oldImage.map(Resource::getIdentifier).orElse(newImage.map(Resource::getIdentifier).orElse(null));
    }

    private static String getOldType(DataEntryUpdateEvent event) {
        return Optional.ofNullable(event)
                   .map(DataEntryUpdateEvent::getOldData)
                   .map(Resource.class::cast)
                   .flatMap(Resource::getInstanceType)
                   .orElse(null);
    }

    private static String getNewType(DataEntryUpdateEvent event) {
        return Optional.ofNullable(event)
                   .map(DataEntryUpdateEvent::getNewData)
                   .map(Resource.class::cast)
                   .flatMap(Resource::getInstanceType)
                   .orElse(null);
    }

    private static PublicationStatus getOldStatus(DataEntryUpdateEvent event) {
        return Optional.ofNullable(event)
                   .map(DataEntryUpdateEvent::getOldData)
                   .map(Resource.class::cast)
                   .map(Resource::getStatus)
                   .orElse(null);
    }

    private static PublicationStatus getNewStatus(DataEntryUpdateEvent event) {
        return Optional.ofNullable(event)
                   .map(DataEntryUpdateEvent::getNewData)
                   .map(Resource.class::cast)
                   .map(Resource::getStatus)
                   .orElse(null);
    }
}

package no.unit.nva.publication.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

public class PublicationUpdateEvent {

    public static final String PUBLICATION_UPDATE_TYPE = "publication.update";

    private final String type;
    private final String updateType;
    private final Publication oldPublication;
    private final Publication newPublication;

    /**
     * Constructor for creating PublicationUpdateEvent.
     *
     * @param type  type
     * @param updateType    eventName from DynamodbStreamRecord
     * @param oldPublication    old Publication
     * @param newPublication    new Publication
     */
    @JsonCreator
    public PublicationUpdateEvent(
            @JsonProperty("type") String type,
            @JsonProperty("updateType") String updateType,
            @JsonProperty("oldPublication") Publication oldPublication,
            @JsonProperty("newPublication") Publication newPublication) {
        this.type = type;
        this.updateType = updateType;
        this.oldPublication = oldPublication;
        this.newPublication = newPublication;
    }

    public String getType() {
        return type;
    }

    public String getUpdateType() {
        return updateType;
    }

    public Publication getOldPublication() {
        return oldPublication;
    }

    public Publication getNewPublication() {
        return newPublication;
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
        PublicationUpdateEvent that = (PublicationUpdateEvent) o;
        return getType().equals(that.getType())
                && getUpdateType().equals(that.getUpdateType())
                && Objects.equals(getOldPublication(), that.getOldPublication())
                && Objects.equals(getNewPublication(), that.getNewPublication());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getType(), getUpdateType(), getOldPublication(), getNewPublication());
    }
}

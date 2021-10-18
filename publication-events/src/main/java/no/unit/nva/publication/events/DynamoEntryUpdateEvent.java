package no.unit.nva.publication.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class DynamoEntryUpdateEvent {

    public static final String PUBLICATION_UPDATE_TYPE = "publication.update";
    public static final String MESSAGE_UPDATE_TYPE = "message.update";
    public static final String DOI_REQUEST_UPDATE_TYPE = "doirequest.update";

    private final String type;
    private final String updateType;
    private final ResourceUpdate oldData;
    private final ResourceUpdate newData;

    /**
     * Constructor for creating DynamoEntryUpdateEvent.
     *
     * @param type           type
     * @param updateType     eventName from DynamodbStreamRecord
     * @param oldData old data
     * @param newData new data
     */
    @JsonCreator
    public DynamoEntryUpdateEvent(
        @JsonProperty("type") String type,
        @JsonProperty("updateType") String updateType,
        @JsonProperty("oldData") ResourceUpdate oldData,
        @JsonProperty("newData") ResourceUpdate newData) {
        this.type = type;
        this.updateType = updateType;
        this.oldData = oldData;
        this.newData = newData;
    }

    public String getType() {
        return type;
    }

    public String getUpdateType() {
        return updateType;
    }

    public ResourceUpdate getOldData() {
        return oldData;
    }

    public ResourceUpdate getNewData() {
        return newData;
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
        DynamoEntryUpdateEvent that = (DynamoEntryUpdateEvent) o;
        return getType().equals(that.getType())
                && getUpdateType().equals(that.getUpdateType())
                && Objects.equals(getOldData(), that.getOldData())
                && Objects.equals(getNewData(), that.getNewData());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getType(), getUpdateType(), getOldData(), getNewData());
    }
}

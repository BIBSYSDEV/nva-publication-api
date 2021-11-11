package no.unit.nva.publication.events.bodies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class DynamoEntryUpdateEvent {

    public static final String PUBLICATION_UPDATE_TYPE = "publication.update";
    public static final String MESSAGE_UPDATE_TYPE = "message.update";
    public static final String DOI_REQUEST_UPDATE_TYPE = "doirequest.update";

    private static final String UPDATE_TYPE = "updateType";
    private static final String OLD_DATA = "oldData";
    private static final String NEW_DATA = "newData";

    @JsonProperty(UPDATE_TYPE)
    private final String updateType;
    @JsonProperty(OLD_DATA)
    private final ResourceUpdate oldData;
    @JsonProperty(NEW_DATA)
    private final ResourceUpdate newData;

    /**
     * Constructor for creating DynamoEntryUpdateEvent.
     *
     * @param updateType     eventName from DynamodbStreamRecord
     * @param oldData old data
     * @param newData new data
     */
    @JsonCreator
    public DynamoEntryUpdateEvent(
            @JsonProperty(UPDATE_TYPE) String updateType,
            @JsonProperty(OLD_DATA) ResourceUpdate oldData,
            @JsonProperty(NEW_DATA) ResourceUpdate newData) {

        this.updateType = updateType;
        this.oldData = oldData;
        this.newData = newData;
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


    @JsonProperty("type")
    private String getType() {
        String eventType = null;
        if (oldData instanceof Resource || newData instanceof Resource) {
            eventType = DynamoEntryUpdateEvent.PUBLICATION_UPDATE_TYPE;
        }
        if (oldData instanceof Message || newData instanceof Message) {
            eventType = DynamoEntryUpdateEvent.MESSAGE_UPDATE_TYPE;
        }
        if (oldData instanceof DoiRequest || newData instanceof DoiRequest) {
            eventType = DynamoEntryUpdateEvent.DOI_REQUEST_UPDATE_TYPE;
        }
        return eventType;
    }
}

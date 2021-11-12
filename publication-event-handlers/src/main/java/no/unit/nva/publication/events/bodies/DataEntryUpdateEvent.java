package no.unit.nva.publication.events.bodies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.DataEntry;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class DataEntryUpdateEvent {

    public static final String RESOURCE_UPDATE_EVENT_TOPIC = "PublicationService.Resource.Update";
    public static final String MESSAGE_UPDATE_EVENT_TOPIC = "PublicationService.Message.Update";
    public static final String DOI_REQUEST_UPDATE_EVENT_TOPIC = "PublicationService.DoiRequest.Update";

    private static final String UPDATE_TYPE = "updateType";
    private static final String OLD_DATA = "oldData";
    private static final String NEW_DATA = "newData";

    @JsonProperty(UPDATE_TYPE)
    private final String updateType;
    @JsonProperty(OLD_DATA)
    private final DataEntry oldData;
    @JsonProperty(NEW_DATA)
    private final DataEntry newData;

    /**
     * Constructor for creating DynamoEntryUpdateEvent.
     *
     * @param updateType     eventName from DynamodbStreamRecord
     * @param oldData old data
     * @param newData new data
     */
    @JsonCreator
    public DataEntryUpdateEvent(
            @JsonProperty(UPDATE_TYPE) String updateType,
            @JsonProperty(OLD_DATA) DataEntry oldData,
            @JsonProperty(NEW_DATA) DataEntry newData) {

        this.updateType = updateType;
        this.oldData = oldData;
        this.newData = newData;
    }

    public String getUpdateType() {
        return updateType;
    }

    public DataEntry getOldData() {
        return oldData;
    }

    public DataEntry getNewData() {
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
        DataEntryUpdateEvent that = (DataEntryUpdateEvent) o;
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
            eventType = DataEntryUpdateEvent.RESOURCE_UPDATE_EVENT_TOPIC;
        }
        if (oldData instanceof Message || newData instanceof Message) {
            eventType = DataEntryUpdateEvent.MESSAGE_UPDATE_EVENT_TOPIC;
        }
        if (oldData instanceof DoiRequest || newData instanceof DoiRequest) {
            eventType = DataEntryUpdateEvent.DOI_REQUEST_UPDATE_EVENT_TOPIC;
        }
        return eventType;
    }
}

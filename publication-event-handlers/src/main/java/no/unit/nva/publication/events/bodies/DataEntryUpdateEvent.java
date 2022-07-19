package no.unit.nva.publication.events.bodies;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.DataEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.core.JacocoGenerated;

public class DataEntryUpdateEvent implements JsonSerializable {
    
    public static final String RESOURCE_UPDATE_EVENT_TOPIC = "PublicationService.Resource.Update";
    public static final String MESSAGE_UPDATE_EVENT_TOPIC = "PublicationService.Message.Update";
    private static final String DOI_REQUEST_UPDATE_EVENT_TOPIC = "PublicationService.DoiRequest.Update";
    private static final String PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC = "PublicationService.PublishingRequest.Update";
    private static final String ACTION = "action";
    private static final String OLD_DATA = "oldData";
    private static final String NEW_DATA = "newData";
    
    private static final Map<Class<?>, String> ENTRY_TYPE_TO_TOPIC_MAP = Map.of(
        Resource.class, RESOURCE_UPDATE_EVENT_TOPIC,
        DoiRequest.class, DOI_REQUEST_UPDATE_EVENT_TOPIC,
        PublishingRequestCase.class, PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC,
        Message.class, MESSAGE_UPDATE_EVENT_TOPIC
    );
    @JsonProperty(ACTION)
    private final String action;
    @JsonProperty(OLD_DATA)
    private final DataEntry oldData;
    @JsonProperty(NEW_DATA)
    private final DataEntry newData;
    
    /**
     * Constructor for creating DynamoEntryUpdateEvent.
     *
     * @param action  eventName from DynamodbStreamRecord
     * @param oldData old data
     * @param newData new data
     */
    @JsonCreator
    public DataEntryUpdateEvent(
        @JsonProperty(ACTION) String action,
        @JsonProperty(OLD_DATA) DataEntry oldData,
        @JsonProperty(NEW_DATA) DataEntry newData) {
        
        this.action = action;
        this.oldData = oldData;
        this.newData = newData;
    }
    
    public static DataEntryUpdateEvent fromJson(String json) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, DataEntryUpdateEvent.class)).orElseThrow();
    }
    
    public String getAction() {
        return action;
    }
    
    public DataEntry getOldData() {
        return oldData;
    }
    
    public DataEntry getNewData() {
        return newData;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getTopic(), getTopic(), getOldData(), getNewData());
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
        return getAction().equals(that.getAction())
               && getTopic().equals(that.getTopic())
               && Objects.equals(getOldData(), that.getOldData())
               && Objects.equals(getNewData(), that.getNewData());
    }
    
    @JsonIgnore
    public boolean notEmpty() {
        return nonNull(oldData) || nonNull(newData);
    }
    
    @JsonProperty("topic")
    public String getTopic() {
        var type = extractDataEntryType();
        return ENTRY_TYPE_TO_TOPIC_MAP.get(type);
    }
    
    private Class<? extends DataEntry> extractDataEntryType() {
        return nonNull(newData) ? newData.getClass() : oldData.getClass();
    }
}

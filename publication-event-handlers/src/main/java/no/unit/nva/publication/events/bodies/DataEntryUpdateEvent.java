package no.unit.nva.publication.events.bodies;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.publicationstate.FileDeletedEvent;
import nva.commons.core.JacocoGenerated;

public class DataEntryUpdateEvent implements JsonSerializable {
    
    public static final String RESOURCE_UPDATE_EVENT_TOPIC = "PublicationService.Resource.Update";
    public static final String MESSAGE_UPDATE_EVENT_TOPIC = "PublicationService.Message.Update";
    public static final String PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC = "PublicationService.PublishingRequest.Update";
    public static final String GENERAL_SUPPORT_REQUEST_UPDATE_EVENT_TOPIC =
        "PublicationService.GeneralSupportRequest.Update";
    private static final String DOI_REQUEST_UPDATE_EVENT_TOPIC = "PublicationService.DoiRequest.Update";
    private static final String UNPUBLISH_REQUEST_UPDATE_EVENT_TOPIC = "PublicationService.UnpublishRequest.Update";
    public static final String FILE_ENTRY_UPDATE_EVENT_TOPIC = "PublicationService.FileEntry.Update";
    public static final String FILE_ENTRY_DELETE_EVENT_TOPIC = "PublicationService.FileEntry.Delete";
    private static final String ACTION = "action";
    private static final String OLD_DATA = "oldData";
    private static final String NEW_DATA = "newData";

    @JsonProperty(ACTION)
    private final String action;
    @JsonProperty(OLD_DATA)
    private final Entity oldData;
    @JsonProperty(NEW_DATA)
    private final Entity newData;
    
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
        @JsonProperty(OLD_DATA) Entity oldData,
        @JsonProperty(NEW_DATA) Entity newData) {
        
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
    
    public Entity getOldData() {
        return oldData;
    }
    
    public Entity getNewData() {
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
    public boolean shouldProcessUpdate() {
        if (extractDataEntryType() instanceof FileEntry) {
            return hasNewImage();
        } else {
            return nonNull(oldData) || nonNull(newData);
        }
    }
    
    @JsonProperty("topic")
    public String getTopic() {
        var type = extractDataEntryType();
        return switch (type) {
            case Resource resource -> RESOURCE_UPDATE_EVENT_TOPIC;
            case DoiRequest doiRequest -> DOI_REQUEST_UPDATE_EVENT_TOPIC;
            case PublishingRequestCase publishingRequestCase -> PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC;
            case Message message -> MESSAGE_UPDATE_EVENT_TOPIC;
            case GeneralSupportRequest generalSupportRequest -> GENERAL_SUPPORT_REQUEST_UPDATE_EVENT_TOPIC;
            case UnpublishRequest unpublishRequest -> UNPUBLISH_REQUEST_UPDATE_EVENT_TOPIC;
            case FileEntry fileEntry when hasNewImage() -> fileEntry.getFileEvent() instanceof FileDeletedEvent
                                            ? FILE_ENTRY_DELETE_EVENT_TOPIC
                                            : FILE_ENTRY_UPDATE_EVENT_TOPIC;
            default -> throw new IllegalArgumentException("Unknown entry type: " + type);
        };
    }

    private Entity extractDataEntryType() {
        return nonNull(newData) ? newData : oldData;
    }

    private boolean hasNewImage() {
        return nonNull(newData);
    }
}

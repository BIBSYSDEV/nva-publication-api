package no.unit.nva.publication.events.bodies;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.model.business.publicationstate.FileDeletedEvent;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class DataEntryUpdateEvent implements JsonSerializable {

    public static final String RESOURCE_DELETED_EVENT_TOPIC = "PublicationService.Resource.Deleted";
    public static final String RESOURCE_UPDATE_EVENT_TOPIC = "PublicationService.Resource.Update";
    public static final String MESSAGE_UPDATE_EVENT_TOPIC = "PublicationService.Message.Update";
    public static final String PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC =
            "PublicationService.PublishingRequest.Update";
    public static final String FILES_APPROVAL_THESIS_UPDATE_EVENT_TOPIC =
            "PublicationService.FilesApprovalThesis.Update";
    public static final String GENERAL_SUPPORT_REQUEST_UPDATE_EVENT_TOPIC =
            "PublicationService.GeneralSupportRequest.Update";
    private static final String DOI_REQUEST_UPDATE_EVENT_TOPIC =
            "PublicationService.DoiRequest.Update";
    private static final String UNPUBLISH_REQUEST_UPDATE_EVENT_TOPIC =
            "PublicationService.UnpublishRequest.Update";
    public static final String FILE_ENTRY_UPDATE_EVENT_TOPIC = "PublicationService.FileEntry.Update";
    public static final String FILE_ENTRY_DELETE_EVENT_TOPIC = "PublicationService.FileEntry.Delete";
    public static final String PUBLICATION_CHANNEL_CONSTRAINT_UPDATED_EVENT_TOPIC =
            "PublicationService.PublicationChannelConstraint.Update";
    private static final String ACTION = "action";
    private static final String OLD_DATA = "oldData";
    private static final String NEW_DATA = "newData";
    protected static final String SHOULD_IGNORE_BATCH_SCAN = "SHOULD_IGNORE_BATCH_SCAN";

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
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, DataEntryUpdateEvent.class))
                .orElseThrow();
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
    public boolean shouldProcessUpdate(Environment environment) {
        if (!isUserUpdate() && shouldIgnoreBatchScan(environment)) {
            return false;
        }
        if (extractDataEntryType() instanceof FileEntry) {
            return hasNewImage();
        } else {
            return nonNull(oldData) || nonNull(newData);
        }
    }

    private static Boolean shouldIgnoreBatchScan(Environment environment) {
        return environment.readEnvOpt(SHOULD_IGNORE_BATCH_SCAN).map(Boolean::valueOf).orElse(true);
    }

    private boolean isUserUpdate() {
        var oldModifiedDate = Optional.ofNullable(oldData).map(Entity::getModifiedDate);
        var newModifiedDate = Optional.ofNullable(newData).map(Entity::getModifiedDate);
        if (oldModifiedDate.isPresent() && newModifiedDate.isPresent()) {
            return !oldModifiedDate.get().equals(newModifiedDate.get());
        } else {
            return true;
        }
    }

    @JsonProperty("topic")
    public String getTopic() {
        var type = extractDataEntryType();
        return switch (type) {
            case Resource resource -> !hasNewImage() && OperationType.REMOVE.equals(OperationType.fromValue(action))
                    ? RESOURCE_DELETED_EVENT_TOPIC
                    : RESOURCE_UPDATE_EVENT_TOPIC;
            case DoiRequest doiRequest -> DOI_REQUEST_UPDATE_EVENT_TOPIC;
            case PublishingRequestCase publishingRequestCase -> PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC;
            case FilesApprovalThesis filesApprovalThesis -> FILES_APPROVAL_THESIS_UPDATE_EVENT_TOPIC;
            case Message message -> MESSAGE_UPDATE_EVENT_TOPIC;
            case GeneralSupportRequest generalSupportRequest -> GENERAL_SUPPORT_REQUEST_UPDATE_EVENT_TOPIC;
            case UnpublishRequest unpublishRequest -> UNPUBLISH_REQUEST_UPDATE_EVENT_TOPIC;
            case FileEntry fileEntry when hasNewImage() -> fileEntry.getFileEvent() instanceof FileDeletedEvent
                    ? FILE_ENTRY_DELETE_EVENT_TOPIC
                    : FILE_ENTRY_UPDATE_EVENT_TOPIC;
            case PublicationChannel publicationChannel -> PUBLICATION_CHANNEL_CONSTRAINT_UPDATED_EVENT_TOPIC;
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

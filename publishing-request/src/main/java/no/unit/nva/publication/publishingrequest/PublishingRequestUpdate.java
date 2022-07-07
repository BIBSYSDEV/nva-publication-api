package no.unit.nva.publication.publishingrequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class PublishingRequestUpdate {

    public static final String NO_CHANGE_REQUESTED_ERROR = "You must request changes to do";
    public static final String PUBLISHING_REQUEST_STATUS = "status";
    public static final String MESSAGE = "message";
    private static final String EMPTY_MESSAGE = null;

    @JsonProperty(PUBLISHING_REQUEST_STATUS)
    private final PublishingRequestStatus publishingRequestStatus;
    @JsonProperty(MESSAGE)
    private final String message;

    @JsonCreator
    public PublishingRequestUpdate(
        @JsonProperty(PUBLISHING_REQUEST_STATUS) PublishingRequestStatus publishingRequestStatus,
        @JsonProperty(MESSAGE) String message) {
        this.publishingRequestStatus = publishingRequestStatus;
        this.message = message;
    }

    public static PublishingRequestUpdate createApproved() {
        return PublishingRequestUpdate.create(PublishingRequestStatus.APPROVED);
    }

    private static PublishingRequestUpdate create(PublishingRequestStatus status) {
        return new PublishingRequestUpdate(status,EMPTY_MESSAGE);
    }

    public PublishingRequestStatus getPublishingRequestStatus() {
        return this.publishingRequestStatus;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPublishingRequestStatus(), getMessage());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestUpdate)) {
            return false;
        }
        PublishingRequestUpdate that = (PublishingRequestUpdate) o;
        return getPublishingRequestStatus() == that.getPublishingRequestStatus() && Objects.equals(getMessage(),
                                                                                                   that.getMessage());
    }

    private BadRequestException noChangeRequested() {
        return new BadRequestException(NO_CHANGE_REQUESTED_ERROR);
    }
}


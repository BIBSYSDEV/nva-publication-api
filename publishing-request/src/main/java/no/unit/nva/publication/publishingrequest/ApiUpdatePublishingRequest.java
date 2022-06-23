package no.unit.nva.publication.publishingrequest;

import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;

import java.util.Optional;

public class ApiUpdatePublishingRequest {

    public static final String NO_CHANGE_REQUESTED_ERROR = "You must request changes to do";
    private PublishingRequestStatus publishingRequestStatus;
    private String message;

    public PublishingRequestStatus getPublishingRequestStatus() {
        return this.publishingRequestStatus;
    }

    public void setPublishingRequestStatus(PublishingRequestStatus status) {
        this.publishingRequestStatus = status;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void validate() throws BadRequestException {
        if (publishingRequestStatus == null) {
            throw noChangeRequested();
        }
    }

    private BadRequestException noChangeRequested() {
        return new BadRequestException(NO_CHANGE_REQUESTED_ERROR);
    }
}


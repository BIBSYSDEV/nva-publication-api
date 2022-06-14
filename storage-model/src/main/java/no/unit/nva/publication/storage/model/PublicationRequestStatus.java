package no.unit.nva.publication.storage.model;

public enum PublicationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static final String INVALID_APPROVE_PUBLICATION_REQUEST_STATUS_ERROR = "Invalid ApprovePublicationRequest status: ";
    public static final String ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S =
            "Not allowed to change status from %s to %s";

    public static PublicationRequestStatus parse(String requestStatus) {

        for (PublicationRequestStatus status : PublicationRequestStatus.values()) {
            if (status.name().equalsIgnoreCase(requestStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException(INVALID_APPROVE_PUBLICATION_REQUEST_STATUS_ERROR + requestStatus);
    }

    /**
     * Changes status for a PublicationRequestStatus change. It will return the new PublicationRequestStatus
     * if the transition is valid.
     *
     * @param requestedStatusChange requested PublicationRequestStatus to transform to.
     * @return New PublicationRequestStatus.
     * @throws IllegalArgumentException requestedStatusChange is not valid to change into.
     */
    public PublicationRequestStatus changeStatus(PublicationRequestStatus requestedStatusChange) {
        if (APPROVED.equals(this)) {
            throw new IllegalArgumentException(getErrorMessageForNotAllowedStatusChange(requestedStatusChange));
        }
        return requestedStatusChange;
    }

    private String getErrorMessageForNotAllowedStatusChange(PublicationRequestStatus requestedStatusChange) {
        return String.format(ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S, this, requestedStatusChange);
    }
}


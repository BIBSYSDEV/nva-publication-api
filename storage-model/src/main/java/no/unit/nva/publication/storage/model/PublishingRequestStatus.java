package no.unit.nva.publication.storage.model;

public enum PublishingRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;
    
    public static final String INVALID_APPROVE_PUBLISHING_REQUEST_STATUS_ERROR = "Invalid PublishingRequestStatus: ";
    public static final String ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S =
        "Not allowed to change status from %s to %s";
    
    public static PublishingRequestStatus parse(String requestStatus) {
        
        for (PublishingRequestStatus status : PublishingRequestStatus.values()) {
            if (status.name().equalsIgnoreCase(requestStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException(INVALID_APPROVE_PUBLISHING_REQUEST_STATUS_ERROR + requestStatus);
    }
    
    /**
     * Changes status for a PublishingRequestStatus change. It will return the new PublishingRequestStatus if the
     * transition is valid.
     *
     * @param requestedStatusChange requested PublishingRequestStatus to transform to.
     * @return New PublishingRequestStatus.
     * @throws IllegalArgumentException requestedStatusChange is not valid to change into.
     */
    public PublishingRequestStatus changeStatus(PublishingRequestStatus requestedStatusChange) {
        if (APPROVED.equals(this)) {
            throw new IllegalArgumentException(getErrorMessageForNotAllowedStatusChange(requestedStatusChange));
        }
        return requestedStatusChange;
    }
    
    private String getErrorMessageForNotAllowedStatusChange(PublishingRequestStatus requestedStatusChange) {
        return String.format(ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S, this, requestedStatusChange);
    }
}


package no.unit.nva.publication.storage.model;

public enum ApprovePublicationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static final String INVALID_APPROVE_PUBLICATION_REQUEST_STATUS_ERROR = "Invalid ApprovePublicationRequest status: ";

    public static ApprovePublicationRequestStatus parse(String requestStatus) {

        for (ApprovePublicationRequestStatus status : ApprovePublicationRequestStatus.values()) {
            if (status.name().equalsIgnoreCase(requestStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException(INVALID_APPROVE_PUBLICATION_REQUEST_STATUS_ERROR + requestStatus);
    }
}


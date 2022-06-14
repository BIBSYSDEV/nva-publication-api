package no.unit.nva.publication.storage.model;

import nva.commons.core.JacocoGenerated;

import java.util.Set;

import static java.util.Collections.emptySet;

@JacocoGenerated
public enum PublicationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static final String INVALID_APPROVE_PUBLICATION_REQUEST_STATUS_ERROR = "Invalid ApprovePublicationRequest status: ";
    public static final String ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S =
            "Not allowed to change status from %s to %s";

    private static final Set<PublicationRequestStatus> validStatusChangeForRejected = Set.of(APPROVED);
    private static final Set<PublicationRequestStatus> validStatusChangeForPending = Set.of(APPROVED, REJECTED);
    private static final Set<PublicationRequestStatus> validDefaultStatusChanges = emptySet();

    public static PublicationRequestStatus parse(String requestStatus) {

        for (PublicationRequestStatus status : PublicationRequestStatus.values()) {
            if (status.name().equalsIgnoreCase(requestStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException(INVALID_APPROVE_PUBLICATION_REQUEST_STATUS_ERROR + requestStatus);
    }

    public boolean isValidStatusChange(PublicationRequestStatus requestedStatusChange) {
        return getValidTransitions(this).contains(requestedStatusChange);
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
        if (isValidStatusChange(requestedStatusChange)) {
            return requestedStatusChange;
        }
        throw new IllegalArgumentException(getErrorMessageForNotAllowedStatusChange(requestedStatusChange));
    }

    protected String getErrorMessageForNotAllowedStatusChange(PublicationRequestStatus requestedStatusChange) {
        return String.format(ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S, this, requestedStatusChange);
    }

    private Set<PublicationRequestStatus> getValidTransitions(PublicationRequestStatus fromRequestStatus) {
        switch (fromRequestStatus) {
            case PENDING:
                return validStatusChangeForPending;
            case REJECTED:
                return validStatusChangeForRejected;
            default:
                return validDefaultStatusChanges;
        }
    }
}


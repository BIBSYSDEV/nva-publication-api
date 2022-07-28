package no.unit.nva.publication.model.business;

import static java.util.Collections.emptySet;
import java.util.Set;

public enum DoiRequestStatus {
    REQUESTED,
    APPROVED,
    REJECTED;
    public static final String ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S =
        "Not allowed to change status from %s to %s";
    public static final String INVALID_DOI_REQUEST_STATUS_ERROR = "Invalid DoiRequest status: ";
    protected static final Set<DoiRequestStatus> validStatusChangeForRejected = Set.of(APPROVED);
    protected static final Set<DoiRequestStatus> validStatusChangeForRequested = Set.of(APPROVED, REJECTED);
    protected static final Set<DoiRequestStatus> validDefaultStatusChanges = emptySet();
    
    public static DoiRequestStatus parse(String doiRequestStatus) {
        DoiRequestStatus[] values = DoiRequestStatus.values();
        
        for (DoiRequestStatus status : values) {
            if (status.name().equalsIgnoreCase(doiRequestStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException(INVALID_DOI_REQUEST_STATUS_ERROR + doiRequestStatus);
    }
    
    public boolean isValidStatusChange(DoiRequestStatus requestedStatusChange) {
        return getValidTransitions(this).contains(requestedStatusChange);
    }
    
    /**
     * Changes status for a DoiRequestStatus change. It will return the new DoiRequestStatus if the transition is
     * valid.
     *
     * @param requestedStatusChange requested DOIRequestStatus to transform to.
     * @return New DoiRequestStatus.
     * @throws IllegalArgumentException requestedStatusChange is not valid to change into.
     */
    public DoiRequestStatus changeStatus(DoiRequestStatus requestedStatusChange) {
        if (isValidStatusChange(requestedStatusChange)) {
            return requestedStatusChange;
        }
        throw new IllegalArgumentException(getErrorMessageForNotAllowedStatusChange(requestedStatusChange));
    }
    
    protected String getErrorMessageForNotAllowedStatusChange(DoiRequestStatus requestedStatusChange) {
        return String.format(ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S, this, requestedStatusChange);
    }
    
    private Set<DoiRequestStatus> getValidTransitions(DoiRequestStatus fromRequestStatus) {
        switch (fromRequestStatus) {
            case REQUESTED:
                return validStatusChangeForRequested;
            case REJECTED:
                return validStatusChangeForRejected;
            default:
                return validDefaultStatusChanges;
        }
    }
}



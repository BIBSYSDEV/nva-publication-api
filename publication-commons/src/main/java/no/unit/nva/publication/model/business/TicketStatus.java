package no.unit.nva.publication.model.business;

import static java.util.Collections.emptySet;
import static no.unit.nva.publication.model.business.TicketStatus.TicketStatusConstants.NEW_STATUS;
import static no.unit.nva.publication.model.business.TicketStatus.TicketStatusConstants.PENDING_STATUS;
import static no.unit.nva.publication.model.business.TicketStatus.TicketStatusConstants.COMPLETED_STATUS;
import static no.unit.nva.publication.model.business.TicketStatus.TicketStatusConstants.CLOSED_STATUS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

public enum TicketStatus {
    NEW(NEW_STATUS, "NEW"),
    PENDING(PENDING_STATUS, "REQUESTED"),
    COMPLETED(COMPLETED_STATUS, "APPROVED"),
    CLOSED(CLOSED_STATUS, "REJECTED");
    
    public static final String ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S =
        "Not allowed to change status from %s to %s";
    public static final String INVALID_DOI_REQUEST_STATUS_ERROR = "Invalid DoiRequest status. Valid values:  ";
    public static final String SEPARATOR = ",";
    static final Set<TicketStatus> validStatusChangeForRejected = Set.of(COMPLETED);
    static final Set<TicketStatus> validStatusChangeForRequested = Set.of(COMPLETED, CLOSED);
    static final Set<TicketStatus> validDefaultStatusChanges = emptySet();
    private final String value;
    private final String legacyValue;
    
    TicketStatus(String value, String legacyValue) {
        this.value = value;
        this.legacyValue = legacyValue;
    }
    
    @JsonCreator
    public static TicketStatus parse(String candidate) {
        return Arrays.stream(TicketStatus.values())
                   .filter(enumValue -> enumValue.filterByNameOrValue(candidate))
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(fail -> handleParsingError());
    }
    
    @JsonValue
    @Override
    public String toString() {
        return value;
    }
    
    public boolean isValidStatusChange(TicketStatus requestedStatusChange) {
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
    public TicketStatus changeStatus(TicketStatus requestedStatusChange) {
        if (isValidStatusChange(requestedStatusChange)) {
            return requestedStatusChange;
        }
        throw new IllegalArgumentException(getErrorMessageForNotAllowedStatusChange(requestedStatusChange));
    }
    
    protected String getErrorMessageForNotAllowedStatusChange(TicketStatus requestedStatusChange) {
        return String.format(ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S, this, requestedStatusChange);
    }
    
    private static IllegalArgumentException handleParsingError() {
        return new IllegalArgumentException(INVALID_DOI_REQUEST_STATUS_ERROR + validValues());
    }
    
    private static String validValues() {
        return Arrays.stream(TicketStatus.values())
                   .map(TicketStatus::toString)
                   .collect(Collectors.joining(SEPARATOR));
    }
    
    private boolean filterByNameOrValue(String candidate) {
        return toString().equalsIgnoreCase(candidate)
               || legacyValue.equalsIgnoreCase(candidate)
               || name().equalsIgnoreCase(candidate);
    }
    
    private Set<TicketStatus> getValidTransitions(TicketStatus fromRequestStatus) {
        switch (fromRequestStatus) {
            case PENDING:
                return validStatusChangeForRequested;
            case CLOSED:
                return validStatusChangeForRejected;
            default:
                return validDefaultStatusChanges;
        }
    }
    
    public static class TicketStatusConstants {

        public static final String NEW_STATUS = "New";
        public static final String PENDING_STATUS = "Pending";
        public static final String COMPLETED_STATUS = "Completed";
        public static final String CLOSED_STATUS = "Closed";
        public static final String READ_STATUS = "Read";
        public static final String UNREAD_STATUS = "Unread";
        
        @JacocoGenerated
        public TicketStatusConstants() {
        }
    }
}



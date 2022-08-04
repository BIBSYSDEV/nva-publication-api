package no.unit.nva.publication.model.business;

import static java.util.Collections.emptySet;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.core.SingletonCollector;

public enum DoiRequestStatus implements TicketStatus {
    PENDING(PENDING_STATUS, "REQUESTED"),
    COMPLETED(COMPLETED_STATUS, "APPROVED"),
    CLOSED(CLOSED_STATUS, "REJECTED");
    
    public static final String ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S =
        "Not allowed to change status from %s to %s";
    public static final String INVALID_DOI_REQUEST_STATUS_ERROR = "Invalid DoiRequest status: ";
    static final Set<DoiRequestStatus> validStatusChangeForRejected = Set.of(COMPLETED);
    static final Set<DoiRequestStatus> validStatusChangeForRequested = Set.of(COMPLETED, CLOSED);
    static final Set<DoiRequestStatus> validDefaultStatusChanges = emptySet();
    private final String value;
    private final String legacyValue;
    
    DoiRequestStatus(String value, String legacyValue) {
        this.value = value;
        this.legacyValue = legacyValue;
    }
    
    @JsonCreator
    public static DoiRequestStatus parse(String candidate) {
        return Arrays.stream(DoiRequestStatus.values())
            .filter(enumValue -> enumValue.filterByNameOrValue(candidate))
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(fail -> handleParsingError());
    }
    
    @JsonValue
    @Override
    public String toString() {
        return value;
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
    
    private static IllegalArgumentException handleParsingError() {
        return new IllegalArgumentException(INVALID_DOI_REQUEST_STATUS_ERROR + validValues());
    }
    
    private static String validValues() {
        return Arrays.stream(DoiRequestStatus.values())
            .map(DoiRequestStatus::toString)
            .collect(Collectors.joining(","));
    }
    
    private boolean filterByNameOrValue(String candidate) {
        return toString().equalsIgnoreCase(candidate)
               || legacyValue.equalsIgnoreCase(candidate)
               || name().equalsIgnoreCase(candidate);
    }
    
    private Set<DoiRequestStatus> getValidTransitions(DoiRequestStatus fromRequestStatus) {
        switch (fromRequestStatus) {
            case PENDING:
                return validStatusChangeForRequested;
            case CLOSED:
                return validStatusChangeForRejected;
            default:
                return validDefaultStatusChanges;
        }
    }
}



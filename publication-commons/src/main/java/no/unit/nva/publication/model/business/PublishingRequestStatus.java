package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;
import java.util.stream.Collectors;

import static no.unit.nva.publication.model.business.TicketStatusConstants.CLOSED_STATUS;
import static no.unit.nva.publication.model.business.TicketStatusConstants.COMPLETED_STATUS;
import static no.unit.nva.publication.model.business.TicketStatusConstants.PENDING_STATUS;

public enum PublishingRequestStatus {
    PENDING(PENDING_STATUS, "PENDING"),
    COMPLETED(COMPLETED_STATUS, "APPROVED"),
    CLOSED(CLOSED_STATUS, "REJECTED");

    public static final String INVALID_APPROVE_PUBLISHING_REQUEST_STATUS_ERROR =
            "Invalid PublishingRequestStatus. Valid values: ";
    public static final String ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S =
        "Not allowed to change status from %s to %s";
    public static final String SEPARATOR = ",";
    private final String value;
    private final String legacyValue;

    PublishingRequestStatus(String value, String legacyValue) {
        this.value = value;
        this.legacyValue = legacyValue;
    }

    @JsonCreator
    public static PublishingRequestStatus parse(String candidate) {
        return Arrays.stream(PublishingRequestStatus.values())
                .filter(enumValue -> enumValue.filterByNameOrValue(candidate))
                .collect(SingletonCollector.tryCollect())
                .orElseThrow(fail -> handleParsingError());
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
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
        if (COMPLETED.equals(this)) {
            throw new IllegalArgumentException(getErrorMessageForNotAllowedStatusChange(requestedStatusChange));
        }
        return requestedStatusChange;
    }
    
    private String getErrorMessageForNotAllowedStatusChange(PublishingRequestStatus requestedStatusChange) {
        return String.format(ERROR_MESSAGE_NOT_ALLOWED_TO_CHANGE_STATUS_FROM_S_TO_S, this, requestedStatusChange);
    }

    private boolean filterByNameOrValue(String candidate) {
        return toString().equalsIgnoreCase(candidate)
                || legacyValue.equalsIgnoreCase(candidate)
                || name().equalsIgnoreCase(candidate);
    }

    private static String validValues() {
        return Arrays.stream(DoiRequestStatus.values())
                .map(DoiRequestStatus::toString)
                .collect(Collectors.joining(SEPARATOR));
    }

    private static IllegalArgumentException handleParsingError() {
        return new IllegalArgumentException(INVALID_APPROVE_PUBLISHING_REQUEST_STATUS_ERROR + validValues());
    }
}


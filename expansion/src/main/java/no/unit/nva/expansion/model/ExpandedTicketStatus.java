package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.stream.Collectors;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.SingletonCollector;

public enum ExpandedTicketStatus {
    NEW("New"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    CLOSED("Closed");

    private static final String INVALID_EXPANDED_TICKET_STATUS_ERROR = "Invalid ExpandedTicket status. Valid values:  ";
    private static final String SEPARATOR = ",";
    private final String value;

    ExpandedTicketStatus(String value) {
        this.value = value;
    }

    public static ExpandedTicketStatus parse(String candidate) {
        return Arrays.stream(ExpandedTicketStatus.values())
            .filter(enumValue -> enumValue.filterByNameOrValue(candidate))
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(fail -> handleParsingError());
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }

    private static IllegalArgumentException handleParsingError() {
        return new IllegalArgumentException(INVALID_EXPANDED_TICKET_STATUS_ERROR + validValues());
    }

    private static String validValues() {
        return Arrays.stream(TicketStatus.values())
            .map(TicketStatus::toString)
            .collect(Collectors.joining(SEPARATOR));
    }

    private boolean filterByNameOrValue(String candidate) {
        return toString().equalsIgnoreCase(candidate)
               || name().equalsIgnoreCase(candidate);
    }
}

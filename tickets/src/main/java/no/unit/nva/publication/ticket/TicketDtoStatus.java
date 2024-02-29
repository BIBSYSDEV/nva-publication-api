package no.unit.nva.publication.ticket;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.stream.Collectors;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.SingletonCollector;

public enum TicketDtoStatus {
    NEW("New"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    NOT_RELEVANT("Not Relevant"),
    CLOSED("Closed");

    private static final String INVALID_TICKET_STATUS_ERROR = "Invalid ticketDto status. Valid values:  ";
    private static final String SEPARATOR = ",";
    private final String value;

    TicketDtoStatus(String value) {
        this.value = value;
    }

    public static TicketDtoStatus parse(String candidate) {
        return Arrays.stream(TicketDtoStatus.values())
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
        return new IllegalArgumentException(INVALID_TICKET_STATUS_ERROR + validValues());
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

package no.unit.nva.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TicketOperation {

    APPROVE("approve");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid AllowedOperation, expected one of: %s";
    public static final String DELIMITER = ", ";
    private final String value;

    TicketOperation(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Lookup enum by value.
     *
     * @param value value
     * @return enum
     */
    @JsonCreator
    public static TicketOperation lookup(String value) {
        return stream(values())
                   .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException(
                       format(ERROR_MESSAGE_TEMPLATE, value,
                              stream(TicketOperation.values())
                                  .map(TicketOperation::toString).collect(joining(DELIMITER)))));
    }
}

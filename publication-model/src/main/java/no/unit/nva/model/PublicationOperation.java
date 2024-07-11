package no.unit.nva.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PublicationOperation {

    UPDATE("update"),
    UNPUBLISH("unpublish"),
    TICKET_PUBLISH("ticket/publish"),
    DELETE("delete"),
    TERMINATE("terminate");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid AllowedOperation, expected one of: %s";
    public static final String DELIMITER = ", ";
    private String value;

    PublicationOperation(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Lookup enum by value.
     *
     * @param value value
     * @return enum
     */
    public static PublicationOperation lookup(String value) {
        return stream(values())
                   .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException(
                       format(ERROR_MESSAGE_TEMPLATE, value,
                              stream(PublicationOperation.values())
                                  .map(PublicationOperation::toString).collect(joining(DELIMITER)))));
    }
}

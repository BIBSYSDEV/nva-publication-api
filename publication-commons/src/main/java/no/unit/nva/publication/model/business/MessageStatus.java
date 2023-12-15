package no.unit.nva.publication.model.business;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonValue;
import no.unit.nva.model.PublicationStatus;
import nva.commons.core.JacocoGenerated;

public enum MessageStatus {


    ACTIVE("Active"),
    DELETED("Deleted");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid MessageStatus, expected one of: %s";
    public static final String DELIMITER = ", ";
    private String value;

    MessageStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JacocoGenerated
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Lookup enum by value.
     *
     * @param value value
     * @return enum
     */
    public static MessageStatus lookup(String value) {
        return stream(values())
                   .filter(status -> status.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> unsupportedStatusException(value));
    }

    private static IllegalArgumentException unsupportedStatusException(String value) {
        return new IllegalArgumentException(
            format(ERROR_MESSAGE_TEMPLATE, value,
                   stream(PublicationStatus.values())
                       .map(PublicationStatus::toString).collect(joining(DELIMITER))));
    }
}

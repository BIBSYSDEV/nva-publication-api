package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonValue;
import no.unit.nva.model.PublicationStatus;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public enum ImportStatus {

    IMPORTED("IMPORTED"),
    NOT_IMPORTED("NOT_IMPORTED"),
    NOT_APPLICABLE("NOT_APPLICABLE");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid ImportStatus, expected one of: %s";
    public static final String DELIMITER = ", ";
    private String value;

    ImportStatus(String value) {
        this.value = value;
    }

    /**
     * Lookup enum by value.
     *
     * @param value value
     * @return enum
     */
    public static ImportStatus lookup(String value) {
        return stream(values())
                   .filter(importStatus -> importStatus.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException(
                       format(ERROR_MESSAGE_TEMPLATE, value, stream(PublicationStatus.values())
                                                                 .map(PublicationStatus::toString)
                                                                 .collect(joining(DELIMITER)))));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

package no.unit.nva.publication.doi.dto;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DoiRequestStatus {

    REQUESTED("REQUESTED"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid DoiRequestStatus, expected one of: %s";
    public static final String DELIMITER = ", ";
    private final String value;

    DoiRequestStatus(String value) {
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
    public static DoiRequestStatus lookup(String value) {
        return stream(values())
            .filter(doiRequestStatus -> doiRequestStatus.getValue().equalsIgnoreCase(value))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(
                format(ERROR_MESSAGE_TEMPLATE, value, stream(DoiRequestStatus.values())
                    .map(DoiRequestStatus::toString).collect(joining(DELIMITER)))));
    }
}

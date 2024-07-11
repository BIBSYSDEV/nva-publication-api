package no.unit.nva.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Revision {

    REVISED("Revised"),
    UNREVISED("Unrevised");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid Revision, expected one of: %s";
    public static final String DELIMITER = ", ";
    private String value;

    Revision(String value) {
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
    public static Revision fromValue(String value) {
        return stream(values())
                   .filter(revision -> revision.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException(
                       format(ERROR_MESSAGE_TEMPLATE, value,
                              stream(Revision.values())
                                  .map(Revision::toString).collect(joining(DELIMITER)))));
    }
}

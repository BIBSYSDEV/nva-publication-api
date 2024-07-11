package no.unit.nva.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NameType {

    ORGANIZATIONAL("Organizational"),
    PERSONAL("Personal");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid NameType, expected one of: %s";
    public static final String DELIMITER = ", ";
    private String value;

    NameType(String value) {
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
    public static NameType lookup(String value) {
        return stream(values())
                   .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException(
                       format(ERROR_MESSAGE_TEMPLATE, value,
                              stream(NameType.values())
                                  .map(NameType::toString).collect(joining(DELIMITER)))));
    }
}

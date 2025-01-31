package no.unit.nva.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FileOperation {

    READ_METADATA("read-metadata"),
    WRITE_METADATA("write-metadata"),
    DELETE("delete"),
    DOWNLOAD("download");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid AllowedOperation, expected one of: %s";
    public static final String DELIMITER = ", ";
    private final String value;

    FileOperation(String value) {
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
    public static FileOperation lookup(String value) {
        return stream(values())
                   .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException(
                       format(ERROR_MESSAGE_TEMPLATE, value,
                              stream(FileOperation.values())
                                  .map(FileOperation::toString).collect(joining(DELIMITER)))));
    }
}

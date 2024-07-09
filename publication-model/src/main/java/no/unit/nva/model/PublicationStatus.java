package no.unit.nva.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

public enum PublicationStatus {

    NEW("NEW"),
    DRAFT("DRAFT"),
    PUBLISHED_METADATA("PUBLISHED_METADATA"),
    PUBLISHED("PUBLISHED"),
    DELETED("DELETED"),
    UNPUBLISHED("UNPUBLISHED"),
    DRAFT_FOR_DELETION("DRAFT_FOR_DELETION");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid PublicationStatus, expected one of: %s";
    public static final String DELIMITER = ", ";
    private String value;

    PublicationStatus(String value) {
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
    public static PublicationStatus lookup(String value) {
        return stream(values())
                   .filter(publicationStatus -> publicationStatus.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> new IllegalArgumentException(
                       format(ERROR_MESSAGE_TEMPLATE, value,
                              stream(PublicationStatus.values())
                                  .map(PublicationStatus::toString).collect(joining(DELIMITER)))));
    }
}

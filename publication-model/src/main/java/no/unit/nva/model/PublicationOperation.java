package no.unit.nva.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PublicationOperation {

    UPDATE("update"),
    UPDATE_FILES("update-including-files"),
    READ_HIDDEN_FILES("read-hidden-files"),
    UNPUBLISH("unpublish"),
    REPUBLISH("republish"),
    DELETE("delete"),
    TERMINATE("terminate"),
    DOI_REQUEST_CREATE("doi-request-create"),
    DOI_REQUEST_APPROVE("doi-request-approve"),
    PUBLISHING_REQUEST_CREATE("publishing-request-create"),
    PUBLISHING_REQUEST_APPROVE("publishing-request-approve"),
    SUPPORT_REQUEST_CREATE("support-request-create"),
    SUPPORT_REQUEST_APPROVE("support-request-approve"),
    UPLOAD_FILE("upload-file");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid AllowedOperation, expected one of: %s";
    public static final String DELIMITER = ", ";
    private final String value;

    PublicationOperation(String value) {
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

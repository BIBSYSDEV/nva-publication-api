package no.unit.nva.model.associatedartifacts.file;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonValue;
import no.unit.nva.model.Revision;

public enum PublisherVersion {
    PUBLISHED_VERSION("PublishedVersion"), ACCEPTED_VERSION("AcceptedVersion");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid PublisherVersion, expected one of: %s";

    public static final String DELIMITER = ", ";

    private final String value;

    PublisherVersion(String value) {
        this.value = value;
    }

    public static PublisherVersion parse(String value) {
        return stream(values())
                   .filter(revision -> revision.getValue().equalsIgnoreCase(value))
                   .findAny()
                   .orElseThrow(() -> throwIllegalArgumentException(value));
    }

    private static IllegalArgumentException throwIllegalArgumentException(String value) {
        return new IllegalArgumentException(format(ERROR_MESSAGE_TEMPLATE, value,
                                                   stream(Revision.values()).map(Revision::toString)
                                                       .collect(joining(DELIMITER))));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

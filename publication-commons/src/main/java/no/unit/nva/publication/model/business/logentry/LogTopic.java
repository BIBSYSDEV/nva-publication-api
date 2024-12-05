package no.unit.nva.publication.model.business.logentry;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LogTopic {

    FILE_PUBLISHED("FilePublished"),
    FILE_REJECTED("FileRejected"),
    FILE_UPLOADED("FileUploaded"),
    PUBLICATION_CREATED("PublicationCreated"),
    PUBLICATION_DELETED("PublicationDeleted"),
    PUBLICATION_PUBLISHED("PublicationPublished"),
    PUBLICATION_UNPUBLISHED("PublicationUnpublished");

    private final String value;

    LogTopic(String value) {
        this.value = value;
    }

    public static LogTopic fromValue(String value) {
        return stream(values()).filter(topic -> topic.getValue().equalsIgnoreCase(value)).findAny().orElseThrow();
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

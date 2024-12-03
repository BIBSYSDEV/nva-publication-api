package no.unit.nva.publication.model.business.logentry;

import static java.util.Arrays.stream;

public enum LogTopic {

    PUBLICATION_CREATED("PublicationCreated"), METADATA_PUBLISHED("PublicationPublished"), PUBLICATION_UNPUBLISHED(
        "PublicationUnpublished"), PUBLICATION_DELETED("PublicationDeleted"), FILE_UPLOADED(
        "FileUploaded"), FILE_PUBLISHED("FilePublished"), FILE_REJECTED("FileRejected");

    private final String value;

    LogTopic(String value) {
        this.value = value;
    }

    public static LogTopic fromValue(String value) {
        return stream(values()).filter(topic -> topic.getValue().equalsIgnoreCase(value)).findAny().orElseThrow();
    }

    public String getValue() {
        return value;
    }
}

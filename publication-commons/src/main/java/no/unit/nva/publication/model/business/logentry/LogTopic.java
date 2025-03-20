package no.unit.nva.publication.model.business.logentry;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LogTopic {

    FILE_APPROVED("FileApproved"),
    FILE_REJECTED("FileRejected"),
    FILE_UPLOADED("FileUploaded"),
    FILE_IMPORTED("FileImported"),
    FILE_RETRACTED("FileRetracted"),
    FILE_HIDDEN("FileHidden"),
    FILE_DELETED("FileDeleted"),
    DOI_RESERVED("DoiReserved"),
    DOI_REQUESTED("DoiRequested"),
    DOI_REJECTED("DoiRejected"),
    DOI_ASSIGNED("DoiAssigned"),
    PUBLICATION_CREATED("PublicationCreated"),
    PUBLICATION_DELETED("PublicationDeleted"),
    PUBLICATION_PUBLISHED("PublicationPublished"),
    PUBLICATION_REPUBLISHED("PublicationRepublished"),
    PUBLICATION_UNPUBLISHED("PublicationUnpublished"),
    PUBLICATION_IMPORTED("PublicationImported"),
    PUBLICATION_MERGED("PublicationMerged");

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

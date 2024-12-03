package no.unit.nva.publication.model.business.logentry;

public enum LogTopic {

    PUBLICATION_CREATED("PublicationCreated");

    private final String value;

    LogTopic(String value) {
        this.value = value;
    }
}

package no.sikt.nva.brage.migration.merger;

public class MergePublicationException extends RuntimeException {

    public MergePublicationException(String message) {
        super(message);
    }

    public MergePublicationException(Exception exception) {
        super(exception);
    }
}

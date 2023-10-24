package no.sikt.nva.brage.migration.merger;

public class DuplicatePublicationException extends RuntimeException {

    public DuplicatePublicationException(String message, Exception exception) {
        super(message, exception);
    }
}

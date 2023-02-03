package no.unit.nva.cristin.patcher.exception;

public class ParentPublicationException extends RuntimeException {

    public ParentPublicationException(String message, Exception exception) {
        super(message, exception);
    }
}

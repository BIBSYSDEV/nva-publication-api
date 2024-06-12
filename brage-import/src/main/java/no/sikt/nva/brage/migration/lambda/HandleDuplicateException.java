package no.sikt.nva.brage.migration.lambda;

public class HandleDuplicateException extends RuntimeException {

    public HandleDuplicateException(String message) {
        super(message);
    }
}

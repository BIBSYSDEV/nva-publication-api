package no.sikt.nva.brage.migration.lambda;

public class MissingFieldsException extends RuntimeException {

    public MissingFieldsException(String message) {
        super(message);
    }
}

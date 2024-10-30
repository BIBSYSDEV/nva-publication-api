package no.unit.nva.publication.services.exceptions;

public class TransactionFailedException extends RuntimeException {

    public TransactionFailedException(Exception exception) {
        super(exception);
    }

    public TransactionFailedException(String message) {
        super(message);
    }
}

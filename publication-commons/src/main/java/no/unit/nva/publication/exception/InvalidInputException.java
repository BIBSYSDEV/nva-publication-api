package no.unit.nva.publication.exception;

public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String emptyMessageError) {
        super(emptyMessageError);
    }
}

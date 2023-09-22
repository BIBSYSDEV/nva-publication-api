package no.unit.nva.cristin.mapper.nva.exceptions;

public class InvalidArchiveException extends RuntimeException {

    public InvalidArchiveException(Exception e) {
        super(e);
    }
}

package no.unit.nva.publication.service.impl.exceptions;

public class EmptyValueMapException extends RuntimeException {

    public static String DEFAULT_MESSAGE = "Update request should specify non null return value";

    public EmptyValueMapException() {
        super(DEFAULT_MESSAGE);
    }
}

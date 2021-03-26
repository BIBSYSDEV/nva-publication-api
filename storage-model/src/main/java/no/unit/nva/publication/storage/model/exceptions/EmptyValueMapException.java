package no.unit.nva.publication.storage.model.exceptions;

public class EmptyValueMapException extends RuntimeException {
    
    public static String DEFAULT_MESSAGE = "Trying to parse empty item";
    
    public EmptyValueMapException() {
        super(DEFAULT_MESSAGE);
    }
}

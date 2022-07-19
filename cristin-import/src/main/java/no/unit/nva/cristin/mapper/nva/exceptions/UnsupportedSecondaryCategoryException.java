package no.unit.nva.cristin.mapper.nva.exceptions;

public class UnsupportedSecondaryCategoryException extends RuntimeException {
    
    public static final String ERROR_PARSING_SECONDARY_CATEGORY = "Error parsing secondary category";
    
    public UnsupportedSecondaryCategoryException() {
        super(ERROR_PARSING_SECONDARY_CATEGORY);
    }
}

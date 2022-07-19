package no.unit.nva.cristin.mapper.nva.exceptions;

public class MissingContributorsException extends RuntimeException {
    
    public static final String MISSING_CONTRIBUTORS_ERROR_MESSAGE =
        "No contributors present. All publications must have contributors.";
    
    public MissingContributorsException() {
        super(MISSING_CONTRIBUTORS_ERROR_MESSAGE);
    }
}

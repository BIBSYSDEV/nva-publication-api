package no.unit.nva.publication.validation;

public class ConfigNotAvailableException extends RuntimeException {

    public ConfigNotAvailableException(String message) {
        super(message);
    }

    public ConfigNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

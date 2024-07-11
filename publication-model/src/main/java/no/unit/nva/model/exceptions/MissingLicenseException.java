package no.unit.nva.model.exceptions;

public class MissingLicenseException extends RuntimeException {
    public MissingLicenseException(String message) {
        super(message);
    }
}
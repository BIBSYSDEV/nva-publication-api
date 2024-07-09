package no.unit.nva.model.exceptions;


public class InvalidIssnException extends Exception {

    public static final String MESSAGE_TEMPLATE = "The ISSN \"%s\" is invalid";

    public InvalidIssnException(String issn) {
        super(String.format(MESSAGE_TEMPLATE, issn));
    }
}

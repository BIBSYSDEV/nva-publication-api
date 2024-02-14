package no.unit.nva.expansion.model;

public class ExpansionException extends RuntimeException {

    public ExpansionException(String message) {
        super(message);
    }

    public static ExpansionException withMessage(String message) {
        return new ExpansionException(message);
    }

}

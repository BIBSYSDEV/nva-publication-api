package no.unit.nva.model.exceptions;

import java.util.List;

public class InvalidIsbnException extends Exception {

    public static final String ERROR_TEMPLATE = "The provided ISBN(s) %s is/are invalid";

    public InvalidIsbnException(List<String> isbnList) {
        super(String.format(ERROR_TEMPLATE, String.join(", ", isbnList)));
    }
}

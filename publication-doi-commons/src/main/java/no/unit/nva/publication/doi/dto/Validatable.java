package no.unit.nva.publication.doi.dto;

import static java.util.Objects.isNull;

public class Validatable {

    public static final String MANDATORY_FIELD_ERROR_PREFIX = "Mandatory field is missing: ";
    protected final String errorMessagePrefix =
        String.format(MANDATORY_FIELD_ERROR_PREFIX + " %s: ", this.getClass().getSimpleName());

    protected Validatable() {

    }

    protected <T> void requireFieldIsNotNull(T value, String fieldName) {
        if (isNull(value)) {
            String errorMessage = errorMessagePrefix + fieldName;
            throw new IllegalArgumentException(errorMessage);
        }
    }
}

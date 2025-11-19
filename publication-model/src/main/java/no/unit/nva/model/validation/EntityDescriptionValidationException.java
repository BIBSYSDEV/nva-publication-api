package no.unit.nva.model.validation;

import java.util.Collection;

public class EntityDescriptionValidationException extends ValidationException {

    public EntityDescriptionValidationException(String message) {
        super(message);
    }

    public EntityDescriptionValidationException(Collection<String> errors) {
        this(String.join(", ", errors));
    }
}

package no.unit.nva.model.validation;

import no.unit.nva.model.EntityDescription;

public interface EntityDescriptionValidator {
    ValidationReport validate(EntityDescription entityDescription);
}

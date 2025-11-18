package no.unit.nva.model.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public record EntityDescriptionValidationReport(Set<String> errors) implements ValidationReport {
    public EntityDescriptionValidationReport(Collection<String> errors) {
        this(new HashSet<>(errors));
    }

    @Override
    public boolean passes() {
        return errors.isEmpty();
    }
}

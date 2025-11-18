package no.unit.nva.model.validation;

import java.util.Set;

public interface ValidationReport {

    boolean isValid();

    Set<String> errors();
}

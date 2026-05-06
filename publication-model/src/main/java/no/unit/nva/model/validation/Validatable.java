package no.unit.nva.model.validation;

public interface Validatable<T extends Validatable<T>> {

  ValidationResult validate(Validator<T> validator);
}

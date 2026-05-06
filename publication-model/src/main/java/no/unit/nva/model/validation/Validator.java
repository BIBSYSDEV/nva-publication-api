package no.unit.nva.model.validation;

public interface Validator<T> {

  ValidationResult validate(T target);
}

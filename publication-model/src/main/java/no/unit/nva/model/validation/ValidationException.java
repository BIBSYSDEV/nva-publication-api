package no.unit.nva.model.validation;

import static java.util.Collections.emptyList;

import java.util.List;

public class ValidationException extends RuntimeException {

  private final List<ValidationError> errors;

  public ValidationException(String message) {
    this(message, emptyList());
  }

  public ValidationException(String message, List<ValidationError> errors) {
    super(message);
    this.errors = List.copyOf(errors);
  }

  public List<ValidationError> getErrors() {
    return errors;
  }
}

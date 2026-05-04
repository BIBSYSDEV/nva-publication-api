package no.unit.nva.model.validation;

import java.util.List;
import nva.commons.apigateway.exceptions.ValidationError;

public class ValidationException extends RuntimeException {

  private final List<ValidationError> errors;

  public ValidationException(String message) {
    this(message, List.of());
  }

  public ValidationException(String message, List<ValidationError> errors) {
    super(message);
    this.errors = List.copyOf(errors);
  }

  public List<ValidationError> getErrors() {
    return errors;
  }
}

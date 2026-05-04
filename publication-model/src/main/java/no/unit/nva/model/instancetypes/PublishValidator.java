package no.unit.nva.model.instancetypes;

import java.util.List;
import nva.commons.apigateway.exceptions.ValidationError;

@FunctionalInterface
public interface PublishValidator {

  List<ValidationError> validateForPublish();
}

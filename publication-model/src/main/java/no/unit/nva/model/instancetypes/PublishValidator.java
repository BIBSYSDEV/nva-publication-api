package no.unit.nva.model.instancetypes;

import java.util.List;
import no.unit.nva.model.validation.ValidationError;

@FunctionalInterface
public interface PublishValidator {

  List<ValidationError> validateForPublish();
}

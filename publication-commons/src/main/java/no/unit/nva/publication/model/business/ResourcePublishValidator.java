package no.unit.nva.publication.model.business;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublishValidator;
import no.unit.nva.model.validation.ValidationError;
import no.unit.nva.model.validation.ValidationResult;
import no.unit.nva.model.validation.Validator;

public class ResourcePublishValidator implements Validator<Resource> {

  static final List<PublicationStatus> PUBLISHABLE_STATUSES =
      List.of(DRAFT, PUBLISHED_METADATA, UNPUBLISHED);
  static final String STATUS_NOT_PUBLISHABLE_MESSAGE =
      "publication cannot be published in current status";
  static final String STATUS_POINTER = "#/status";
  static final String MAIN_TITLE_REQUIRED_MESSAGE = "mainTitle is required";
  static final String MAIN_TITLE_POINTER = "#/entityDescription/mainTitle";

  @Override
  public ValidationResult validate(Resource resource) {
    var errors = new ArrayList<ValidationError>();
    if (!PUBLISHABLE_STATUSES.contains(resource.getStatus())) {
      errors.add(new ValidationError(STATUS_NOT_PUBLISHABLE_MESSAGE, STATUS_POINTER));
    }
    if (mainTitleIsMissing(resource)) {
      errors.add(new ValidationError(MAIN_TITLE_REQUIRED_MESSAGE, MAIN_TITLE_POINTER));
    }
    errors.addAll(collectInstanceValidationErrors(resource));
    return new ValidationResult(errors);
  }

  private boolean mainTitleIsMissing(Resource resource) {
    return Optional.ofNullable(resource.getEntityDescription())
        .map(EntityDescription::getMainTitle)
        .isEmpty();
  }

  private List<ValidationError> collectInstanceValidationErrors(Resource resource) {
    return Optional.ofNullable(resource.getEntityDescription())
        .map(EntityDescription::getReference)
        .map(Reference::getPublicationInstance)
        .filter(PublishValidator.class::isInstance)
        .map(PublishValidator.class::cast)
        .map(PublishValidator::validateForPublish)
        .orElse(List.of());
  }
}

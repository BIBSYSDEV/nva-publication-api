package no.unit.nva.publication.permissions.publication.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_IMPORT;

import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class ImporterGrantStrategy extends PublicationStrategyBase
    implements PublicationGrantStrategy {

  public ImporterGrantStrategy(Resource resource, UserInstance userInstance) {
    super(resource, userInstance);
  }

  @Override
  public boolean allowsAction(PublicationOperation permission) {
    if (permission == PublicationOperation.ADD_ADDITIONAL_IDENTIFIERS) {
      return hasAccessRight(MANAGE_IMPORT);
    }
    return false;
  }
}

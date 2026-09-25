package no.unit.nva.publication.update;

import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class RepublishUtil {

  private final ResourceService resourceService;
  private final PublicationPermissions permissionStrategy;

  public RepublishUtil(ResourceService resourceService, PublicationPermissions permissionStrategy) {
    this.resourceService = resourceService;
    this.permissionStrategy = permissionStrategy;
  }

  public static RepublishUtil create(
      ResourceService resourceService, PublicationPermissions permissionStrategy) {
    return new RepublishUtil(resourceService, permissionStrategy);
  }

  public Resource republish(Resource resource, UserInstance userInstance)
      throws ApiGatewayException {
    validateRepublishing();
    resource.republish(resourceService, userInstance);

    return resource
        .fetch(resourceService)
        .orElseThrow(() -> new NotFoundException("Resource not found!"));
  }

  private void validateRepublishing() throws ForbiddenException {
    if (!permissionStrategy.allowsAction(PublicationOperation.REPUBLISH)) {
      throw new ForbiddenException();
    }
  }
}

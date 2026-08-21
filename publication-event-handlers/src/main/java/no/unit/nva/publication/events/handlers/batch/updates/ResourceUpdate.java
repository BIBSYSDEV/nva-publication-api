package no.unit.nva.publication.events.handlers.batch.updates;

import no.unit.nva.publication.events.handlers.batch.ManualUpdate;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

abstract class ResourceUpdate implements ManualUpdate {

  private final ResourceService resourceService;

  protected ResourceUpdate(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @Override
  public final void apply(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var updated = update(resource, request);
    resourceService.updateResource(updated, UserInstance.fromPublication(updated.toPublication()));
  }

  protected abstract Resource update(Resource resource, ManuallyUpdatePublicationsRequest request);
}

package no.unit.nva.publication.events.handlers.batch.updates;

import static nva.commons.core.attempt.Try.attempt;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.handlers.batch.ManualUpdate;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.events.handlers.batch.PublicationDiff;
import no.unit.nva.publication.events.handlers.batch.ResourceChange;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

abstract class ResourceUpdate implements ManualUpdate {

  private final ResourceService resourceService;

  protected ResourceUpdate(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @Override
  public final ResourceChange apply(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var target = request.isDryRun() ? detachedCopyOf(resource) : resource;
    var before = PublicationDiff.snapshot(target.toPublication());
    var updated = update(target, request);
    var after = PublicationDiff.snapshot(updated.toPublication());
    var fieldChanges = PublicationDiff.between(before, after);
    var shouldPersist = !request.isDryRun() && !fieldChanges.isEmpty();

    if (shouldPersist) {
      persist(updated);
    }
    UpdateLog.logApplied(request, resource, fieldChanges.size(), shouldPersist);
    return new ResourceChange(resource.getIdentifier().toString(), fieldChanges);
  }

  protected abstract Resource update(Resource resource, ManuallyUpdatePublicationsRequest request);

  private Resource detachedCopyOf(Resource resource) {
    var snapshot = PublicationDiff.snapshot(resource.toPublication());
    return Resource.fromPublication(
        attempt(() -> JsonUtils.dtoObjectMapper.treeToValue(snapshot, Publication.class))
            .orElseThrow());
  }

  private void persist(Resource resource) {
    resourceService.updateResource(
        resource, UserInstance.fromPublication(resource.toPublication()));
  }
}

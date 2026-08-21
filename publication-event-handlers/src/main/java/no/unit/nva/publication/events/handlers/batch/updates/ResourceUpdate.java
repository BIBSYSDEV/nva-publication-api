package no.unit.nva.publication.events.handlers.batch.updates;

import static nva.commons.core.attempt.Try.attempt;

import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.events.handlers.batch.FieldChange;
import no.unit.nva.publication.events.handlers.batch.ManualUpdate;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.events.handlers.batch.ResourceDiff;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

abstract class ResourceUpdate implements ManualUpdate {

  private final ResourceService resourceService;

  protected ResourceUpdate(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @Override
  public final List<FieldChange> plan(
      Resource resource, ManuallyUpdatePublicationsRequest request) {
    var target = detachedCopyOf(resource);
    var before = ResourceDiff.snapshot(target);
    var after = ResourceDiff.snapshot(update(target, request));
    return ResourceDiff.between(before, after);
  }

  @Override
  public final void commit(Resource resource, ManuallyUpdatePublicationsRequest request) {
    CommitGuard.rejectDryRun(request);
    var updated = update(resource, request);
    resourceService.updateResource(updated, UserInstance.fromPublication(updated.toPublication()));
  }

  protected abstract Resource update(Resource resource, ManuallyUpdatePublicationsRequest request);

  private static Resource detachedCopyOf(Resource resource) {
    var snapshot = ResourceDiff.snapshot(resource);
    return attempt(() -> JsonUtils.dtoObjectMapper.treeToValue(snapshot, Resource.class))
        .orElseThrow();
  }
}

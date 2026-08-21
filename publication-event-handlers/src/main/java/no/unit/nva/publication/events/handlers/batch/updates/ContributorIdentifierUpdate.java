package no.unit.nva.publication.events.handlers.batch.updates;

import no.unit.nva.publication.events.handlers.batch.Contributors;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

public final class ContributorIdentifierUpdate extends ResourceUpdate {

  public ContributorIdentifierUpdate(ResourceService resourceService) {
    super(resourceService);
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.CONTRIBUTOR_IDENTIFIER;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return Contributors.of(resource).stream()
        .anyMatch(contributor -> Contributors.hasIdentifier(contributor, request.oldValue()));
  }

  @Override
  protected Resource update(Resource resource, ManuallyUpdatePublicationsRequest request) {
    Contributors.of(resource).stream()
        .filter(contributor -> Contributors.hasIdentifier(contributor, request.oldValue()))
        .findFirst()
        .orElseThrow()
        .identity()
        .setId(Contributors.personUri(request.newValue()));
    return resource;
  }
}

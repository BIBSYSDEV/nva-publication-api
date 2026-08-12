package no.unit.nva.publication.events.handlers.batch;

import no.unit.nva.publication.model.business.Resource;

public interface ManualUpdate {

  ManualUpdateType type();

  boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request);

  ResourceChange apply(Resource resource, ManuallyUpdatePublicationsRequest request);
}

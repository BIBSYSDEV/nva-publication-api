package no.unit.nva.publication.events.handlers.batch;

import java.util.List;
import no.unit.nva.publication.model.business.Resource;

public interface ManualUpdate {

  ManualUpdateType type();

  boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request);

  List<FieldChange> plan(Resource resource, ManuallyUpdatePublicationsRequest request);

  void commit(Resource resource, ManuallyUpdatePublicationsRequest request);
}

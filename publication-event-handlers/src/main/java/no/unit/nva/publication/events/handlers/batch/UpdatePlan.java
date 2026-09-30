package no.unit.nva.publication.events.handlers.batch;

import java.util.List;
import no.unit.nva.publication.model.business.Resource;

record UpdatePlan(Resource resource, List<FieldChange> fieldChanges) {

  boolean hasChanges() {
    return !fieldChanges.isEmpty();
  }

  ResourceChange toResourceChange() {
    return new ResourceChange(resource.getIdentifier().toString(), fieldChanges);
  }
}

package no.unit.nva.publication.events.handlers.batch;

import java.util.List;
import no.unit.nva.publication.model.business.Resource;

public record UpdatePlan(Resource resource, List<FieldChange> fieldChanges) {

  public boolean hasChanges() {
    return !fieldChanges.isEmpty();
  }

  public ResourceChange toResourceChange() {
    return new ResourceChange(resource.getIdentifier().toString(), fieldChanges);
  }
}

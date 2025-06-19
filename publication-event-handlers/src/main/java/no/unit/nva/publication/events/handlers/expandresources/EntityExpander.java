package no.unit.nva.publication.events.handlers.expandresources;

import java.util.Optional;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.model.business.Entity;

public interface EntityExpander {

    boolean canExpand(Entity entity);

    Optional<ExpandedDataEntry> expand(ResourceExpansionService resourceExpansionService,
                                       Entity oldEntity,
                                       Entity newEntity);
}

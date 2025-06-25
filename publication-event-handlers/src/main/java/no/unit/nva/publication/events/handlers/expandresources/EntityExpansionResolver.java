package no.unit.nva.publication.events.handlers.expandresources;

import java.util.Optional;
import no.unit.nva.publication.model.business.Entity;

public interface EntityExpansionResolver {

    Optional<Entity> resolveEntityToExpand(Entity oldEntity, Entity newEntity);
}

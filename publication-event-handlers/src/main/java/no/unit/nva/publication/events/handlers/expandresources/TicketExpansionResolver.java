package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.nonNull;
import java.util.Optional;
import no.unit.nva.publication.model.business.Entity;

public class TicketExpansionResolver implements EntityExpansionResolver {

    @Override
    public Optional<Entity> resolveEntityToExpand(Entity oldEntity, Entity newEntity) {
        return nonNull(newEntity) ? Optional.of(newEntity) : Optional.empty();
    }
}

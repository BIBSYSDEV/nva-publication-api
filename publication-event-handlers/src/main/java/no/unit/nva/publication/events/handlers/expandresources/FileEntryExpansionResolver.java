package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.nonNull;
import java.util.Optional;
import no.unit.nva.publication.model.business.Entity;

public class FileEntryExpansionResolver implements EntityExpansionResolver {

    @Override
    public Optional<Entity> resolveEntityToExpand(Entity oldEntity, Entity newEntity) {
        var entity = nonNull(newEntity) ? newEntity : oldEntity;
        return Optional.ofNullable(entity);
    }
}

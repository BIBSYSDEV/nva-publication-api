package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.nonNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.publication.model.business.Entity;

public class EntityExpansionResolverRegistry {

    private final Map<Class<? extends Entity>, EntityExpansionResolver> resolvers = new HashMap<>();

    public void register(Class<? extends Entity> entityClass, EntityExpansionResolver strategy) {
        resolvers.put(entityClass, strategy);
    }

    public Optional<Entity> resolveEntityToExpand(Entity oldEntity, Entity newEntity) {
        var entity = nonNull(oldEntity) ? oldEntity : newEntity;
        return resolvers.entrySet().stream()
                   .filter(entry -> entry.getKey().isAssignableFrom(entity.getClass()))
                   .findFirst()
                   .orElseThrow(() -> new NoEntityExpansionResolverException(entity.getClass()))
                   .getValue()
                   .resolveEntityToExpand(oldEntity, newEntity);
    }
}

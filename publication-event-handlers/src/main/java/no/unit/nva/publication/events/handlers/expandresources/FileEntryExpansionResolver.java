package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileEntryExpansionResolver implements EntityExpansionResolver {

    private static final Logger logger = LoggerFactory.getLogger(FileEntryExpansionResolver.class);
    private static final String OPERATION_INSERT_MODIFY = "insertion/modification";
    private static final String OPERATION_DELETE = "deletion";

    @Override
    public Optional<Entity> resolveEntityToExpand(Entity oldEntity, Entity newEntity) {
        var entity = nonNull(newEntity) ? logOperationOnEntity(OPERATION_INSERT_MODIFY, newEntity)
                         : logOperationOnEntity(OPERATION_DELETE, oldEntity);
        return Optional.ofNullable(entity);
    }

    private Entity logOperationOnEntity(String operation, Entity entity) {
        logger.info("Triggering expansion of resource based on file entry {}: {}",
                    operation,
                    attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(entity))
                        .orElseThrow());
        return entity;
    }
}

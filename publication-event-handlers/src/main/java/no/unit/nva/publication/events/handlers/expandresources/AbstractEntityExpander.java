package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Optional;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.model.business.Entity;
import nva.commons.apigateway.exceptions.NotFoundException;

/**
 * Abstract base class for {@link EntityExpander} implementations providing a default implementation for
 * {@link EntityExpander#expand(ResourceExpansionService, Entity, Entity)} that expands only on insertion/modification
 * (newEntity is not null). The class also provides a method for handling errors when expanding the entity using
 * {@link ResourceExpansionService}.
 */
public abstract class AbstractEntityExpander implements EntityExpander {

    @Override
    public Optional<ExpandedDataEntry> expand(ResourceExpansionService resourceExpansionService,
                                              Entity oldEntity,
                                              Entity newEntity) {
        if (isNull(newEntity)) {
            return Optional.empty();
        }

        return doExpand(resourceExpansionService, newEntity);
    }

    protected Optional<ExpandedDataEntry> doExpand(ResourceExpansionService resourceExpansionService, Entity entity) {
        try {
            return resourceExpansionService.expandEntry(entity, true);
        } catch (JsonProcessingException e) {
            throw new EntityExpansionException("Failed during serialization/deserialization", e);
        } catch (NotFoundException e) {
            throw new EntityExpansionException("Failed to look up data", e);
        }
    }
}

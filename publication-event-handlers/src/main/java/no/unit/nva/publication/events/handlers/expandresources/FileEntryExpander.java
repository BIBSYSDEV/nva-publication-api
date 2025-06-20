package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.nonNull;
import java.util.Optional;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;

/**
 * Expander for {@link FileEntry} entities.
 * <p>
 * Actually expands the owning {@link no.unit.nva.publication.model.business.Resource} entity both on insertion,
 * modification and deletion.
 * </p>
 */
public class FileEntryExpander extends AbstractEntityExpander {

    @Override
    public boolean canExpand(Class<? extends Entity> entityClass) {
        return FileEntry.class.isAssignableFrom(entityClass);
    }

    @Override
    public Optional<ExpandedDataEntry> expand(ResourceExpansionService resourceExpansionService,
                                              Entity oldEntity,
                                              Entity newEntity) {
        var entity = (FileEntry) (nonNull(newEntity) ? newEntity : oldEntity);
        return doExpand(resourceExpansionService, entity);
    }
}

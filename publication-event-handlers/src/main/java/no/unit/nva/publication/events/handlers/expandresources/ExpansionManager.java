package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.nonNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.model.business.Entity;

/**
 * Manager class for expanding entities.
 * <p>
 * Iterates through a list of configured {@link EntityExpander}'s and delegates to the first expander that can expand
 * the entity based on {@link EntityExpander#canExpand(Entity)}. The {@link EntityExpander}'s are responsible for
 * deciding whether to expand the entity, and optionally expanding it.
 * </p>
 */
public class ExpansionManager {

    private final List<EntityExpander> entityExpanders;
    private final ResourceExpansionService resourceExpansionService;

    public ExpansionManager(ResourceExpansionService resourceExpansionService, EntityExpander... entityExpanders) {
        this.resourceExpansionService = resourceExpansionService;
        this.entityExpanders = Collections.unmodifiableList(Arrays.asList(entityExpanders));
    }

    public Optional<ExpandedDataEntry> expand(Entity oldVersion, Entity newVersion) {
        var entity = nonNull(newVersion) ? newVersion : oldVersion;
        return entityExpanders.stream()
                   .filter(entityExpander -> entityExpander.canExpand(entity))
                   .findFirst()
                   .orElseThrow(() -> new NoEntityExpanderException(entity.getClass()))
                   .expand(resourceExpansionService, oldVersion, newVersion);
    }
}

package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.model.PublicationStatus.DELETED;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import java.util.List;
import java.util.Optional;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;

/**
 * Entity expander for resources.
 * <p>
 * Resources are only expanded on modification and when the new version of the resource has one of the following
 * statuses:
 * <ul>
 * <li>{@link PublicationStatus#PUBLISHED}</li>
 * <li>{@link PublicationStatus#PUBLISHED_METADATA}</li>
 * <li>{@link PublicationStatus#UNPUBLISHED}</li>
 * <li>{@link PublicationStatus#DELETED}</li>
 * <li>{@link PublicationStatus#DRAFT}</li>
 * </ul>
 * </p>
 */
public class ResourceExpander extends AbstractEntityExpander {

    private static final List<PublicationStatus> PUBLICATION_STATUSES_TO_BE_EXPANDED = List.of(PUBLISHED,
                                                                                               PUBLISHED_METADATA,
                                                                                               UNPUBLISHED, DELETED,
                                                                                               DRAFT);

    @Override
    public boolean canExpand(Entity entity) {
        return entity instanceof Resource;
    }

    @Override
    public Optional<ExpandedDataEntry> expand(ResourceExpansionService resourceExpansionService,
                                              Entity oldEntity,
                                              Entity newEntity) {

        if (newEntity instanceof Resource resource && shouldExpandResource(resource)) {
            return doExpand(resourceExpansionService, resource);
        }

        return Optional.empty();
    }

    private static boolean shouldExpandResource(Resource resource) {
        return PUBLICATION_STATUSES_TO_BE_EXPANDED.contains(resource.getStatus());
    }
}

package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.model.PublicationStatus.DELETED;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;

public class ResourceExpansionResolver implements EntityExpansionResolver {

    private static final List<PublicationStatus> PUBLICATION_STATUSES_TO_BE_EXPANDED = List.of(PUBLISHED,
                                                                                               PUBLISHED_METADATA,
                                                                                               UNPUBLISHED, DELETED,
                                                                                               DRAFT);

    @Override
    public Optional<Entity> resolveEntityToExpand(Entity oldEntity, Entity newEntity) {
        return newEntity instanceof Resource resource && shouldExpandResource(resource)
                   ? Optional.of(newEntity)
                   : Optional.empty();
    }

    private static boolean shouldExpandResource(Resource resource) {
        return PUBLICATION_STATUSES_TO_BE_EXPANDED.contains(resource.getStatus());
    }
}

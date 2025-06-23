package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import java.util.Optional;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;

public class TicketExpansionResolver implements EntityExpansionResolver {

    @Override
    public Optional<Entity> resolveEntityToExpand(Entity oldEntity, Entity newEntity) {
        if (newEntity instanceof DoiRequest doiRequest) {
            return doiRequestReadyForExpansion(doiRequest) ? Optional.of(doiRequest) : Optional.empty();
        }

        return nonNull(newEntity) ? Optional.of(newEntity) : Optional.empty();
    }

    private boolean doiRequestReadyForExpansion(DoiRequest doiRequest) {
        return PUBLISHED.equals(doiRequest.getResourceStatus());
    }
}

package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import java.util.Optional;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.TicketEntry;

/**
 * Expander for {@link TicketEntry} entities.
 * <p>
 * Actually expands all tickets on insertion/modification except for {@link DoiRequest} tickets that are only expanded
 * if the associated resource has the status {@link no.unit.nva.model.PublicationStatus#PUBLISHED}.
 * </p>
 */
public class TicketExpander extends AbstractEntityExpander {

    @Override
    public boolean canExpand(Entity entity) {
        return entity instanceof TicketEntry;
    }

    @Override
    public Optional<ExpandedDataEntry> expand(ResourceExpansionService resourceExpansionService,
                                              Entity oldEntity,
                                              Entity newEntity) {

        if (newEntity instanceof DoiRequest doiRequest) {
            return doiRequestReadyForExpansion(doiRequest)
                       ? doExpand(resourceExpansionService, doiRequest)
                       : Optional.empty();
        }

        if (nonNull(newEntity)) {
            return doExpand(resourceExpansionService, newEntity);
        }

        return Optional.empty();
    }

    private boolean doiRequestReadyForExpansion(DoiRequest doiRequest) {
        return PUBLISHED.equals(doiRequest.getResourceStatus());
    }
}

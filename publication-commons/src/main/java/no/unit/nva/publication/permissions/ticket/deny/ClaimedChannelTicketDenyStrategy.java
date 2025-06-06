package no.unit.nva.publication.permissions.ticket.deny;

import static no.unit.nva.model.TicketOperation.TRANSFER;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.ticket.TicketDenyStrategy;
import no.unit.nva.publication.permissions.ticket.TicketStrategyBase;

public class ClaimedChannelTicketDenyStrategy extends TicketStrategyBase implements TicketDenyStrategy {

    public ClaimedChannelTicketDenyStrategy(TicketEntry ticket, UserInstance userInstance, Resource resource) {
        super(ticket, userInstance, resource);
    }

    @Override
    public boolean deniesAction(TicketOperation operation) {
        return TRANSFER.equals(operation) && hasClaimedPublicationChannel();
    }

    private boolean hasClaimedPublicationChannel() {
        return resource.getPrioritizedClaimedPublicationChannelWithinScope().isPresent();
    }
}

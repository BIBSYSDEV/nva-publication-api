package no.unit.nva.publication.permissions.ticket.deny;

import static no.unit.nva.publication.model.business.TicketStatus.CLOSED;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.model.business.TicketStatus.NOT_APPLICABLE;
import static no.unit.nva.publication.model.business.TicketStatus.REMOVED;
import java.util.List;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.ticket.TicketDenyStrategy;
import no.unit.nva.publication.permissions.ticket.TicketStrategyBase;

public class FinalizedTicketDenyStrategy extends TicketStrategyBase implements TicketDenyStrategy {

    public FinalizedTicketDenyStrategy(TicketEntry ticket,
                                       UserInstance userInstance,
                                       Resource resource) {
        super(ticket, userInstance, resource);
    }

    @Override
    public boolean deniesAction(TicketOperation permission) {
        return List.of(CLOSED, COMPLETED, REMOVED, NOT_APPLICABLE).contains(ticket.getStatus());
    }
}

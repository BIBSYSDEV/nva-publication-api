package no.unit.nva.publication.permissions.ticket;

import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketStrategyBase {

    public static final Logger logger = LoggerFactory.getLogger(TicketStrategyBase.class);

    protected final TicketEntry ticket;
    protected final UserInstance userInstance;
    protected final Resource resource;

    protected TicketStrategyBase(TicketEntry ticket, UserInstance userInstance, Resource resource) {
        this.ticket = ticket;
        this.userInstance = userInstance;
        this.resource = resource;
    }
}

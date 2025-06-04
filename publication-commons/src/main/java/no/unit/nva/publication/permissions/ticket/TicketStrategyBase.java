package no.unit.nva.publication.permissions.ticket;

import static java.util.Objects.nonNull;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;
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

    public boolean canManageTicket() {
        return resource.isDegree()
                   ? hasAccessRight(MANAGE_DEGREE) || hasAccessRight(MANAGE_DEGREE_EMBARGO)
                   : hasAccessRight(MANAGE_PUBLISHING_REQUESTS);
    }

    protected boolean hasAccessRight(AccessRight accessRight) {
        return nonNull(userInstance) && userInstance.getAccessRights().contains(accessRight);
    }
}

package no.unit.nva.publication.permissions.ticket;

import static java.util.Objects.nonNull;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import java.util.Optional;
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

    public boolean canManagePublishingTicket() {
        var haveAccess = resource.isDegree()
                             ? hasAccessRight(MANAGE_DEGREE) || hasAccessRight(MANAGE_DEGREE_EMBARGO)
                             : hasAccessRight(MANAGE_PUBLISHING_REQUESTS);

        return haveAccess && userBelongsToReceivingTopLevelOrg();
    }

    public boolean canManageSupportTicket() {
        return hasAccessRight(SUPPORT) && userBelongsToReceivingTopLevelOrg();
    }

    public boolean canManageDoiTicket() {
        return hasAccessRight(MANAGE_DOI) && userBelongsToReceivingTopLevelOrg();
    }

    protected boolean hasAccessRight(AccessRight accessRight) {
        return nonNull(userInstance) && userInstance.getAccessRights().contains(accessRight);
    }

    public boolean userBelongsToReceivingTopLevelOrg() {
        return Optional.ofNullable(userInstance)
                   .map(UserInstance::getTopLevelOrgCristinId)
                   .map(userTopLevelOrg -> userTopLevelOrg.equals(ticket.getReceivingOrganizationDetails()
                                                                      .topLevelOrganizationId()))
                   .orElse(false);
    }
}

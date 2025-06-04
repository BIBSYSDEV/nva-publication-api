package no.unit.nva.publication.permissions.ticket.grant;

import static no.unit.nva.model.TicketOperation.TRANSFER;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.permissions.ticket.TicketGrantStrategy;
import no.unit.nva.publication.permissions.ticket.TicketStrategyBase;

public class TransferTicketGrantStrategy extends TicketStrategyBase implements TicketGrantStrategy {
    private final PublicationPermissions publicationPermissions;

    public TransferTicketGrantStrategy(TicketEntry ticket, UserInstance userInstance, Resource resource,
                                      PublicationPermissions publicationPermissions) {
        super(ticket, userInstance, resource);
        this.publicationPermissions = publicationPermissions;
    }

    @Override
    public boolean allowsAction(TicketOperation permission) {
        if (permission.equals(TRANSFER)) {
            return publicationPermissions.allowsAction(PublicationOperation.APPROVE_FILES)
                   && userBelongsToReceivingTopLevelOrg();
        }

        return false;
    }

    private boolean userBelongsToReceivingTopLevelOrg() {
        return ticket.getReceivingOrganizationDetails()
                   .topLevelOrganizationId()
                   .equals(userInstance.getTopLevelOrgCristinId());
    }
}

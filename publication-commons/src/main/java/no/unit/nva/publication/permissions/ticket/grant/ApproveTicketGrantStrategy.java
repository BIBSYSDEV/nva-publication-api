package no.unit.nva.publication.permissions.ticket.grant;

import static no.unit.nva.model.TicketOperation.APPROVE;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.ticket.TicketGrantStrategy;
import no.unit.nva.publication.permissions.ticket.TicketStrategyBase;

public class ApproveTicketGrantStrategy extends TicketStrategyBase implements TicketGrantStrategy {

    public ApproveTicketGrantStrategy(TicketEntry ticket, UserInstance userInstance, Resource resource) {
        super(ticket, userInstance, resource);
    }

    @Override
    public boolean allowsAction(TicketOperation permission) {
        if (permission.equals(APPROVE)) {
            if (ticket instanceof FilesApprovalEntry) {
                return canManagePublishingTicket();
            }
            if (ticket instanceof DoiRequest) {
                return canManageDoiTicket();
            }
            if (ticket instanceof GeneralSupportRequest) {
                return canManageSupportTicket();
            }
        }

        return false;
    }
}

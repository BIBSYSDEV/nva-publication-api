package no.unit.nva.publication.permissions.ticket.grant;

import static no.unit.nva.model.TicketOperation.TRANSFER;
import static no.unit.nva.publication.utils.CuratingInstitutionsExtractor.getCuratingInstitutionsIdList;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.ticket.TicketGrantStrategy;
import no.unit.nva.publication.permissions.ticket.TicketStrategyBase;

public class TransferTicketGrantStrategy extends TicketStrategyBase implements TicketGrantStrategy {

    public TransferTicketGrantStrategy(TicketEntry ticket, UserInstance userInstance, Resource resource) {
        super(ticket, userInstance, resource);
    }

    @Override
    public boolean allowsAction(TicketOperation permission) {
        if (!permission.equals(TRANSFER)) {
            return false;
        }

        return ticket instanceof FilesApprovalEntry && canManagePublishingTicket() && anyValidReceivers();
    }

    private boolean anyValidReceivers() {
        return getCuratingInstitutionsIdList(resource).stream().anyMatch(orgId -> !userInstance.getTopLevelOrgCristinId().equals(orgId));
    }
}

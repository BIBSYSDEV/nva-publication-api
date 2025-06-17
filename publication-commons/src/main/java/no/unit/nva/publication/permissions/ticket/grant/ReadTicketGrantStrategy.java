package no.unit.nva.publication.permissions.ticket.grant;

import static no.unit.nva.model.TicketOperation.READ;
import java.util.Optional;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.permissions.ticket.TicketGrantStrategy;
import no.unit.nva.publication.permissions.ticket.TicketStrategyBase;

public class ReadTicketGrantStrategy extends TicketStrategyBase implements TicketGrantStrategy {

    private final PublicationPermissions publicationPermissions;

    public ReadTicketGrantStrategy(TicketEntry ticket, UserInstance userInstance, Resource resource,
                                   PublicationPermissions publicationPermissions) {
        super(ticket, userInstance, resource);
        this.publicationPermissions = publicationPermissions;
    }

    @Override
    public boolean allowsAction(TicketOperation permission) {
        return permission.equals(READ) && (userOwnsTicket()
                                           || userBelongsToReceivingTopLevelOrg()
                                           || allowedToSeeLimitedSetOfCompletedTickets());
    }

    private boolean userOwnsTicket() {
        return Optional.ofNullable(userInstance)
                   .map(UserInstance::getUser).map(user -> user.equals(ticket.getOwner())).orElse(false);
    }

    private boolean allowedToSeeLimitedSetOfCompletedTickets() {
        if (ticket instanceof DoiRequest || ticket instanceof FilesApprovalEntry) {
            return publicationPermissions.allowsAction(PublicationOperation.PARTIAL_UPDATE) && !ticket.isPending();
        }
        return false;
    }
}

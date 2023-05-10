package no.unit.nva.expansion.utils;

import static java.util.Objects.isNull;
import no.unit.nva.expansion.model.ExpandedTicketStatus;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;

public final class ExpandedTicketStatusMapper {

    private ExpandedTicketStatusMapper() {
    }

    public static ExpandedTicketStatus getExpandedTicketStatus(TicketEntry ticketEntry) {
        if (isPending(ticketEntry)) {
            return getStatusNewIfTicketIsUnassigned(ticketEntry);
        }
        return ExpandedTicketStatus.parse(ticketEntry.getStatusString());
    }

    static ExpandedTicketStatus getStatusNewIfTicketIsUnassigned(TicketEntry ticketEntry) {
        return isNull(ticketEntry.getAssignee()) ? ExpandedTicketStatus.NEW : ExpandedTicketStatus.PENDING;
    }

    private static boolean isPending(TicketEntry ticketEntry) {
        return TicketStatus.PENDING.equals(ticketEntry.getStatus());
    }
}
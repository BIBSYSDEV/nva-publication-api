package no.unit.nva.expansion.utils;

import no.unit.nva.expansion.model.ExpandedTicketStatus;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;


import static java.util.Objects.isNull;

public final class ExpandedTicketStatusMapper {

    private ExpandedTicketStatusMapper() {
    }

    public static ExpandedTicketStatus getExpandedTicketStatus(TicketEntry ticketEntry) {
        TicketStatus status = ticketEntry.getStatus();
        switch (status) {
            case COMPLETED:
                return ExpandedTicketStatus.COMPLETED;
            case CLOSED:
                return ExpandedTicketStatus.CLOSED;
            default:
                return getNewTicketStatus(ticketEntry);
        }
    }


    static ExpandedTicketStatus getNewTicketStatus(TicketEntry ticketEntry) {
        return isNull(ticketEntry.getAssignee()) ? ExpandedTicketStatus.NEW : ExpandedTicketStatus.PENDING;
    }


}
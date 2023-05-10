package no.unit.nva.expansion.utils;

import no.unit.nva.expansion.model.ExpandedTicketStatus;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;

import java.util.Objects;

public class ExpandedTicketStatusQueries {
    public static ExpandedTicketStatus getExpandedTicketStatus(TicketEntry ticketEntry) {
        TicketStatus status = ticketEntry.getStatus();
        switch (status) {
            case PENDING:
                return getNewTicketStatus(ticketEntry);
            case COMPLETED:
                return ExpandedTicketStatus.COMPLETED;
            case CLOSED:
                return ExpandedTicketStatus.CLOSED;
            default:
                return ExpandedTicketStatus.PENDING;
        }
    }

    static ExpandedTicketStatus getNewTicketStatus(TicketEntry doiRequest) {
        if (Objects.isNull(doiRequest.getAssignee())) {
            return ExpandedTicketStatus.NEW;
        } else {
            return ExpandedTicketStatus.PENDING;
        }
    }


    public static TicketStatus getTicketStatus(ExpandedTicketStatus expandedTicketStatus) {
        if (expandedTicketStatus.equals(ExpandedTicketStatus.NEW)) {
            return TicketStatus.PENDING;
        } else {
            return TicketStatus.parse(expandedTicketStatus.toString());
        }
    }
}
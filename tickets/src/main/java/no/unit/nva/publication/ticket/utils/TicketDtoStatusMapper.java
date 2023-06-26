package no.unit.nva.publication.ticket.utils;

import static java.util.Objects.isNull;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.ticket.TicketDtoStatus;

public final class TicketDtoStatusMapper {

    private TicketDtoStatusMapper() {
    }

    public static TicketDtoStatus getTicketDtoStatus(TicketEntry ticketEntry) {
        if (isPending(ticketEntry)) {
            return getStatusNewIfTicketIsUnassigned(ticketEntry);
        }
        return TicketDtoStatus.parse(ticketEntry.getStatusString());
    }

    static TicketDtoStatus getStatusNewIfTicketIsUnassigned(TicketEntry ticketEntry) {
        return isNull(ticketEntry.getAssignee()) ? TicketDtoStatus.NEW : TicketDtoStatus.PENDING;
    }

    private static boolean isPending(TicketEntry ticketEntry) {
        return TicketStatus.PENDING.equals(ticketEntry.getStatus());
    }
}
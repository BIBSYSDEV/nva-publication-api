package no.unit.nva.publication.permissions.ticket;

import no.unit.nva.model.TicketOperation;

public interface TicketDenyStrategy {
    boolean deniesAction(TicketOperation permission);
}

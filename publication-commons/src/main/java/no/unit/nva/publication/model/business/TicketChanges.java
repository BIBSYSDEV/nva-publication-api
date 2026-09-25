package no.unit.nva.publication.model.business;

import static java.util.Collections.emptyList;

import java.util.Collection;

/** Tickets to write together with their resource: existing tickets that changed, and new ones. */
public record TicketChanges(
    Collection<TicketEntry> changedTickets, Collection<TicketEntry> newTickets) {

  public static TicketChanges none() {
    return new TicketChanges(emptyList(), emptyList());
  }
}

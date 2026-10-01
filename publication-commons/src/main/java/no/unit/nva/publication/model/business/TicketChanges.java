package no.unit.nva.publication.model.business;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/** Tickets to write together with their resource: existing tickets that changed, and new ones. */
public record TicketChanges(
    Collection<TicketEntry> changedTickets, Collection<TicketEntry> newTickets) {

  public static TicketChanges none() {
    return new TicketChanges(emptyList(), emptyList());
  }

  public TicketChanges combinedWith(TicketChanges otherChanges) {
    return new TicketChanges(
        concat(changedTickets, otherChanges.changedTickets()),
        concat(newTickets, otherChanges.newTickets()));
  }

  private static List<TicketEntry> concat(
      Collection<TicketEntry> tickets, Collection<TicketEntry> otherTickets) {
    return Stream.concat(tickets.stream(), otherTickets.stream()).toList();
  }
}

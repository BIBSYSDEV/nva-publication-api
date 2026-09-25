package no.unit.nva.publication.model.business;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;

/** Tickets to write together with their resource: existing tickets that changed, and new ones. */
public record TicketChanges(
    Collection<TicketEntry> changedTickets, Collection<TicketEntry> newTickets) {

  public static TicketChanges none() {
    return new TicketChanges(emptyList(), emptyList());
  }

  /**
   * Combines this change with a later one. When both change the same ticket, identified by its
   * identifier, the version from {@code laterChanges} is kept, so it must build on this change.
   */
  public TicketChanges followedBy(TicketChanges laterChanges) {
    return new TicketChanges(
        latestVersionOfEachTicket(changedTickets, laterChanges.changedTickets()),
        Stream.concat(newTickets.stream(), laterChanges.newTickets().stream()).toList());
  }

  private static Collection<TicketEntry> latestVersionOfEachTicket(
      Collection<TicketEntry> tickets, Collection<TicketEntry> laterTickets) {
    var ticketsByIdentifier = new LinkedHashMap<SortableIdentifier, TicketEntry>();
    Stream.concat(tickets.stream(), laterTickets.stream())
        .forEach(ticket -> ticketsByIdentifier.put(ticket.getIdentifier(), ticket));
    return List.copyOf(ticketsByIdentifier.values());
  }
}

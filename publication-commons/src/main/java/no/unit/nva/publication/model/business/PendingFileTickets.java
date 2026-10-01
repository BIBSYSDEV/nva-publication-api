package no.unit.nva.publication.model.business;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.TicketStatus.NOT_APPLICABLE;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.FilesApprovalTickets;

/**
 * Works out the file approval tickets a resource needs from its current state: one ticket per
 * uploading institution, covering that institution's pending files. An existing ticket that is set
 * aside or still pending is reused when it is identical to the ticket that would be created, which
 * keeps its assignee, messages and sub-unit; other pending tickets are set aside. Nothing is
 * persisted, but reused and set-aside tickets change status in place.
 */
final class PendingFileTickets {

  private PendingFileTickets() {}

  static TicketChanges changesFor(
      Resource resource, Collection<TicketEntry> tickets, CustomerApiClient customerApiClient) {
    var reusableTickets = reusableTickets(tickets);
    var reusedTicketIdentifiers = new HashSet<SortableIdentifier>();
    var newTickets = new ArrayList<TicketEntry>();

    for (var fileEntries : pendingFilesByInstitution(resource)) {
      var ticket = ticketFor(resource, fileEntries, customerApiClient);
      reusableTickets.stream()
          .filter(candidate -> !reusedTicketIdentifiers.contains(candidate.getIdentifier()))
          .filter(candidate -> isIdentical(candidate, ticket))
          .findFirst()
          .ifPresentOrElse(
              reusedTicket -> reusedTicketIdentifiers.add(reusedTicket.getIdentifier()),
              () -> newTickets.add(ticket));
    }
    return new TicketChanges(statusChanges(reusableTickets, reusedTicketIdentifiers), newTickets);
  }

  /** Set-aside and pending file approval tickets, most recently modified first. */
  private static List<FilesApprovalEntry> reusableTickets(Collection<TicketEntry> tickets) {
    return tickets.stream()
        .filter(FilesApprovalEntry.class::isInstance)
        .map(FilesApprovalEntry.class::cast)
        .filter(ticket -> PENDING == ticket.getStatus() || NOT_APPLICABLE == ticket.getStatus())
        .sorted(comparing(TicketEntry::getModifiedDate, nullsLast(reverseOrder())))
        .toList();
  }

  private static Collection<List<FileEntry>> pendingFilesByInstitution(Resource resource) {
    return resource.getFileEntries().stream()
        .filter(fileEntry -> fileEntry.getFile().isPending())
        .collect(
            Collectors.groupingBy(
                fileEntry -> Optional.ofNullable(fileEntry.getOwnerAffiliation())))
        .values();
  }

  private static FilesApprovalEntry ticketFor(
      Resource resource, List<FileEntry> fileEntries, CustomerApiClient customerApiClient) {
    var uploader = UserInstance.fromFileEntry(fileEntries.getFirst());
    var customer = customerApiClient.fetch(uploader.getCustomerId());
    var files = fileEntries.stream().map(FileEntry::getFile).collect(Collectors.toSet());
    return new FilesApprovalTickets(uploader, customer).newTicket(resource, files);
  }

  /**
   * The sub-unit is left out on purpose: a new ticket is routed to the whole institution, so
   * comparing it would never match, and keeping the existing ticket's sub-unit is the point.
   */
  private static boolean isIdentical(FilesApprovalEntry existing, FilesApprovalEntry candidate) {
    return candidate.isPending()
        && existing.getClass().equals(candidate.getClass())
        && Objects.equals(existing.getOwnerAffiliation(), candidate.getOwnerAffiliation())
        && hasSameReceivingInstitution(existing, candidate)
        && existing.getWorkflow() == candidate.getWorkflow()
        && existing.getFilesForApproval().equals(candidate.getFilesForApproval());
  }

  private static boolean hasSameReceivingInstitution(
      FilesApprovalEntry existing, FilesApprovalEntry candidate) {
    var existingReceiver = existing.getReceivingOrganizationDetails();
    var candidateReceiver = candidate.getReceivingOrganizationDetails();
    return nonNull(existingReceiver)
        && nonNull(candidateReceiver)
        && Objects.equals(
            existingReceiver.topLevelOrganizationId(), candidateReceiver.topLevelOrganizationId())
        && Objects.equals(
            existingReceiver.influencingChannelClaim(),
            candidateReceiver.influencingChannelClaim());
  }

  private static List<TicketEntry> statusChanges(
      List<FilesApprovalEntry> reusableTickets, Set<SortableIdentifier> reusedTicketIdentifiers) {
    var changedTickets = new ArrayList<TicketEntry>();
    for (var ticket : reusableTickets) {
      if (reusedTicketIdentifiers.contains(ticket.getIdentifier())) {
        ticket.setStatus(PENDING);
        changedTickets.add(ticket);
      } else if (ticket.isPending()) {
        ticket.setStatus(NOT_APPLICABLE);
        changedTickets.add(ticket);
      }
    }
    return changedTickets;
  }
}

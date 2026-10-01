package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.TicketStatus.NOT_APPLICABLE;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.FilesApprovalTickets;

/**
 * Works out the file approval tickets a resource needs from its current state: one new ticket per
 * uploading institution, covering that institution's pending files. File approval tickets that are
 * still pending are set aside, since the new tickets replace them. Nothing is persisted, but
 * set-aside tickets change status in place.
 */
final class PendingFileTickets {

  private PendingFileTickets() {}

  static TicketChanges changesFor(
      Resource resource, Collection<TicketEntry> tickets, CustomerApiClient customerApiClient) {
    var newTickets =
        pendingFilesByInstitution(resource).stream()
            .map(fileEntries -> ticketFor(resource, fileEntries, customerApiClient))
            .map(TicketEntry.class::cast)
            .toList();
    var replacedTickets = pendingFileApprovalTickets(tickets);
    replacedTickets.forEach(ticket -> ticket.setStatus(NOT_APPLICABLE));
    return new TicketChanges(replacedTickets, newTickets);
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

  private static List<TicketEntry> pendingFileApprovalTickets(Collection<TicketEntry> tickets) {
    return tickets.stream()
        .filter(FilesApprovalEntry.class::isInstance)
        .filter(TicketEntry::isPending)
        .toList();
  }
}

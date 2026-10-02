package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.TicketStatus.NOT_APPLICABLE;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.FilesApprovalTickets;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketChanges;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;

/**
 * Works out the file approval tickets a resource needs from its current state: one new ticket per
 * uploading institution, covering that institution's pending files. A file without a known uploader
 * institution counts as uploaded by the resource owner. File approval tickets that are still
 * pending are set aside, since the new tickets replace them. Nothing is persisted, but set-aside
 * tickets change status in place.
 */
final class PendingFileTickets {

  private PendingFileTickets() {}

  static TicketChanges changesFor(
      Resource resource, Collection<TicketEntry> tickets, CustomerApiClient customerApiClient) {
    var newTickets =
        pendingFilesByUploaderInstitution(resource).stream()
            .map(uploaders -> ticketFor(resource, uploaders, customerApiClient))
            .toList();
    var replacedTickets = pendingFileApprovalTickets(tickets);
    replacedTickets.forEach(ticket -> ticket.setStatus(NOT_APPLICABLE));
    return new TicketChanges(replacedTickets, newTickets);
  }

  /** Customer and top-level institution are both part of the key, since both decide the ticket. */
  private static Collection<List<PendingFile>> pendingFilesByUploaderInstitution(
      Resource resource) {
    return resource.getFileEntries().stream()
        .filter(fileEntry -> fileEntry.getFile().isPending())
        .map(fileEntry -> new PendingFile(fileEntry, uploaderOf(resource, fileEntry)))
        .collect(Collectors.groupingBy(PendingFile::uploaderInstitution))
        .values();
  }

  private static UserInstance uploaderOf(Resource resource, FileEntry fileEntry) {
    return nonNull(fileEntry.getCustomerId()) && nonNull(fileEntry.getOwnerAffiliation())
        ? UserInstance.fromFileEntry(fileEntry)
        : UserInstance.fromPublication(resource.toPublication());
  }

  private static TicketEntry ticketFor(
      Resource resource, List<PendingFile> pendingFiles, CustomerApiClient customerApiClient) {
    var uploader = pendingFiles.getFirst().uploader();
    var customer = customerApiClient.fetch(uploader.getCustomerId());
    var files =
        pendingFiles.stream()
            .map(pendingFile -> pendingFile.fileEntry().getFile())
            .collect(Collectors.toSet());
    return new FilesApprovalTickets(uploader, customer).newTicket(resource, files);
  }

  private static List<TicketEntry> pendingFileApprovalTickets(Collection<TicketEntry> tickets) {
    return tickets.stream()
        .filter(FilesApprovalEntry.class::isInstance)
        .filter(TicketEntry::isPending)
        .toList();
  }

  private record PendingFile(FileEntry fileEntry, UserInstance uploader) {

    private UploaderInstitution uploaderInstitution() {
      return new UploaderInstitution(uploader.getCustomerId(), uploader.getTopLevelOrgCristinId());
    }
  }

  private record UploaderInstitution(URI customerId, URI topLevelOrganizationId) {}
}

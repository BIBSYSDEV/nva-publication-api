package no.unit.nva.publication.model.business;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.FilesApprovalTickets;

/**
 * Pending files can lack a pending approval ticket, for example when uploaded while the resource
 * was unpublished. Such files are added to their uploading institution's pending ticket, or get a
 * new ticket when the institution has none. Nothing is persisted.
 */
final class UncoveredFileTickets {

  private UncoveredFileTickets() {}

  /**
   * @param pendingTickets the resource's pending tickets
   */
  static TicketChanges changesFor(
      Resource resource,
      Collection<TicketEntry> pendingTickets,
      CustomerApiClient customerApiClient) {
    var pendingFilesApprovalTickets = filesApprovalTicketsAmong(pendingTickets);
    var changedTickets = new ArrayList<TicketEntry>();
    var newTickets = new ArrayList<TicketEntry>();

    uncoveredPendingFilesByInstitution(resource, pendingFilesApprovalTickets)
        .forEach(
            (institution, fileEntries) -> {
              var uploader = UserInstance.fromFileEntry(fileEntries.getFirst());
              var customer = customerApiClient.fetch(uploader.getCustomerId());
              var filesApprovalTickets = new FilesApprovalTickets(uploader, customer);
              var files = fileEntries.stream().map(FileEntry::getFile).collect(Collectors.toSet());

              institutionTicket(pendingFilesApprovalTickets, institution)
                  .ifPresentOrElse(
                      ticket ->
                          changedTickets.add(
                              filesApprovalTickets.withAddedFiles(resource, ticket, files)),
                      () -> newTickets.add(filesApprovalTickets.newTicket(resource, files)));
            });
    return new TicketChanges(changedTickets, newTickets);
  }

  private static List<FilesApprovalEntry> filesApprovalTicketsAmong(
      Collection<TicketEntry> tickets) {
    return tickets.stream()
        .filter(FilesApprovalEntry.class::isInstance)
        .map(FilesApprovalEntry.class::cast)
        .toList();
  }

  private static Map<Optional<URI>, List<FileEntry>> uncoveredPendingFilesByInstitution(
      Resource resource, List<FilesApprovalEntry> pendingTickets) {
    var coveredFileIdentifiers = filesCoveredBy(pendingTickets);
    return resource.getFileEntries().stream()
        .filter(fileEntry -> fileEntry.getFile().isPending())
        .filter(fileEntry -> !coveredFileIdentifiers.contains(fileEntry.getFile().getIdentifier()))
        .collect(
            Collectors.groupingBy(
                fileEntry -> Optional.ofNullable(fileEntry.getOwnerAffiliation())));
  }

  private static Set<UUID> filesCoveredBy(List<FilesApprovalEntry> tickets) {
    return tickets.stream()
        .flatMap(ticket -> ticket.getFilesForApproval().stream())
        .map(File::getIdentifier)
        .collect(Collectors.toSet());
  }

  private static Optional<FilesApprovalEntry> institutionTicket(
      List<FilesApprovalEntry> pendingTickets, Optional<URI> institution) {
    return institution.flatMap(
        institutionId ->
            pendingTickets.stream()
                .filter(ticket -> institutionId.equals(ticket.getOwnerAffiliation()))
                .findFirst());
  }
}

package no.unit.nva.publication.update;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.FilesApprovalTickets;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public final class PublishingRequestResolver {

  private final TicketService ticketService;
  private final ResourceService resourceService;
  private final UserInstance userInstance;
  private final Customer customer;

  public PublishingRequestResolver(
      ResourceService resourceService,
      TicketService ticketService,
      UserInstance userInstance,
      Customer customer) {
    this.ticketService = ticketService;
    this.resourceService = resourceService;
    this.userInstance = userInstance;
    this.customer = customer;
  }

  public void resolve(Resource oldImage, Resource newImage) throws ApiGatewayException {
    if (isAlreadyPublished(oldImage)) {
      handlePublishingRequest(oldImage, newImage);
    }
  }

  private static Stream<File> getPendingFiles(Resource resource) {
    return resource.getFiles().stream().filter(File::isPending);
  }

  private static boolean isPending(TicketEntry ticketEntry) {
    return TicketStatus.PENDING == ticketEntry.getStatus();
  }

  private void handlePublishingRequest(Resource oldImage, Resource newImage)
      throws ApiGatewayException {
    var filesApprovalEntries =
        fetchPendingFileApprovalEntryForUserInstitutionOrWithFilesForApprovalWhenDegree(oldImage);
    if (filesApprovalEntries.isEmpty()) {
      createPublishingRequestOnFileUpdate(oldImage, newImage);
      return;
    }
    if (thereAreNoPendingFiles(newImage)) {
      autoCompletePendingPublishingRequestsIfNeeded(newImage, filesApprovalEntries);
    } else {
      updateFilesForApproval(oldImage, newImage, filesApprovalEntries);
    }
  }

  private void autoCompletePendingPublishingRequestsIfNeeded(
      Resource resource, List<FilesApprovalEntry> filesApprovalEntries) {
    filesApprovalEntries.forEach(
        ticket ->
            ticket.complete(resource.toPublication(), userInstance).persistUpdate(ticketService));
  }

  private boolean thereAreNoPendingFiles(Resource resource) {
    return resource.getAssociatedArtifacts().stream()
        .noneMatch(
            associatedArtifact -> associatedArtifact instanceof File file && file.isPending());
  }

  private List<FilesApprovalEntry>
      fetchPendingFileApprovalEntryForUserInstitutionOrWithFilesForApprovalWhenDegree(
          Resource resource) {
    return resourceService
        .fetchAllTicketsForResource(resource)
        .filter(FilesApprovalEntry.class::isInstance)
        .map(FilesApprovalEntry.class::cast)
        .filter(ticketEntry -> shouldIncludeEntry(resource, ticketEntry, userInstance))
        .filter(PublishingRequestResolver::isPending)
        .toList();
  }

  private boolean shouldIncludeEntry(
      Resource resource, FilesApprovalEntry ticketEntry, UserInstance userInstance) {
    return resource.getPrioritizedClaimedPublicationChannelWithinScope().isPresent()
            && !ticketEntry.getFilesForApproval().isEmpty()
        || ticketEntry.hasSameOwnerAffiliationAs(userInstance);
  }

  private void createPublishingRequestOnFileUpdate(Resource oldImage, Resource newImage)
      throws ApiGatewayException {
    if (containsNewPublishableFiles(oldImage, newImage)) {
      persistPendingPublishingRequest(oldImage, newImage);
    }
  }

  private void persistPendingPublishingRequest(Resource oldImage, Resource newImage)
      throws ApiGatewayException {
    var files = getNewPendingFiles(oldImage, newImage).collect(Collectors.toSet());
    new FilesApprovalTickets(userInstance, customer)
        .newTicket(newImage, files)
        .persistNewTicket(ticketService, newImage.toPublication());
  }

  private boolean containsNewPublishableFiles(Resource oldImage, Resource newImage) {
    return getNewPendingFiles(oldImage, newImage).findAny().isPresent();
  }

  private void updateFilesForApproval(
      Resource oldImage, Resource newImage, List<FilesApprovalEntry> filesApprovalEntries) {
    filesApprovalEntries.forEach(
        filesApprovalEntry -> updatePublishingRequest(oldImage, newImage, filesApprovalEntry));
  }

  private Stream<File> prepareFilesForApproval(
      Resource oldImage, Resource newImage, FilesApprovalEntry filesApprovalEntry) {
    // get updated files on the ticket
    var updatedTicketFiles = getUpdatedTicketFiles(filesApprovalEntry, newImage);
    // start with a list of updated files on the ticket
    var files = new ArrayList<>(updatedTicketFiles);
    // add new files that should be on the ticket (we assume all new files belongs to the current
    // ticket)
    files.addAll(getNewPendingFiles(oldImage, newImage).toList());

    return files.stream();
  }

  private static Set<File> getUpdatedTicketFiles(
      FilesApprovalEntry filesApprovalEntry, Resource newImage) {
    return filesApprovalEntry.getFilesForApproval().stream()
        .map(
            fileForApproval ->
                newImage.getFileByIdentifier(fileForApproval.getIdentifier()).orElse(null))
        .filter(Objects::nonNull)
        .filter(File::isPending)
        .collect(Collectors.toSet());
  }

  private Stream<File> getNewPendingFiles(Resource oldImage, Resource newImage) {
    var existingPendingFiles = getPendingFiles(oldImage).toList();
    var newPendingFiles = new ArrayList<>(getPendingFiles(newImage).toList());
    newPendingFiles.removeIf(
        newFile ->
            existingPendingFiles.stream()
                .map(File::getIdentifier)
                .anyMatch(oldFile -> oldFile.equals(newFile.getIdentifier())));
    return newPendingFiles.stream();
  }

  private void updatePublishingRequest(
      Resource oldImage, Resource newImage, FilesApprovalEntry filesApprovalEntry) {
    var files =
        prepareFilesForApproval(oldImage, newImage, filesApprovalEntry).collect(Collectors.toSet());
    new FilesApprovalTickets(userInstance, customer)
        .withFiles(newImage, filesApprovalEntry, files)
        .persistUpdate(ticketService);
  }

  private boolean isAlreadyPublished(Resource resource) {
    var status = resource.getStatus();
    return PUBLISHED == status || PUBLISHED_METADATA == status;
  }
}

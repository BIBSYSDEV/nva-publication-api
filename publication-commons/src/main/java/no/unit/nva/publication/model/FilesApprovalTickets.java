package no.unit.nva.publication.model;

import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;

import java.util.Set;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;

/**
 * Builds new file approval tickets, and sets the files on existing ones, on behalf of the user who
 * uploaded the files, following the publishing workflow of that user's customer. Nothing is
 * persisted. Setting files changes the given ticket in place; when the workflow publishes files
 * automatically, the returned ticket is a completed copy instead.
 */
public final class FilesApprovalTickets {

  private final UserInstance uploader;
  private final PublishingWorkflow workflow;

  public FilesApprovalTickets(UserInstance uploader, Customer customer) {
    this.uploader = uploader;
    this.workflow = PublishingWorkflow.lookUp(customer.getPublicationWorkflow());
  }

  public FilesApprovalEntry newTicket(Resource resource, Set<File> files) {
    return resource.isDegree()
        ? newFilesApprovalThesis(resource, files)
        : newPublishingRequest(resource, files);
  }

  /** Replaces the files on the ticket. */
  public FilesApprovalEntry withFiles(
      Resource resource, FilesApprovalEntry ticket, Set<File> files) {
    return completeIfAutoPublishing(resource, ticket.withFilesForApproval(files));
  }

  private FilesApprovalEntry newPublishingRequest(Resource resource, Set<File> files) {
    return completeIfAutoPublishing(
        resource,
        PublishingRequestCase.createWithFilesForApproval(resource, uploader, workflow, files));
  }

  private FilesApprovalEntry newFilesApprovalThesis(Resource resource, Set<File> files) {
    var channelClaimOfOtherInstitution =
        resource
            .getPrioritizedClaimedPublicationChannelWithinScope()
            .filter(
                channelClaim ->
                    !channelClaim.getOrganizationId().equals(uploader.getTopLevelOrgCristinId()));
    var filesApprovalThesis =
        channelClaimOfOtherInstitution
            .map(
                channelClaim ->
                    FilesApprovalThesis.createForChannelOwningInstitution(
                        resource,
                        uploader,
                        channelClaim.getOrganizationId(),
                        channelClaim.getIdentifier(),
                        workflow))
            .orElseGet(
                () -> FilesApprovalThesis.createForUserInstitution(resource, uploader, workflow));
    return filesApprovalThesis.withFilesForApproval(files);
  }

  private FilesApprovalEntry completeIfAutoPublishing(
      Resource resource, FilesApprovalEntry ticket) {
    return REGISTRATOR_PUBLISHES_METADATA_AND_FILES == workflow
        ? ticket.complete(resource.toPublication(), uploader)
        : ticket;
  }
}

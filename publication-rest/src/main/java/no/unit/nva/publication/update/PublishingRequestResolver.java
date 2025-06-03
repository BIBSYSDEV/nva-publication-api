package no.unit.nva.publication.update;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.publication.model.business.PublishingRequestCase.createWithFilesForApproval;
import static no.unit.nva.publication.model.business.PublishingWorkflow.lookUp;
import static nva.commons.core.attempt.Try.attempt;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
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

    private boolean customerAllowsPublishingMetadataAndFiles() {
        return PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES
                   .getValue()
                   .equals(customer.getPublicationWorkflow());
    }

    private static Stream<File> getPendingFiles(Resource resource) {
        return resource.getFiles().stream().filter(PendingFile.class::isInstance);
    }

    private static boolean isPending(TicketEntry ticketEntry) {
        return TicketStatus.PENDING.equals(ticketEntry.getStatus());
    }

    private void handlePublishingRequest(Resource oldImage, Resource newImage) throws ApiGatewayException {
        var filesApprovalEntries = fetchPendingFileApprovalEntryForUserInstitutionOrWithFilesForApprovalWhenDegree(oldImage);
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
            ticket -> ticket.complete(resource.toPublication(), userInstance).persistUpdate(ticketService));
    }

    private boolean thereAreNoPendingFiles(Resource resource) {
        return resource.getAssociatedArtifacts().stream()
                   .noneMatch(PendingFile.class::isInstance);
    }

    private List<FilesApprovalEntry> fetchPendingFileApprovalEntryForUserInstitutionOrWithFilesForApprovalWhenDegree(
        Resource resource) {
        return resourceService
                   .fetchAllTicketsForResource(resource)
                   .filter(FilesApprovalEntry.class::isInstance)
                   .map(FilesApprovalEntry.class::cast)
                   .filter(ticketEntry -> shouldIncludeEntry(resource, ticketEntry, userInstance))
                   .filter(PublishingRequestResolver::isPending)
                   .toList();
    }

    private boolean shouldIncludeEntry(Resource resource, FilesApprovalEntry ticketEntry, UserInstance userInstance) {
        return resource.getPrioritizedClaimedPublicationChannelWithinScope().isPresent() && !ticketEntry.getFilesForApproval().isEmpty()
               || ticketEntry.hasSameOwnerAffiliationAs(userInstance);
    }

    private void createPublishingRequestOnFileUpdate(Resource oldImage, Resource newImage)
        throws ApiGatewayException {
        if (containsNewPublishableFiles(oldImage, newImage)) {
            persistPendingPublishingRequest(oldImage, newImage);
        }
    }

    private TicketEntry persistPublishingRequest(
        Resource newImage, PublishingRequestCase publishingRequest)
        throws ApiGatewayException {
        return customerAllowsPublishingMetadataAndFiles()
                   ? publishingRequest
                         .approveFiles()
                         .persistAutoComplete(ticketService, newImage.toPublication(), userInstance)
                   : publishingRequest.persistNewTicket(ticketService);
    }

    private void persistPendingPublishingRequest(Resource oldImage, Resource newImage)
        throws ApiGatewayException {
        var files = getNewPendingFiles(oldImage, newImage).collect(Collectors.toSet());
        var workflow = lookUp(customer.getPublicationWorkflow());

        if (newImage.isDegree()) {
            handleDegree(newImage, workflow, files);
        } else {
            var publishingRequest = createWithFilesForApproval(newImage, userInstance, workflow, files);
            attempt(() -> persistPublishingRequest(newImage, publishingRequest));
        }
    }

    private void handleDegree(Resource resource, PublishingWorkflow workflow, Set<File> files) throws ApiGatewayException {
        var channelClaim = resource.getPrioritizedClaimedPublicationChannelWithinScope();
        if (channelClaim.isPresent() && !channelClaim.get().getOrganizationId().equals(userInstance.getTopLevelOrgCristinId())) {
            persistFilesApprovalThesis(channelClaim.get(), resource, workflow, files);
            return;
        }
        persistFilesApprovalThesisForUserInstitution(resource, workflow, files);
    }

    private void persistFilesApprovalThesisForUserInstitution(Resource resource, PublishingWorkflow workflow, Set<File> files) throws ApiGatewayException {
        FilesApprovalThesis.createForUserInstitution(resource, userInstance, workflow)
            .withFilesForApproval(files)
            .persistNewTicket(ticketService);
    }

    private void persistFilesApprovalThesis(ClaimedPublicationChannel channelClaim, Resource resource,
                                            PublishingWorkflow workflow, Set<File> files) throws ApiGatewayException {
        var organizationId = channelClaim.getOrganizationId();
        var channelClaimIdentifier = channelClaim.getIdentifier();
        FilesApprovalThesis.createForChannelOwningInstitution(resource, userInstance, organizationId,
                                                              channelClaimIdentifier, workflow)
            .withFilesForApproval(files)
            .persistNewTicket(ticketService);
    }

    private boolean containsNewPublishableFiles(Resource oldImage, Resource newImage) {
        return getNewPendingFiles(oldImage, newImage).findAny().isPresent();
    }

    private void updateFilesForApproval(
        Resource oldImage,
        Resource newImage,
        List<FilesApprovalEntry> filesApprovalEntries) {
        filesApprovalEntries.forEach(
            filesApprovalEntry ->
                updatePublishingRequest(oldImage, newImage, filesApprovalEntry));
    }

    private Stream<File> prepareFilesForApproval(Resource oldImage, Resource newImage,
                                                 FilesApprovalEntry filesApprovalEntry) {
        // get updated files on the ticket
        var updatedTicketFiles = getUpdatedTicketFiles(filesApprovalEntry, newImage);
        // start with a list of updated files on the ticket
        var files = new ArrayList<>(updatedTicketFiles);
        // add new files that should be on the ticket (we assume all new files belongs to the current ticket)
        files.addAll(getNewPendingFiles(oldImage, newImage).toList());

        return files.stream();
    }

    private static Set<File> getUpdatedTicketFiles(FilesApprovalEntry filesApprovalEntry, Resource newImage) {
        return filesApprovalEntry.getFilesForApproval()
                   .stream()
                   .map(fileForApproval -> newImage.getFileByIdentifier(fileForApproval.getIdentifier()).orElse(null))
                   .filter(
                       Objects::nonNull)
                   .filter(PendingFile.class::isInstance)
                   .collect(Collectors.toSet());
    }

    private Stream<File> getNewPendingFiles(Resource oldImage, Resource newImage) {
        var existingPendingFiles = getPendingFiles(oldImage).toList();
        var newPendingFiles = new ArrayList<>(getPendingFiles(newImage).toList());
        newPendingFiles.removeIf(
            newFile -> existingPendingFiles.stream().map(File::getIdentifier)
                           .anyMatch(oldFile -> oldFile.equals(newFile.getIdentifier())));
        return newPendingFiles.stream();
    }

    private void updatePublishingRequest(Resource oldImage, Resource newImage,
                                         FilesApprovalEntry filesApprovalEntry) {
        var files = prepareFilesForApproval(oldImage, newImage, filesApprovalEntry).collect(Collectors.toSet());
        if (customerAllowsPublishingMetadataAndFiles()) {
            filesApprovalEntry
                .withFilesForApproval(files)
                .approveFiles()
                .complete(newImage.toPublication(), userInstance)
                .persistUpdate(ticketService);
        } else {
            filesApprovalEntry
                .withFilesForApproval(files)
                .persistUpdate(ticketService);
        }
    }

    private boolean isAlreadyPublished(Resource resource) {
        var status = resource.getStatus();
        return PUBLISHED.equals(status) || PUBLISHED_METADATA.equals(status);
    }
}

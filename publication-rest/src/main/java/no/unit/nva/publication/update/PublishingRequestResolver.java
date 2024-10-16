package no.unit.nva.publication.update;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.publication.model.business.PublishingWorkflow.lookUp;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.core.attempt.Try.attempt;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.business.FileForApproval;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
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

    public PublishingRequestResolver(ResourceService resourceService,
                                     TicketService ticketService,
                                     UserInstance userInstance,
                                     Customer customer) {
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.userInstance = userInstance;
        this.customer = customer;
    }

    public void resolve(Publication oldImage, Publication newImage) {
        if (isAlreadyPublished(oldImage)) {
            handlePublishingRequest(oldImage, newImage);
        }
    }

    private boolean canManagePublishingRequests() {
        return userCanManagePublishingRequests() || userIsAllowedToPublishFiles();
    }

    private boolean userCanManagePublishingRequests() {
        return userInstance.getAccessRights().contains(MANAGE_PUBLISHING_REQUESTS);
    }

    private boolean userIsAllowedToPublishFiles() {
        return PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES.getValue()
                   .equals(customer.getPublicationWorkflow());
    }

    private static Set<FileForApproval> mergeFilesForApproval(PublishingRequestCase publishingRequestCase,
                                                                  Set<FileForApproval> filesForApproval) {
        var combinedFilesForApproval = new HashSet<>(publishingRequestCase.getFilesForApproval());
        combinedFilesForApproval.addAll(filesForApproval);
        return combinedFilesForApproval;
    }

    private static List<File> getUnpublishedFiles(Publication publication) {
        return publication.getAssociatedArtifacts()
                   .stream()
                   .filter(UnpublishedFile.class::isInstance)
                   .map(UnpublishedFile.class::cast)
                   .collect(Collectors.toList());
    }

    private static boolean isPending(TicketEntry publishingRequest) {
        return TicketStatus.PENDING.equals(publishingRequest.getStatus());
    }

    private Username getUsername() {
        return new Username(userInstance.getUsername());
    }

    private void handlePublishingRequest(Publication oldImage, Publication newImage) {
        var pendingPublishingRequests = fetchPendingPublishingRequestsForUserInstitution(oldImage);
        if (pendingPublishingRequests.isEmpty()) {
            createPublishingRequestOnFileUpdate(oldImage, newImage);
            return;
        }
        if (thereAreNoFiles(newImage)) {
            autoCompletePendingPublishingRequestsIfNeeded(newImage, pendingPublishingRequests);
            return;
        }
        if (updateHasFileChanges(oldImage, newImage)) {
            updateFilesForApproval(oldImage, newImage, pendingPublishingRequests);
        }
    }

    private boolean updateHasFileChanges(Publication oldImage, Publication newImage) {
        var existingFiles = new HashSet<>(getUnpublishedFiles(oldImage));
        var updatedFiles = new HashSet<>(getUnpublishedFiles(newImage));
        return !existingFiles.equals(updatedFiles);
    }

    private void autoCompletePendingPublishingRequestsIfNeeded(Publication publication,
                                                               List<PublishingRequestCase> pendingPublishingRequests) {
        pendingPublishingRequests.forEach(
            ticket -> ticket.complete(publication, getUsername()).persistUpdate(ticketService));
    }

    private boolean thereAreNoFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream().noneMatch(File.class::isInstance);
    }

    private List<PublishingRequestCase> fetchPendingPublishingRequestsForUserInstitution(Publication publication) {
        return resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                   .filter(PublishingRequestCase.class::isInstance)
                   .map(PublishingRequestCase.class::cast)
                   .filter(ticketEntry -> ticketEntry.hasSameOwnerAffiliationAs(userInstance))
                   .filter(PublishingRequestResolver::isPending)
                   .toList();
    }

    private void createPublishingRequestOnFileUpdate(Publication oldImage, Publication newImage) {
        if (containsNewPublishableFiles(oldImage, newImage)) {
            persistPendingPublishingRequest(oldImage, newImage);
        }
    }

    private TicketEntry persistPublishingRequest(Publication newImage,
                                                 PublishingRequestCase publishingRequest) throws ApiGatewayException {
        return canManagePublishingRequests()
                   ? publishingRequest.approveFiles().persistAutoComplete(ticketService, newImage, getUsername())
                   : publishingRequest.persistNewTicket(ticketService);
    }

    private void persistPendingPublishingRequest(Publication oldImage, Publication newImage) {
        attempt(() -> TicketEntry.requestNewTicket(newImage, PublishingRequestCase.class)).map(
                PublishingRequestCase.class::cast)
            .map(publishingRequest -> publishingRequest.withOwnerAffiliation(userInstance.getTopLevelOrgCristinId()))
            .map(publishingRequest -> publishingRequest.withWorkflow(lookUp(customer.getPublicationWorkflow())))
            .map(publishingRequest -> publishingRequest.withFilesForApproval(getFilesForApproval(oldImage, newImage)))
            .map(publishingRequest -> persistPublishingRequest(newImage, publishingRequest));
    }

    private boolean isPublishable(File file) {
        return nonNull(file.getLicense()) && !file.isAdministrativeAgreement();
    }

    private boolean containsNewPublishableFiles(Publication oldImage, Publication newImage) {
        return getNewUnpublishedFiles(oldImage, newImage).stream().anyMatch(this::isPublishable);
    }

    private void updateFilesForApproval(Publication oldImage, Publication newImage,
                                        List<PublishingRequestCase> pendingPublishingRequests) {
        var filesForApproval = getFilesForApproval(oldImage, newImage);
        pendingPublishingRequests.forEach(
            publishingRequestCase -> updatePublishingRequest(newImage, publishingRequestCase, filesForApproval));
    }

    private List<File> getNewUnpublishedFiles(Publication oldImage, Publication newImage) {
        var existingUnpublishedFiles = getUnpublishedFiles(oldImage);
        var newUnpublishedFiles = getUnpublishedFiles(newImage);
        newUnpublishedFiles.removeAll(existingUnpublishedFiles);
        return newUnpublishedFiles;
    }

    private Set<FileForApproval> getFilesForApproval(Publication oldImage, Publication newImage) {
        return getNewUnpublishedFiles(oldImage, newImage).stream()
                   .map(FileForApproval::fromFile)
                   .collect(Collectors.toSet());
    }

    private void updatePublishingRequest(Publication newImage, PublishingRequestCase publishingRequest,
                                         Set<FileForApproval> filesForApproval) {
        var updatedFilesForApproval = mergeFilesForApproval(publishingRequest, filesForApproval);
        ensureFileExists(newImage, updatedFilesForApproval);
        if (canManagePublishingRequests()) {
            publishingRequest.withFilesForApproval(updatedFilesForApproval)
                .approveFiles()
                .complete(newImage, getUsername())
                .persistUpdate(ticketService);
        } else {
            publishingRequest.withFilesForApproval(updatedFilesForApproval).persistUpdate(ticketService);
        }
    }

    private void ensureFileExists(Publication publication, Set<FileForApproval> updatedFilesForApproval) {
        updatedFilesForApproval.removeIf(file -> publicationDoesNotContainFile(publication, file));
    }

    private boolean publicationDoesNotContainFile(Publication publication, FileForApproval file) {
        return publication.getAssociatedArtifacts()
                   .stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast)
                   .noneMatch(existingFile -> existingFile.getIdentifier().equals(file.identifier()));
    }

    private boolean isAlreadyPublished(Publication existingPublication) {
        var status = existingPublication.getStatus();
        return PUBLISHED.equals(status) || PUBLISHED_METADATA.equals(status);
    }
}


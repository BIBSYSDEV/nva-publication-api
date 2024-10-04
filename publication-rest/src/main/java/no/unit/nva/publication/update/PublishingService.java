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

public final class PublishingService {

    private final TicketService ticketService;
    private final ResourceService resourceService;

    private PublishingService(ResourceService resourceService, TicketService ticketService) {
        this.ticketService = ticketService;
        this.resourceService = resourceService;
    }

    public static PublishingService create(ResourceService resourceService, TicketService ticketService) {
        return new PublishingService(resourceService, ticketService);
    }

    public void resolve(Publication oldImage, Publication newImage, Customer customer, UserInstance userInstance) {
        if (isAlreadyPublished(oldImage)) {
            handlePublishingRequest(oldImage, newImage, customer, userInstance);
        }
    }

    private static boolean canManagePublishingRequests(Customer customer, UserInstance userInstance) {
        return canManagePublishingRequests(userInstance) || useIsAllowedToPublishFiles(customer);
    }

    private static boolean canManagePublishingRequests(UserInstance userInstance) {
        return userInstance.getAccessRights().contains(MANAGE_PUBLISHING_REQUESTS);
    }

    private static boolean useIsAllowedToPublishFiles(Customer customer) {
        return PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES.getValue()
                   .equals(customer.getPublicationWorkflow());
    }

    private static HashSet<FileForApproval> mergeFilesForApproval(PublishingRequestCase publishingRequestCase,
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

    private static Username getUsername(UserInstance userInstance) {
        return new Username(userInstance.getUsername());
    }

    private void handlePublishingRequest(Publication oldImage, Publication newImage, Customer customer,
                                         UserInstance userInstance) {
        var pendingPublishingRequests = fetchPendingPublishingRequestsForUserInstitution(oldImage, userInstance);
        if (pendingPublishingRequests.isEmpty()) {
            createPublishingRequestOnFileUpdate(oldImage, newImage, customer, userInstance);
        }
        if (thereAreNoFiles(newImage)) {
            autoCompletePendingPublishingRequestsIfNeeded(newImage, userInstance, pendingPublishingRequests);
        }
        if (updateHasFileChanges(oldImage, newImage)) {
            updateFilesForApproval(oldImage, newImage, customer, userInstance, pendingPublishingRequests);
        }
    }

    private boolean updateHasFileChanges(Publication oldImage, Publication newImage) {
        var existingFiles = new HashSet<>(getUnpublishedFiles(oldImage));
        var updatedFiles = new HashSet<>(getUnpublishedFiles(newImage));
        return !existingFiles.equals(updatedFiles);
    }

    private void autoCompletePendingPublishingRequestsIfNeeded(Publication publication, UserInstance userInstance,
                                                               List<PublishingRequestCase> pendingPublishingRequests) {
        pendingPublishingRequests.forEach(
            ticket -> ticket.complete(publication, getUsername(userInstance)).persistUpdate(ticketService));
    }

    private boolean thereAreNoFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream().noneMatch(File.class::isInstance);
    }

    private List<PublishingRequestCase> fetchPendingPublishingRequestsForUserInstitution(Publication publication,
                                                                                         UserInstance userInstance) {
        return resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                   .filter(PublishingRequestCase.class::isInstance)
                   .map(PublishingRequestCase.class::cast)
                   .filter(ticketEntry -> ticketEntry.hasSameOwnerAffiliationAs(userInstance))
                   .filter(PublishingService::isPending)
                   .toList();
    }

    private void createPublishingRequestOnFileUpdate(Publication oldImage, Publication newImage, Customer customer,
                                                     UserInstance userInstance) {
        if (containsNewPublishableFiles(oldImage, newImage)) {
            persistPendingPublishingRequest(oldImage, newImage, customer, userInstance);
        }
    }

    private TicketEntry persistPublishingRequest(Publication newImage, UserInstance userInstance, Customer customer,
                                                 PublishingRequestCase publishingRequest) throws ApiGatewayException {
        return canManagePublishingRequests(customer, userInstance) ? publishingRequest.approveFiles()
                                                                         .persistAutoComplete(ticketService, newImage,
                                                                                              getUsername(userInstance))
                   : publishingRequest.persistNewTicket(ticketService);
    }

    private void persistPendingPublishingRequest(Publication oldImage, Publication newImage, Customer customer,
                                                 UserInstance userInstance) {
        attempt(() -> TicketEntry.requestNewTicket(newImage, PublishingRequestCase.class)).map(
                PublishingRequestCase.class::cast)
            .map(publishingRequest -> publishingRequest.withOwnerAffiliation(userInstance.getTopLevelOrgCristinId()))
            .map(publishingRequest -> publishingRequest.withWorkflow(lookUp(customer.getPublicationWorkflow())))
            .map(publishingRequest -> publishingRequest.withFilesForApproval(getFilesForApproval(oldImage, newImage)))
            .map(publishingRequest -> persistPublishingRequest(newImage, userInstance, customer, publishingRequest));
    }

    private boolean isPublishable(File file) {
        return nonNull(file.getLicense()) && !file.isAdministrativeAgreement();
    }

    private boolean containsNewPublishableFiles(Publication oldImage, Publication newImage) {
        return getNewUnpublishedFiles(oldImage, newImage).stream().anyMatch(this::isPublishable);
    }

    private void updateFilesForApproval(Publication oldImage, Publication newImage, Customer customer,
                                        UserInstance userInstance,
                                        List<PublishingRequestCase> pendingPublishingRequests) {
        var filesForApproval = getFilesForApproval(oldImage, newImage);
        pendingPublishingRequests.forEach(
            publishingRequestCase -> updatePublishingRequest(newImage, publishingRequestCase, filesForApproval,
                                                             customer, userInstance));
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
                                         Set<FileForApproval> filesForApproval, Customer customer,
                                         UserInstance userInstance) {
        var updatedFilesForApproval = mergeFilesForApproval(publishingRequest, filesForApproval);
        ensureFileExists(newImage, updatedFilesForApproval);
        if (canManagePublishingRequests(customer, userInstance)) {
            publishingRequest.withFilesForApproval(updatedFilesForApproval)
                .approveFiles()
                .complete(newImage, getUsername(userInstance))
                .persistUpdate(ticketService);
        } else {
            publishingRequest.withFilesForApproval(updatedFilesForApproval).persistUpdate(ticketService);
        }
    }

    private void ensureFileExists(Publication publication, HashSet<FileForApproval> updatedFilesForApproval) {
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


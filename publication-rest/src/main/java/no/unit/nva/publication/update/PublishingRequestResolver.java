package no.unit.nva.publication.update;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.publication.model.business.PublishingWorkflow.lookUp;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.publication.commons.customer.Customer;
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

    public void resolve(Publication oldImage, Publication newImage) {
        if (isAlreadyPublished(oldImage)) {
            handlePublishingRequest(oldImage, newImage);
        }
    }

    private boolean customerAllowsPublishingMetadataAndFiles() {
        return PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES
                          .getValue()
                          .equals(customer.getPublicationWorkflow());
    }

    private static Stream<File> getPendingFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                .filter(PendingFile.class::isInstance)
                .map(File.class::cast);
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
        if (thereAreNoPendingFiles(newImage)) {
            autoCompletePendingPublishingRequestsIfNeeded(newImage, pendingPublishingRequests);
            return;
        }
        if (updateHasPendingFileChanges(oldImage, newImage)) {
            updateFilesForApproval(oldImage, newImage, pendingPublishingRequests);
        }
    }

    private boolean updateHasPendingFileChanges(Publication oldImage, Publication newImage) {
        var existingFiles = getPendingFiles(oldImage).collect(Collectors.toSet());
        var updatedFiles = getPendingFiles(newImage).collect(Collectors.toSet());
        return !existingFiles.equals(updatedFiles);
    }

    private void autoCompletePendingPublishingRequestsIfNeeded(
            Publication publication, List<PublishingRequestCase> pendingPublishingRequests) {
        pendingPublishingRequests.forEach(
                ticket -> ticket.complete(publication, getUsername()).persistUpdate(ticketService));
    }

    private boolean thereAreNoPendingFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream()
                .noneMatch(PendingFile.class::isInstance);
    }

    private List<PublishingRequestCase> fetchPendingPublishingRequestsForUserInstitution(
            Publication publication) {
        return resourceService
                .fetchAllTicketsForResource(Resource.fromPublication(publication))
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

    private TicketEntry persistPublishingRequest(
            Publication newImage, PublishingRequestCase publishingRequest)
            throws ApiGatewayException {
        return customerAllowsPublishingMetadataAndFiles()
                ? publishingRequest
                        .approveFiles()
                        .persistAutoComplete(ticketService, newImage, getUsername())
                : publishingRequest.persistNewTicket(ticketService);
    }

    private void persistPendingPublishingRequest(Publication oldImage, Publication newImage) {
        var files = getNewPendingFiles(oldImage, newImage).collect(Collectors.toSet());
        attempt(() -> TicketEntry.requestNewTicket(newImage, PublishingRequestCase.class))
                .map(PublishingRequestCase.class::cast)
                .map(publishingRequest ->
                         publishingRequest.withOwnerAffiliation(userInstance.getTopLevelOrgCristinId()))
                .map(publishingRequest -> publishingRequest.withWorkflow(lookUp(customer.getPublicationWorkflow())))
                .map(publishingRequest -> publishingRequest.withFilesForApproval(files))
                .map(publishingRequest -> publishingRequest.withOwner(userInstance.getUsername()))
                .map(PublishingRequestCase.class::cast)
                .map(publishingRequest -> persistPublishingRequest(newImage, publishingRequest));
    }

    private boolean containsNewPublishableFiles(Publication oldImage, Publication newImage) {
        return getNewPendingFiles(oldImage, newImage).findAny().isPresent();
    }

    private void updateFilesForApproval(
            Publication oldImage,
            Publication newImage,
            List<PublishingRequestCase> pendingPublishingRequests) {
        pendingPublishingRequests.forEach(
                publishingRequestCase ->
                        updatePublishingRequest(oldImage, newImage, publishingRequestCase));
    }

    private Stream<File> prepareFilesForApproval(Publication oldImage, Publication newImage) {
        var filesForApproval = getNewPendingFiles(oldImage, newImage);
        return removeFilesIfFileDoesNotExists(newImage, filesForApproval);
    }

    private Stream<File> getNewPendingFiles(Publication oldImage, Publication newImage) {
        var existingPendingFiles = getPendingFiles(oldImage).toList();
        return getPendingFiles(newImage)
                   .filter(file -> !existingPendingFiles.contains(file))
                   .distinct();
    }

    private void updatePublishingRequest(Publication oldImage, Publication newImage,
                                         PublishingRequestCase publishingRequest) {
        var files = prepareFilesForApproval(oldImage, newImage).collect(Collectors.toSet());
        if (customerAllowsPublishingMetadataAndFiles()) {
            publishingRequest
                    .withFilesForApproval(files)
                    .approveFiles()
                    .complete(newImage, getUsername())
                    .persistUpdate(ticketService);
        } else {
            publishingRequest
                    .withFilesForApproval(files)
                    .persistUpdate(ticketService);
        }
    }

    private Stream<File> removeFilesIfFileDoesNotExists(Publication publication, Stream<File> updatedFilesForApproval) {
        var preexistingFiles = publication.getAssociatedArtifacts().stream()
                                   .filter(File.class::isInstance)
                                   .map(File.class::cast)
                                   .map(File::getIdentifier)
                                   .toList();
        return updatedFilesForApproval.filter(file -> preexistingFiles.contains(file.getIdentifier()));
    }

    private boolean isAlreadyPublished(Publication existingPublication) {
        var status = existingPublication.getStatus();
        return PUBLISHED.equals(status) || PUBLISHED_METADATA.equals(status);
    }
}

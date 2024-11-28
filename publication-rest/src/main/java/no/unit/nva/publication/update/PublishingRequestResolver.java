package no.unit.nva.publication.update;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.publication.model.business.PublishingWorkflow.lookUp;
import static nva.commons.core.attempt.Try.attempt;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    private static Set<File> mergeFilesForApproval(
            PublishingRequestCase publishingRequestCase, Set<File> filesForApproval) {
        var newFileIds = filesForApproval.stream()
                                      .map(File::getIdentifier)
                                      .collect(Collectors.toSet());

        var existingFiles = publishingRequestCase.getFilesForApproval().stream()
                                 .filter(file -> !newFileIds.contains(file.getIdentifier()))
                                 .collect(Collectors.toSet());

        filesForApproval.addAll(existingFiles);

        return filesForApproval;
    }

    private static List<File> getPendingFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                .filter(PendingFile.class::isInstance)
                .map(File.class::cast)
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
        if (thereAreNoPendingFiles(newImage)) {
            autoCompletePendingPublishingRequestsIfNeeded(newImage, pendingPublishingRequests);
            return;
        }
        if (updateHasFileChanges(oldImage, newImage)) {
            updateFilesForApproval(oldImage, newImage, pendingPublishingRequests);
        }
    }

    private boolean updateHasFileChanges(Publication oldImage, Publication newImage) {
        var existingFiles = new HashSet<>(getPendingFiles(oldImage));
        var updatedFiles = new HashSet<>(getPendingFiles(newImage));
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
        attempt(() -> TicketEntry.requestNewTicket(newImage, PublishingRequestCase.class))
                .map(PublishingRequestCase.class::cast)
                .map(
                        publishingRequest ->
                                publishingRequest.withOwnerAffiliation(
                                        userInstance.getTopLevelOrgCristinId()))
                .map(
                        publishingRequest ->
                                publishingRequest.withWorkflow(
                                        lookUp(customer.getPublicationWorkflow())))
                .map(
                        publishingRequest ->
                                publishingRequest.withFilesForApproval(
                                        getFilesForApproval(oldImage, newImage)))
                .map(publishingRequest -> publishingRequest.withOwner(userInstance.getUsername()))
                .map(
                        publishingRequest ->
                                persistPublishingRequest(
                                        newImage, (PublishingRequestCase) publishingRequest));
    }

    private boolean containsNewPublishableFiles(Publication oldImage, Publication newImage) {
        return !getNewPendingFiles(oldImage, newImage).isEmpty();
    }

    private void updateFilesForApproval(
            Publication oldImage,
            Publication newImage,
            List<PublishingRequestCase> pendingPublishingRequests) {
        pendingPublishingRequests.forEach(
                publishingRequestCase ->
                        updatePublishingRequest(oldImage, newImage, publishingRequestCase));
    }

    private Set<File> prepareFilesForApproval(Publication oldImage, Publication newImage, PublishingRequestCase publishingRequestCase) {
        var filesForApproval = getFilesForApproval(oldImage, newImage);
        var mergedFilesForApproval = mergeFilesForApproval(publishingRequestCase, filesForApproval);
        ensureFileExists(newImage, mergedFilesForApproval);
        return mergedFilesForApproval;
    }

    private List<File> getNewPendingFiles(Publication oldImage, Publication newImage) {
        var existingPendingFiles = getPendingFiles(oldImage);
        var newPendingFiles = getPendingFiles(newImage);
        newPendingFiles.removeAll(existingPendingFiles);
        return newPendingFiles;
    }

    private Set<File> getFilesForApproval(Publication oldImage, Publication newImage) {
        return new HashSet<>(getNewPendingFiles(oldImage, newImage));
    }

    private void updatePublishingRequest(Publication oldImage, Publication newImage,
                                         PublishingRequestCase publishingRequest) {
        var updatedFilesForApproval = prepareFilesForApproval(oldImage, newImage, publishingRequest);
        ensureFileExists(newImage, updatedFilesForApproval);
        if (customerAllowsPublishingMetadataAndFiles()) {
            publishingRequest
                    .withFilesForApproval(updatedFilesForApproval)
                    .approveFiles()
                    .complete(newImage, getUsername())
                    .persistUpdate(ticketService);
        } else {
            publishingRequest
                    .withFilesForApproval(updatedFilesForApproval)
                    .persistUpdate(ticketService);
        }
    }

    private void ensureFileExists(Publication publication, Set<File> updatedFilesForApproval) {
        updatedFilesForApproval.removeIf(file -> publicationDoesNotContainFile(publication, file));
    }

    private boolean publicationDoesNotContainFile(Publication publication, File file) {
        return publication.getAssociatedArtifacts().stream()
                .filter(File.class::isInstance)
                .map(File.class::cast)
                .noneMatch(existingFile -> existingFile.getIdentifier().equals(file.getIdentifier()));
    }

    private boolean isAlreadyPublished(Publication existingPublication) {
        var status = existingPublication.getStatus();
        return PUBLISHED.equals(status) || PUBLISHED_METADATA.equals(status);
    }
}

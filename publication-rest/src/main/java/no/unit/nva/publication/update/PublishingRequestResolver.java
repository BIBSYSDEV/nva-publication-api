package no.unit.nva.publication.update;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.publication.model.business.PublishingRequestCase.createWithFilesForApproval;
import static no.unit.nva.publication.model.business.PublishingWorkflow.lookUp;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

public final class PublishingRequestResolver {

    private static final String CUSTOMER = "customer";
    private static final String CHANNEL_CLAIM = "channel-claim";
    private static final String API_HOST = new Environment().readEnv("API_HOST");

    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;
    private final UserInstance userInstance;
    private final Customer customer;

    public PublishingRequestResolver(
        ResourceService resourceService,
        TicketService ticketService,
        IdentityServiceClient identityServiceClient,
        UserInstance userInstance,
        Customer customer) {
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.identityServiceClient = identityServiceClient;
        this.userInstance = userInstance;
        this.customer = customer;
    }

    public void resolve(Publication oldImage, Publication newImage) throws ApiGatewayException {
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

    private static boolean isPending(TicketEntry ticketEntry) {
        return TicketStatus.PENDING.equals(ticketEntry.getStatus());
    }

    private void handlePublishingRequest(Publication oldImage, Publication newImage) throws ApiGatewayException {
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
        Publication publication, List<FilesApprovalEntry> filesApprovalEntries) {
        filesApprovalEntries.forEach(
            ticket -> ticket.complete(publication, userInstance).persistUpdate(ticketService));
    }

    private boolean thereAreNoPendingFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream()
                   .noneMatch(PendingFile.class::isInstance);
    }

    private List<FilesApprovalEntry> fetchPendingFileApprovalEntryForUserInstitutionOrWithFilesForApprovalWhenDegree(
        Publication publication) {
        return resourceService
                   .fetchAllTicketsForResource(Resource.fromPublication(publication))
                   .filter(FilesApprovalEntry.class::isInstance)
                   .map(FilesApprovalEntry.class::cast)
                   .filter(ticketEntry -> shouldIncludeEntry(publication, ticketEntry, userInstance))
                   .filter(PublishingRequestResolver::isPending)
                   .toList();
    }

    private boolean shouldIncludeEntry(Publication publication, FilesApprovalEntry ticketEntry, UserInstance userInstance) {
        return Resource.fromPublication(publication).isDegree() && !ticketEntry.getFilesForApproval().isEmpty()
               || ticketEntry.hasSameOwnerAffiliationAs(userInstance);
    }

    private void createPublishingRequestOnFileUpdate(Publication oldImage, Publication newImage)
        throws ApiGatewayException {
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
                         .persistAutoComplete(ticketService, newImage, userInstance)
                   : publishingRequest.persistNewTicket(ticketService);
    }

    private void persistPendingPublishingRequest(Publication oldImage, Publication newImage)
        throws ApiGatewayException {
        var files = getNewPendingFiles(oldImage, newImage).collect(Collectors.toSet());
        var resource = Resource.fromPublication(newImage);
        var workflow = lookUp(customer.getPublicationWorkflow());

        if (resource.isDegree()) {
            handleDegree(resource, workflow, files);
        } else {
            var publishingRequest = createWithFilesForApproval(resource, userInstance, workflow, files);
            attempt(() -> persistPublishingRequest(newImage, publishingRequest));
        }
    }

    private void handleDegree(Resource resource, PublishingWorkflow workflow, Set<File> files) throws ApiGatewayException {
        var publisher = getPublisher(resource);
        if (publisher.isPresent()) {
            var channelClaim = getChannelClaim(publisher.get());
            if (channelClaim.isPresent() && !isClaimedByUserOrganization(channelClaim.get(), userInstance)) {
                persistFilesApprovalThesis(channelClaim.get(), resource, workflow, files);
                return;
            }
        }
        persistFilesApprovalThesisForUserInstitution(resource, workflow, files);
    }

    private void persistFilesApprovalThesisForUserInstitution(Resource resource, PublishingWorkflow workflow, Set<File> files) throws ApiGatewayException {
        FilesApprovalThesis.createForUserInstitution(resource, userInstance, workflow)
            .withFilesForApproval(files)
            .persistNewTicket(ticketService);
    }

    private void persistFilesApprovalThesis(ChannelClaimDto channelClaim, Resource resource,
                                            PublishingWorkflow workflow, Set<File> files) throws ApiGatewayException {
        var organizationId = channelClaim.claimedBy().organizationId();
        FilesApprovalThesis.create(resource, userInstance, organizationId, workflow)
            .withFilesForApproval(files)
            .persistNewTicket(ticketService);
    }

    private boolean isClaimedByUserOrganization(ChannelClaimDto channelClaim, UserInstance userInstance) {
        return Optional.ofNullable(channelClaim)
                   .map(ChannelClaimDto::claimedBy)
                   .map(CustomerSummaryDto::id)
                   .map(id -> userInstance.getCustomerId().equals(id))
                   .orElse(true);
    }

    private Optional<ChannelClaimDto> getChannelClaim(Publisher publisher) throws BadGatewayException {
        try {
            return Optional.of(identityServiceClient.getChannelClaim(createChannelClaimUri(publisher)));
        } catch (NotFoundException exception) {
            return Optional.empty();
        } catch (Exception e) {
            throw new BadGatewayException("Could not fetch channel owner!");
        }
    }

    private URI createChannelClaimUri(Publisher publisher) {
        var channelClaimIdentifier = UriWrapper.fromUri(publisher.getId())
                                         .replacePathElementByIndexFromEnd(0, StringUtils.EMPTY_STRING)
                                         .getLastPathElement();
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CUSTOMER)
                   .addChild(CHANNEL_CLAIM)
                   .addChild(channelClaimIdentifier)
                   .getUri();
    }

    private static Optional<Publisher> getPublisher(Resource resource) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .filter(Degree.class::isInstance)
                   .map(Degree.class::cast)
                   .map(Book::getPublisher)
                   .filter(Publisher.class::isInstance)
                   .map(Publisher.class::cast);
    }

    private boolean containsNewPublishableFiles(Publication oldImage, Publication newImage) {
        return getNewPendingFiles(oldImage, newImage).findAny().isPresent();
    }

    private void updateFilesForApproval(
        Publication oldImage,
        Publication newImage,
        List<FilesApprovalEntry> filesApprovalEntries) {
        filesApprovalEntries.forEach(
            filesApprovalEntry ->
                updatePublishingRequest(oldImage, newImage, filesApprovalEntry));
    }

    private Stream<File> prepareFilesForApproval(Publication oldImage, Publication newImage,
                                                 FilesApprovalEntry filesApprovalEntry) {
        // get updated files on the ticket
        var updatedTicketFiles = getUpdatedTicketFiles(filesApprovalEntry, newImage);
        // start with a list of updated files on the ticket
        var files = new ArrayList<>(updatedTicketFiles);
        // add new files that should be on the ticket (we assume all new files belongs to the current ticket)
        files.addAll(getNewPendingFiles(oldImage, newImage).toList());

        return files.stream();
    }

    private static Set<File> getUpdatedTicketFiles(FilesApprovalEntry filesApprovalEntry, Publication newImage) {
        return filesApprovalEntry.getFilesForApproval()
                   .stream()
                   .map(fileForApproval -> newImage.getFile(fileForApproval.getIdentifier()).orElse(null))
                   .filter(
                       Objects::nonNull)
                   .filter(PendingFile.class::isInstance)
                   .collect(Collectors.toSet());
    }

    private Stream<File> getNewPendingFiles(Publication oldImage, Publication newImage) {
        var existingPendingFiles = getPendingFiles(oldImage).toList();
        var newPendingFiles = new ArrayList<>(getPendingFiles(newImage).toList());
        newPendingFiles.removeIf(
            newFile -> existingPendingFiles.stream().map(File::getIdentifier)
                           .anyMatch(oldFile -> oldFile.equals(newFile.getIdentifier())));
        return newPendingFiles.stream();
    }

    private void updatePublishingRequest(Publication oldImage, Publication newImage,
                                         FilesApprovalEntry filesApprovalEntry) {
        var files = prepareFilesForApproval(oldImage, newImage, filesApprovalEntry).collect(Collectors.toSet());
        if (customerAllowsPublishingMetadataAndFiles()) {
            filesApprovalEntry
                .withFilesForApproval(files)
                .approveFiles()
                .complete(newImage, userInstance)
                .persistUpdate(ticketService);
        } else {
            filesApprovalEntry
                .withFilesForApproval(files)
                .persistUpdate(ticketService);
        }
    }

    private boolean isAlreadyPublished(Publication existingPublication) {
        var status = existingPublication.getStatus();
        return PUBLISHED.equals(status) || PUBLISHED_METADATA.equals(status);
    }
}

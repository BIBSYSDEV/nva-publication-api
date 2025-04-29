package no.unit.nva.publication.update;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomUploadedFile;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;
import static no.unit.nva.publication.ticket.test.TicketTestUtils.createPersistedPublicationWithFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PublishingRequestResolverTest extends ResourcesLocalTest {

    private TicketService ticketService;
    private ResourceService resourceService;
    private IdentityServiceClient identityServiceClient;

    @BeforeEach
    public void setUp() throws NotFoundException {
        super.init();
        ticketService = getTicketService();
        resourceService = getResourceServiceBuilder().build();
        identityServiceClient = mock(IdentityServiceClient.class);
    }

    @DisplayName("When user removes unpublished files from a publication" +
                 "and there exists pending publishing request with those files" +
                 "and there are no new unpublished files to publish" +
                 "then existing pending publishing request should be completed")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#notApprovedFilesProvider")
    void shouldApprovePendingPublishingReqeustForInstitutionWhenUserRemovesUnpublishedFiles(File file)
        throws ApiGatewayException {
        var publication = createPersistedPublicationWithFile(PublicationStatus.PUBLISHED, file, resourceService);
        persistPublishingRequestContainingExistingPendingFiles(publication);
        var publicationUpdateRemovingUnpublishedFiles = publication.copy()
                                                            .withAssociatedArtifacts(List.of())
                                                            .withStatus(PublicationStatus.PUBLISHED)
                                                            .build();
        publishingRequestResolver(publication).resolve(publication, publicationUpdateRemovingUnpublishedFiles);

        var publishingRequest = getFileApprovalEntry(publication);

        Assertions.assertEquals(COMPLETED, publishingRequest.getStatus());
        assertTrue(publishingRequest.getFilesForApproval().isEmpty());
        assertTrue(publishingRequest.getApprovedFiles().isEmpty());
    }

    @Test
    void shouldApprovePendingPublishingReqeustForInstitutionWhenUserRemovesUnpublishedFilesButPublicationHasPublishedFiles()
        throws ApiGatewayException {
        var publication = randomPublication();
        var openFile = randomOpenFile();
        var pendingOpenFile = randomPendingOpenFile();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(pendingOpenFile, openFile)));
        var persistedPublication = persistPublication(publication);
        Resource.fromPublication(persistedPublication).publish(resourceService, UserInstance.fromPublication(publication));
        persistPublishingRequestContainingExistingPendingFiles(persistedPublication);
        var publicationUpdateRemovingUnpublishedFiles = persistedPublication.copy()
                                                            .withAssociatedArtifacts(List.of(openFile))
                                                            .withStatus(PublicationStatus.PUBLISHED)
                                                            .build();
        publishingRequestResolver(persistedPublication).resolve(resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier()),
                                                                publicationUpdateRemovingUnpublishedFiles);

        var publishingRequest = getFileApprovalEntry(persistedPublication);

        Assertions.assertEquals(COMPLETED, publishingRequest.getStatus());
        assertTrue(publishingRequest.getFilesForApproval().isEmpty());
        assertTrue(publishingRequest.getApprovedFiles().isEmpty());
    }

    private FilesApprovalEntry getFileApprovalEntry(Publication publication) {
        return resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                   .map(FilesApprovalEntry.class::cast)
                   .toList()
                   .getFirst();
    }

    @Test
    void shouldUpdateExistingPendingFileInFilesForApprovalInPendingPublishingRequestWhenUpdatingFileForPublication()
        throws ApiGatewayException {
        var publication = randomPublication();
        var pendingOpenFile = randomPendingOpenFile().copy().withLicense(null).buildPendingOpenFile();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(pendingOpenFile)));
        var persistedPublication = persistPublication(publication);
        persistPublishingRequestContainingExistingPendingFiles(persistedPublication);

        var updatedFile = pendingOpenFile.copy().withLicense(randomUri()).buildPendingOpenFile();
        var updatedPublication = persistedPublication.copy()
            .withAssociatedArtifacts(List.of(updatedFile))
            .build();
        publishingRequestResolver(persistedPublication).resolve(resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier()),
                                                               updatedPublication);

        var filesForApproval = getFileApprovalEntry(persistedPublication).getFilesForApproval();

        assertTrue(filesForApproval.contains(updatedFile));
    }

    @Test
    void shouldNotChangeApprovalListWhenChangingFromOnePendingToAnotherStatus()
        throws ApiGatewayException {
        var publication = randomPublication();
        var randomPendingOpenFile1 = randomPendingOpenFile();
        var randomPendingOpenFile2 = randomPendingOpenFile();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(randomPendingOpenFile1, randomPendingOpenFile2)));
        var persistedPublication = persistPublication(publication);
        persistPublishingRequestContainingExistingPendingFiles(persistedPublication);

        var randomPendingInternalFile2 = randomPendingOpenFile2.copy().buildPendingInternalFile();
        var updatedPublication = persistedPublication.copy()
                                     .withAssociatedArtifacts(List.of(randomPendingOpenFile1, randomPendingInternalFile2))
                                     .build();
        publishingRequestResolver(persistedPublication).resolve(resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier()),
                                                                updatedPublication);

        var filesForApproval = getFileApprovalEntry(persistedPublication).getFilesForApproval();

        assertTrue(filesForApproval.contains(randomPendingInternalFile2));
        assertTrue(filesForApproval.contains(randomPendingOpenFile1));
    }

    @Test
    void shouldRemoveFileFromPendingPublishingRequestWhenFileIsBeingUpdatedFromPendingToHiddenFile()
        throws ApiGatewayException {
        var publication = randomPublication();
        var randomPendingOpenFile1 = randomPendingOpenFile();
        var randomPendingOpenFile2 = randomPendingOpenFile();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(randomPendingOpenFile1, randomPendingOpenFile2)));
        var persistedPublication = persistPublication(publication);
        persistPublishingRequestContainingExistingPendingFiles(persistedPublication);

        var hiddenFile = randomPendingOpenFile2.copy().buildHiddenFile();
        var updatedPublication = persistedPublication.copy()
                                     .withAssociatedArtifacts(List.of(randomPendingOpenFile1, hiddenFile))
                                     .build();
        publishingRequestResolver(persistedPublication).resolve(resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier()),
                                                                updatedPublication);

        var filesForApproval = getFileApprovalEntry(persistedPublication).getFilesForApproval();

        assertFalse(filesForApproval.contains(hiddenFile));
        assertTrue(filesForApproval.contains(randomPendingOpenFile1));
    }

    @Test
    void shouldCompletePendingPublishingRequestWhenFileIsBeingUpdatedFromPendingToHiddenFileAndIsTheOnlyFileToApprove()
        throws ApiGatewayException {
        var publication = randomPublication();
        var pendingFile = randomPendingOpenFile();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(pendingFile)));
        var persistedPublication = persistPublication(publication);
        persistPublishingRequestContainingExistingPendingFiles(persistedPublication);

        var hiddenFile = pendingFile.copy().buildHiddenFile();
        var updatedPublication = persistedPublication.copy()
                                     .withAssociatedArtifacts(List.of(hiddenFile))
                                     .build();
        publishingRequestResolver(persistedPublication).resolve(resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier()),
                                                                updatedPublication);

        var publishingRequest = getFileApprovalEntry(persistedPublication);

        assertEquals(COMPLETED, publishingRequest.getStatus());
        assertTrue(publishingRequest.getFilesForApproval().isEmpty());
        assertTrue(publishingRequest.getApprovedFiles().isEmpty());
    }

    @Test
    void shouldPersistFilesApprovalThesisWhenUpdatingFileToPendingWhenDegreeAndChannelClaimedByUserInstitution()
        throws ApiGatewayException {
        var instanceType = DegreeBachelor.class;
        var publication = randomPublication(instanceType);
        var uploadedFile = randomUploadedFile();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(uploadedFile)));
        var persistedPublication = persistPublication(publication);

        var pendingFile = uploadedFile.copy().buildPendingOpenFile();
        var updatedPublication = persistedPublication.copy()
                                     .withAssociatedArtifacts(List.of(pendingFile))
                                     .build();
        when(identityServiceClient.getChannelClaim(any())).thenReturn(channelClaimDto(
            publication.getPublisher().getId(), publication.getResourceOwner().getOwnerAffiliation(),
           instanceType.getSimpleName()
        ));
        publishingRequestResolver(persistedPublication).resolve(resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier()),
                                                                updatedPublication);

        var fileApprovalEntry = getFileApprovalEntry(persistedPublication);

        assertEquals(PENDING, fileApprovalEntry.getStatus());
        assertTrue(fileApprovalEntry.getFilesForApproval().contains(pendingFile));
        assertEquals(UserInstance.fromPublication(publication).getTopLevelOrgCristinId(),
                     fileApprovalEntry.getOwnerAffiliation());
    }

    @Test
    void shouldPersistFilesApprovalThesisWhenUpdatingFileToPendingWhenDegreeAndChannelClaimedByNotUserInstitution()
        throws ApiGatewayException {
        var instanceType = DegreeBachelor.class;
        var publication = randomPublication(instanceType);
        var uploadedFile = randomUploadedFile();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(uploadedFile)));
        var persistedPublication = persistPublication(publication);

        var pendingFile = uploadedFile.copy().buildPendingOpenFile();
        var updatedPublication = persistedPublication.copy()
                                     .withAssociatedArtifacts(List.of(pendingFile))
                                     .build();
        var ownerAffiliation = publication.getResourceOwner().getOwnerAffiliation();
        when(identityServiceClient.getChannelClaim(any())).thenReturn(channelClaimDto(
            randomUri(), ownerAffiliation,
            instanceType.getSimpleName()
        ));
        publishingRequestResolver(persistedPublication).resolve(resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier()),
                                                                updatedPublication);

        var fileApprovalEntry = getFileApprovalEntry(persistedPublication);

        assertEquals(PENDING, fileApprovalEntry.getStatus());
        assertTrue(fileApprovalEntry.getFilesForApproval().contains(pendingFile));
        assertEquals(ownerAffiliation, fileApprovalEntry.getOwnerAffiliation());
    }

    private ChannelClaimDto channelClaimDto(URI customerId, URI organizationId, String... scope) {
        return new ChannelClaimDto(new CustomerSummaryDto(customerId, organizationId),
                                   new ChannelClaim(randomUri(), new ChannelConstraint(
                                       "Everyone", "Everyone", Arrays.asList(scope)
                                   )));
    }

    private Publication persistPublication(Publication publication) throws ApiGatewayException {
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = resourceService.createPublication(userInstance,
                                                                     publication);
        Resource.fromPublication(persistedPublication).publish(resourceService, UserInstance.fromPublication(publication));
        return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
    }

    private static Customer customerNotAllowingPublishingFiles() {
        return new Customer(Set.of(), PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), null);
    }

    private PublishingRequestResolver publishingRequestResolver(Publication publication) {
        return new PublishingRequestResolver(resourceService, ticketService,
                                             identityServiceClient, UserInstance.fromPublication(publication),
                                             customerNotAllowingPublishingFiles());
    }

    private void persistPublishingRequestContainingExistingPendingFiles(Publication publication)
        throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createNewTicket(publication,
                                                                                              PublishingRequestCase.class,
                                                                                              SortableIdentifier::next)
                                                            .withOwner(UserInstance.fromPublication(publication).getUsername())
                                                            .withOwnerAffiliation(
                                                                publication.getResourceOwner().getOwnerAffiliation());
        publishingRequest.withFilesForApproval(TicketTestUtils.getFilesForApproval(publication));
        publishingRequest.persistNewTicket(ticketService);
    }
}
package no.unit.nva.publication.update;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.ticket.test.TicketTestUtils.createPersistedPublicationWithFile;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketStatus;
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

    @BeforeEach
    public void setUp() throws NotFoundException {
        super.init();
        ticketService = getTicketService();
        resourceService = getResourceServiceBuilder().build();
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

        var publishingRequest = getPublishingRequest(publication);

        Assertions.assertEquals(TicketStatus.COMPLETED, publishingRequest.getStatus());
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
        resourceService.publishPublication(UserInstance.fromPublication(publication),
                                           persistedPublication.getIdentifier());
        persistPublishingRequestContainingExistingPendingFiles(persistedPublication);
        var publicationUpdateRemovingUnpublishedFiles = persistedPublication.copy()
                                                            .withAssociatedArtifacts(List.of(openFile))
                                                            .withStatus(PublicationStatus.PUBLISHED)
                                                            .build();
        publishingRequestResolver(persistedPublication).resolve(resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier()),
                                                                publicationUpdateRemovingUnpublishedFiles);

        var publishingRequest = getPublishingRequest(persistedPublication);

        Assertions.assertEquals(TicketStatus.COMPLETED, publishingRequest.getStatus());
        assertTrue(publishingRequest.getFilesForApproval().isEmpty());
        assertTrue(publishingRequest.getApprovedFiles().isEmpty());
    }

    private PublishingRequestCase getPublishingRequest(Publication publication) {
        return resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                   .map(PublishingRequestCase.class::cast)
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

        var filesForApproval = getPublishingRequest(persistedPublication).getFilesForApproval();

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

        var filesForApproval = getPublishingRequest(persistedPublication).getFilesForApproval();

        assertTrue(filesForApproval.contains(randomPendingInternalFile2));
        assertTrue(filesForApproval.contains(randomPendingOpenFile1));
    }

    private Publication persistPublication(Publication publication) throws ApiGatewayException {
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = resourceService.createPublication(userInstance,
                                                                     publication);
        resourceService.publishPublication(userInstance, persistedPublication.getIdentifier());
        return resourceService.getPublicationByIdentifier(persistedPublication.getIdentifier());
    }

    private static Customer customerNotAllowingPublishingFiles() {
        return new Customer(Set.of(), PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), null);
    }

    private PublishingRequestResolver publishingRequestResolver(Publication publication) {
        return new PublishingRequestResolver(resourceService, ticketService, UserInstance.fromPublication(publication),
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
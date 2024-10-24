package no.unit.nva.publication.update;

import static no.unit.nva.publication.ticket.test.TicketTestUtils.createPersistedPublicationWithUnpublishedFiles;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.business.FileForApproval;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @Test
    void shouldApprovePendingPublishingReqeustForInstitutionWhenUserRemovesUnpublishedFiles()
        throws ApiGatewayException {
        var publication = createPersistedPublicationWithUnpublishedFiles(PublicationStatus.PUBLISHED, resourceService);
        persistPublishingRequestContainingExistingUnpublishedFiles(publication);
        var publicationUpdateRemovingUnpublishedFiles = publication.copy()
                                                            .withAssociatedArtifacts(List.of())
                                                            .withStatus(PublicationStatus.PUBLISHED)
                                                            .build();
        publishingRequestResolver(publication).resolve(publication, publicationUpdateRemovingUnpublishedFiles);

        var publishingRequest = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                                    .map(PublishingRequestCase.class::cast)
                                    .toList()
                                    .getFirst();

        Assertions.assertEquals(TicketStatus.COMPLETED, publishingRequest.getStatus());
        Assertions.assertTrue(publishingRequest.getFilesForApproval().isEmpty());
        Assertions.assertTrue(publishingRequest.getApprovedFiles().isEmpty());
    }

    private static Customer customerNotAllowingPublishingFiles() {
        return new Customer(Set.of(), PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), null);
    }

    private PublishingRequestResolver publishingRequestResolver(Publication publication) {
        return new PublishingRequestResolver(resourceService, ticketService, UserInstance.fromPublication(publication),
                                             customerNotAllowingPublishingFiles());
    }

    private TicketEntry persistPublishingRequestContainingExistingUnpublishedFiles(Publication publication)
        throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createNewTicket(publication,
                                                                                              PublishingRequestCase.class,
                                                                                              SortableIdentifier::next)
                                                            .withOwner(UserInstance.fromPublication(publication).getUsername())
                                                            .withOwnerAffiliation(
                                                                publication.getResourceOwner().getOwnerAffiliation());
        publishingRequest.withFilesForApproval(convertUnpublishedFilesToFilesForApproval(publication));
        return publishingRequest.persistNewTicket(ticketService);
    }

    private Set<FileForApproval> convertUnpublishedFilesToFilesForApproval(Publication publication) {
        return publication.getAssociatedArtifacts()
                   .stream()
                   .filter(UnpublishedFile.class::isInstance)
                   .map(UnpublishedFile.class::cast)
                   .map(FileForApproval::fromFile)
                   .collect(Collectors.toSet());
    }
}
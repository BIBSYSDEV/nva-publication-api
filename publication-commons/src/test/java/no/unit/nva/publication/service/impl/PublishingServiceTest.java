package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublishingServiceTest extends ResourcesLocalTest {

    protected static final String OWNER_ONLY = "OwnerOnly";
    protected static final String EVERYONE = "Everyone";
    private ResourceService resourceService;
    private IdentityServiceClient identityServiceClient;
    private PublishingService publishingService;

    @BeforeEach
    void setUp() throws NotFoundException {
        super.init();
        this.resourceService = getResourceService(client);
        var ticketService = getTicketService();
        this.identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getCustomerById(any())).thenReturn(customerWithWorkflow(
            REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue()));
        this.publishingService = new PublishingService(resourceService, ticketService, identityServiceClient);
    }

    @Test
    void shouldPublishResourceWhenPublicationIsPublishable() throws ApiGatewayException {
        var publication = randomPublication(JournalArticle.class).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of())
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        publishingService.publishResource(persistedPublication.getIdentifier(), userInstance);

        var publishedPublication = Resource.fromPublication(persistedPublication).fetch(resourceService);

        assertEquals(PUBLISHED, publishedPublication.orElseThrow().getStatus());
    }

    @Test
    void shouldThrowForbiddenExceptionWhenUserHasNoPermissionToPublishPublication() throws ApiGatewayException {
        var publication = Resource.fromPublication(randomPublication())
                              .persistNew(resourceService, UserInstance.create(randomString(), randomUri()));

        var randomUserInstance = UserInstance.create(randomString(), randomUri());
        assertThrows(ForbiddenException.class,
                     () -> publishingService.publishResource(publication.getIdentifier(), randomUserInstance));
    }

    @Test
    void shouldThrowForbiddenExceptionWhenPublicationToPublishHasChannelClaimedByAnotherCustomerAndIsInScopeWhereOwnerOnlyCanPublish()
        throws ApiGatewayException {
        var instanceType = DegreeBachelor.class;
        var publication = randomPublication(instanceType).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        when(identityServiceClient.getChannelClaim(any())).thenReturn(channelClaim(randomUri(),
                                                                                   randomUri(), getPublisherChannelClaimId(persistedPublication),
                                                                                   OWNER_ONLY,
                                                                                   instanceType.getSimpleName()));
        assertThrows(ForbiddenException.class,
                     () -> publishingService.publishResource(persistedPublication.getIdentifier(), userInstance));
    }

    @Test
    void shouldPublishPublicationWhenPublicationToPublishHasChannelClaimedByUserCustomerAndIsInScopeWhereOwnerOnlyCanPublish()
        throws ApiGatewayException {
        var instanceType = DegreeBachelor.class;
        var publication = randomPublication(instanceType).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        when(identityServiceClient.getChannelClaim(any())).thenReturn(channelClaim(userInstance.getCustomerId(),
                                                                                   userInstance.getTopLevelOrgCristinId(),
                                                                                   getPublisherChannelClaimId(persistedPublication),
                                                                                   OWNER_ONLY,
                                                                                   instanceType.getSimpleName()));

        publishingService.publishResource(persistedPublication.getIdentifier(), userInstance);

        var publishedPublication = Resource.fromPublication(persistedPublication).fetch(resourceService);

        assertEquals(PUBLISHED, publishedPublication.orElseThrow().getStatus());
    }

    @Test
    void shouldPublishPublicationWhenPublicationToPublishHasNonClaimedPublisher()
        throws ApiGatewayException {
        var instanceType = DegreeBachelor.class;
        var publication = randomPublication(instanceType).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        when(identityServiceClient.getChannelClaim(any())).thenThrow(new NotFoundException("Not found"));

        publishingService.publishResource(persistedPublication.getIdentifier(), userInstance);

        var publishedPublication = Resource.fromPublication(persistedPublication).fetch(resourceService);

        assertEquals(PUBLISHED, publishedPublication.orElseThrow().getStatus());
    }

    @Test
    void shouldPublishPublicationWhenPublicationToPublishHasClaimedPublisherButInstanceTypeIsOutOfScope()
        throws ApiGatewayException {
        var instanceType = DegreeBachelor.class;
        var publication = randomPublication(instanceType).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        when(identityServiceClient.getChannelClaim(any())).thenReturn(channelClaim(randomUri(),
                                                                                   randomUri(), getPublisherChannelClaimId(persistedPublication),
                                                                                   EVERYONE,
                                                                                   DegreeMaster.class.getSimpleName()));

        publishingService.publishResource(persistedPublication.getIdentifier(), userInstance);

        var publishedPublication = Resource.fromPublication(persistedPublication).fetch(resourceService);

        assertEquals(PUBLISHED, publishedPublication.orElseThrow().getStatus());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenPublishingNonExistingPublication() {
        var publication = randomPublication();

        assertThrows(NotFoundException.class, () -> publishingService.publishResource(publication.getIdentifier(),
                                                                                      UserInstance.create(
                                                                                          randomString(),
                                                                                          randomUri())));
    }

    @Test
    void shouldPersistPublishingRequestWhenPublicationToPublishHasPendingFiles() throws ApiGatewayException {
        var publication = randomPublication(JournalArticle.class).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        publishingService.publishResource(publication.getIdentifier(), userInstance);

        var ticket = getAllFileApprovals(publication).findFirst();

        assertInstanceOf(PublishingRequestCase.class, ticket.orElseThrow());
    }

    @Test
    void shouldNotPersistPublishingRequestWhenPublicationToPublishHasNoPendingFiles() throws ApiGatewayException {
        var publication = randomPublication(DataSet.class).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of())
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        publishingService.publishResource(publication.getIdentifier(), userInstance);

        var tickets = getAllFileApprovals(publication).toList();

        assertTrue(tickets.isEmpty());
    }

    private Stream<TicketEntry> getAllFileApprovals(Publication publication) {
        return resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                   .filter(FilesApprovalEntry.class::isInstance);
    }

    @Test
    void shouldPersistFilesApprovalThesisForUserInstitutionWhenPublicationToPublishHasPendingFilesAndIsDegreeAndPublisherIsOwnedByUserInstitution()
        throws ApiGatewayException {
        var instanceType = DegreeBachelor.class;
        var publication = randomPublication(instanceType).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        when(identityServiceClient.getChannelClaim(any())).thenReturn(channelClaim(userInstance.getCustomerId(),
                                                                                   userInstance.getTopLevelOrgCristinId(), getPublisherChannelClaimId(publication),
                                                                                   EVERYONE,
                                                                                   instanceType.getSimpleName()));
        publishingService.publishResource(publication.getIdentifier(), userInstance);

        var ticket = (FilesApprovalThesis) getAllFileApprovals(publication).findFirst().orElseThrow();

        assertEquals(ticket.getOwnerAffiliation(), userInstance.getTopLevelOrgCristinId());
        assertEquals(ticket.getResponsibilityArea(), userInstance.getPersonAffiliation());
    }

    @Test
    void shouldPersistFilesApprovalThesisForChannelClaimInstitutionWhenPublicationToPublishHasPendingFilesAndIsDegreeAndPublisherIsOwnedByOtherInstitution()
        throws ApiGatewayException {
        var instanceType = DegreeBachelor.class;
        var publication = randomPublication(instanceType).copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        var channelClaimOwner = randomUri();
        when(identityServiceClient.getChannelClaim(any())).thenReturn(channelClaim(randomUri(),
                                                                                   channelClaimOwner,
                                                                                   getPublisherChannelClaimId(publication), EVERYONE, instanceType.getSimpleName()));
        publishingService.publishResource(publication.getIdentifier(), userInstance);

        var ticket = (FilesApprovalThesis) getAllFileApprovals(publication).findFirst().orElseThrow();

        assertEquals(ticket.getReceivingOrganizationDetails().topLevelOrganizationId(), channelClaimOwner);
        assertEquals(ticket.getReceivingOrganizationDetails().subOrganizationId(), channelClaimOwner);
    }

    @Test
    void shouldPersistDoiRequestWhenPublishingPublicationWithDoi() throws ApiGatewayException {
        var publication = randomPublication();
        var resource = persistResource(Resource.fromPublication(publication));

        publishingService.publishResource(resource.getIdentifier(), UserInstance.fromPublication(publication));

        var doiRequest = getPersistedDoiRequest(resource);

        assertTrue(doiRequest.isPresent());
    }

    private Optional<DoiRequest> getPersistedDoiRequest(Resource resource) {
        return resourceService.fetchAllTicketsForResource(resource)
                   .filter(DoiRequest.class::isInstance)
                   .map(DoiRequest.class::cast)
                   .findFirst();
    }

    private URI getPublisherChannelClaimId(Publication publication) {
        var degree = (Degree) publication.getEntityDescription().getReference().getPublicationContext();
        var publisher = (Publisher) degree.getPublisher();
        return UriWrapper.fromUri(publisher.getId())
                   .replacePathElementByIndexFromEnd(0, StringUtils.EMPTY_STRING)
                   .getUri();
    }

    private ChannelClaimDto channelClaim(URI customerId, URI topLevelOrgCristinId, URI id, String publishingPolicy,
                                         String... scope) {
        return new ChannelClaimDto(randomUri(), new CustomerSummaryDto(customerId, topLevelOrgCristinId),
                                   new ChannelClaim(id, new ChannelConstraint(publishingPolicy, randomString(),
                                                                              Arrays.asList(scope))));
    }

    private CustomerDto customerWithWorkflow(String workflow) {
        return new CustomerDto(RandomDataGenerator.randomUri(), UUID.randomUUID(), randomString(), randomString(),
                               randomString(), RandomDataGenerator.randomUri(),
                               workflow, randomBoolean(),
                               randomBoolean(), randomBoolean(), Collections.emptyList(),
                               new CustomerDto.RightsRetentionStrategy(randomString(),
                                                                       RandomDataGenerator.randomUri()), randomBoolean());
    }
}
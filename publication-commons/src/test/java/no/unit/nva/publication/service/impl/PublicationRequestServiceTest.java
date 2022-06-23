package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static no.unit.nva.publication.TestingUtils.createPublicationForUser;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PublicationRequestServiceTest extends ResourcesLocalTest {

    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant PUBLICATION_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");

    private ResourceService resourceService;
    private PublishingRequestService publicationRequestService;
    private UserInstance owner;

    @BeforeEach
    public void initialize() {
        super.init();
        Clock mockClock = mock(Clock.class);
        this.owner = randomUserInstance();
        when(mockClock.instant())
                .thenReturn(PUBLICATION_CREATION_TIME)
                .thenReturn(PUBLICATION_REQUEST_CREATION_TIME)
                .thenReturn(PUBLICATION_REQUEST_UPDATE_TIME);
        this.resourceService = new ResourceService(client, mockClock);
        this.publicationRequestService = new PublishingRequestService(client, mockClock);
    }

    @Test
    void shouldCreatePublicationRequestWhenPublicationIsPublishable() throws ApiGatewayException {
        var publication = createPublication(owner);
        createPublicationRequest(publication);
        var publicationRequest = getPublicationRequest(publication);
        assertThat(publicationRequest.getCreatedDate(), is(equalTo(PUBLICATION_REQUEST_CREATION_TIME)));
        assertThat(publicationRequest, is(not(nullValue())));
    }

    @Test
    void shouldReturnBadRequestWhenPublicationIsAlreadyPublished() throws ApiGatewayException {
        var publication = createPublishedPublication(owner);
        Executable action = () -> publicationRequestService.createPublishingRequest(owner, publication.getIdentifier());
        assertThrows(BadRequestException.class, action);
    }

    @Test
    void shouldReturnNotFoundWhenPublicationRequestWasNotFound() {
        Executable action = () -> publicationRequestService
                .getPublishingRequest(randomUserInstance(), SortableIdentifier.next());
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldListPublicationRequestsForUserReturnsPublicationRequestsWithStatusPending() throws ApiGatewayException {
        var publication = createPublication(owner);
        createPublicationRequest(publication);
        var result = publicationRequestService.listPublishingRequestsForUser(owner);
        var expectedPublicationRequest =
                publicationRequestService.getPublishingRequest(owner, publication.getIdentifier());
        assertThat(result, is(equalTo(List.of(expectedPublicationRequest))));
    }

    @Test
    void shouldUpdatePublicationRequestUpdatesPublicationRequestStatusInDatabase()
            throws ApiGatewayException {
        var publication = createPublication(owner);
        var userInstance = createUserInstance(publication);
        publicationRequestService.createPublishingRequest(owner, publication.getIdentifier());

        var expectedNewPublicationRequestStatus = PublishingRequestStatus.APPROVED;
        publicationRequestService.updatePublishingRequest(userInstance, publication.getIdentifier(),
                expectedNewPublicationRequestStatus);

        var updatedPublicationRequest = publicationRequestService.getPublishingRequest(userInstance,
                publication.getIdentifier());
        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(expectedNewPublicationRequestStatus)));
    }

    @Test
    void shouldFailWhenPublicationRequestStatusIsSetToRejectedAfterApprove()
            throws ApiGatewayException {
        var publication = createPublication(owner);
        var userInstance = createUserInstance(publication);
        publicationRequestService.createPublishingRequest(owner, publication.getIdentifier());


        var expectedNewPublicationRequestStatus = PublishingRequestStatus.APPROVED;
        publicationRequestService.updatePublishingRequest(userInstance, publication.getIdentifier(),
                expectedNewPublicationRequestStatus);

        var updatedPublicationRequest = publicationRequestService.getPublishingRequest(userInstance,
                publication.getIdentifier());
        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(expectedNewPublicationRequestStatus)));

        Executable action =
                () -> publicationRequestService.updatePublishingRequest(userInstance, publication.getIdentifier(),
                        PublishingRequestStatus.REJECTED);
        assertThrows(IllegalArgumentException.class, action);
    }


    @Test
    void shouldThrowBadRequestExceptionWhenItemUpdateFails() throws ApiGatewayException {

        var publication = createPublication(owner);
        var userInstance = createUserInstance(publication);
        publicationRequestService.createPublishingRequest(owner, publication.getIdentifier());
        var updateItemRequest = mock(UpdateItemRequest.class);
        var publicationRequestServiceSpy = failingPublicationRequestService(updateItemRequest);

        Executable action =
                () -> publicationRequestServiceSpy.updatePublishingRequest(userInstance, publication.getIdentifier(),
                        PublishingRequestStatus.APPROVED);
        assertThrows(no.unit.nva.publication.exception.BadRequestException.class, action);
    }

    @Test
    void shouldThrowDynamoDbExceptionWhenItemUpdateFails() throws ApiGatewayException {

        var publication = createPublication(owner);
        var userInstance = createUserInstance(publication);
        publicationRequestService.createPublishingRequest(owner, publication.getIdentifier());

        var updateItemRequest = mock(UpdateItemRequest.class);

        var publicationRequestServiceSpy =
                failingPublicationRequestServiceBecauseOfEmptyUpdateItemRequest(updateItemRequest);


        Executable action =
                () -> publicationRequestServiceSpy.updatePublishingRequest(userInstance, publication.getIdentifier(),
                        PublishingRequestStatus.APPROVED);
        assertThrows(no.unit.nva.publication.exception.DynamoDBException.class, action);
    }

    private PublishingRequestService failingPublicationRequestServiceBecauseOfEmptyUpdateItemRequest(
            UpdateItemRequest updateItemRequest) throws NotFoundException {
        var publicationRequestServiceSpy = spy(publicationRequestService);

        doReturn(updateItemRequest).when(publicationRequestServiceSpy).createRequestForUpdatingPublishingRequest(
                any(UserInstance.class),
                any(SortableIdentifier.class),
                any(PublishingRequestStatus.class));
        return publicationRequestServiceSpy;
    }

    private PublishingRequestService failingPublicationRequestService(UpdateItemRequest updateItemRequest)
            throws NotFoundException {
        var publicationRequestServiceSpy =
                failingPublicationRequestServiceBecauseOfEmptyUpdateItemRequest(updateItemRequest);
        doReturn(Boolean.TRUE).when(publicationRequestServiceSpy).updateConditionFailed(any());
        return publicationRequestServiceSpy;
    }

    private PublishingRequest getPublicationRequest(Publication publication) throws NotFoundException {
        return publicationRequestService
                .getPublishingRequest(
                        createUserInstance(publication),
                        publication.getIdentifier()
                );
    }

    private Publication createPublication(UserInstance owner) throws ApiGatewayException {
        var publication = createPublicationForUser(owner);
        return resourceService.createPublication(owner, publication);
    }

    private Publication createPublishedPublication(UserInstance owner) throws ApiGatewayException {
        var publication = createPublication(owner);
        publication.setStatus(PublicationStatus.PUBLISHED);
        return resourceService.createPublicationWithStatusFromInput(owner, publication);
    }

    private void createPublicationRequest(Publication publication) throws ApiGatewayException {
        publicationRequestService.createPublishingRequest(createUserInstance(publication), publication.getIdentifier());
    }

    private UserInstance createUserInstance(Publication publication) {
        return UserInstance.create(publication.getResourceOwner().getOwner(), publication.getPublisher().getId());
    }
}

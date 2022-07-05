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

class PublishingRequestServiceTest extends ResourcesLocalTest {

    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant PUBLICATION_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");

    private ResourceService resourceService;
    private PublishingRequestService publishingRequestService;
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
        this.publishingRequestService = new PublishingRequestService(client, mockClock);
    }

    @Test
    void shouldCreatePublicationRequestWhenPublicationIsPublishable() throws ApiGatewayException {
        var publication = createPublication(owner);
        createPublishingRequest(publication);
        var publicationRequest = getPublishingRequest(publication);
        assertThat(publicationRequest.getCreatedDate(), is(equalTo(PUBLICATION_REQUEST_CREATION_TIME)));
        assertThat(publicationRequest, is(not(nullValue())));
    }

    @Test
    void shouldReturnPublishRequestIdentifierWhenCreatingNewPublishRequest() throws ApiGatewayException {
        var publication = createPublication(owner);
        var publishRequestIdentifier = createPublishingRequest(publication);
        var publishRequest = getPublishingRequest(publication);
        assertThat(publishRequest.getCreatedDate(), is(equalTo(PUBLICATION_REQUEST_CREATION_TIME)));
        assertThat(publishRequestIdentifier,is(equalTo(publishRequest.getIdentifier())));
    }

    @Test
    void shouldAllowCreationOfManyPublishingRequestsPerPublication() throws ApiGatewayException {
        var publication = createPublication(owner);
        var firstPublishRequestIdentifier = createPublishingRequest(publication);
        var secondPublishRequestIdentifier = createPublishingRequest(publication);
        var firstPublishingRequest = getPublishingRequest(publication);
        var secondPublishingRequest = getPublishingRequest(publication);
        assertThat(firstPublishingRequest.getIdentifier(),is(not(equalTo(secondPublishingRequest.getIdentifier()))));
    }

    @Test
    void shouldReturnBadRequestWhenPublicationIsAlreadyPublished() throws ApiGatewayException {
        var publication = createPublishedPublication(owner);
        Executable action = () -> publishingRequestService.createPublishingRequest(owner, publication.getIdentifier());
        assertThrows(BadRequestException.class, action);
    }

    @Test
    void shouldReturnNotFoundWhenPublicationRequestWasNotFound() {
        Executable action = () -> publishingRequestService
                .getPublishingRequest(randomUserInstance(), SortableIdentifier.next());
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldListPublicationRequestsForUserReturnsPublicationRequestsWithStatusPending() throws ApiGatewayException {
        var publication = createPublication(owner);
        createPublishingRequest(publication);
        var result = publishingRequestService.listPublishingRequestsForUser(owner);
        var expectedPublicationRequest =
                publishingRequestService.getPublishingRequest(owner, publication.getIdentifier());
        assertThat(result, is(equalTo(List.of(expectedPublicationRequest))));
    }

    @Test
    void shouldUpdatePublicationRequestUpdatesPublicationRequestStatusInDatabase()
            throws ApiGatewayException {
        var publication = createPublication(owner);
        var userInstance = createUserInstance(publication);
        publishingRequestService.createPublishingRequest(owner, publication.getIdentifier());

        var expectedNewPublicationRequestStatus = PublishingRequestStatus.APPROVED;
        publishingRequestService.updatePublishingRequest(userInstance, publication.getIdentifier(),
                                                         expectedNewPublicationRequestStatus);

        var updatedPublicationRequest = publishingRequestService.getPublishingRequest(userInstance,
                                                                                      publication.getIdentifier());
        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(expectedNewPublicationRequestStatus)));
    }

    @Test
    void shouldFailWhenPublicationRequestStatusIsSetToRejectedAfterApprove()
            throws ApiGatewayException {
        var publication = createPublication(owner);
        var userInstance = createUserInstance(publication);
        publishingRequestService.createPublishingRequest(owner, publication.getIdentifier());


        var expectedNewPublicationRequestStatus = PublishingRequestStatus.APPROVED;
        publishingRequestService.updatePublishingRequest(userInstance, publication.getIdentifier(),
                                                         expectedNewPublicationRequestStatus);

        var updatedPublicationRequest = publishingRequestService.getPublishingRequest(userInstance,
                                                                                      publication.getIdentifier());
        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(expectedNewPublicationRequestStatus)));

        Executable action =
                () -> publishingRequestService.updatePublishingRequest(userInstance, publication.getIdentifier(),
                                                                       PublishingRequestStatus.REJECTED);
        assertThrows(IllegalArgumentException.class, action);
    }


    @Test
    void shouldThrowBadRequestExceptionWhenItemUpdateFails() throws ApiGatewayException {

        var publication = createPublication(owner);
        var userInstance = createUserInstance(publication);
        publishingRequestService.createPublishingRequest(owner, publication.getIdentifier());
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
        publishingRequestService.createPublishingRequest(owner, publication.getIdentifier());

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
        var publicationRequestServiceSpy = spy(publishingRequestService);

        doReturn(updateItemRequest).when(publicationRequestServiceSpy).createUpdateDatabaseItemRequest(
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

    private PublishingRequest getPublishingRequest(Publication publication)
        throws NotFoundException {
        return publishingRequestService
            .getPublishingRequest(createUserInstance(publication), publication.getIdentifier());
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

    private SortableIdentifier createPublishingRequest(Publication publication) throws ApiGatewayException {
        return publishingRequestService.createPublishingRequest(createUserInstance(publication),
                                                           publication.getIdentifier());
    }

    private UserInstance createUserInstance(Publication publication) {
        return UserInstance.create(publication.getResourceOwner().getOwner(), publication.getPublisher().getId());
    }
}

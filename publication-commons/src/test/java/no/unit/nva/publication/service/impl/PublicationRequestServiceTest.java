package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.PublicationRequest;
import no.unit.nva.publication.storage.model.PublicationRequestStatus;
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

    private Clock mockClock;
    private ResourceService resourceService;
    private PublicationRequestService publicationRequestService;
    private UserInstance owner;

    @BeforeEach
    public void initialize() {
        super.init();
        this.mockClock = mock(Clock.class);
        this.owner = randomUserInstance();
        when(mockClock.instant())
                .thenReturn(PUBLICATION_CREATION_TIME)
                .thenReturn(PUBLICATION_REQUEST_CREATION_TIME)
                .thenReturn(PUBLICATION_REQUEST_UPDATE_TIME);
        this.resourceService = new ResourceService(client, mockClock);
        this.publicationRequestService = new PublicationRequestService(client, mockClock);
    }

    @Test
    void createPublicationRequestStoresNewPublicationRequestForResource() throws ApiGatewayException {
        Publication publication = createPublication(owner);
        createPublicationRequest(publication);
        PublicationRequest publicationRequest = getPublicationRequest(publication);
        assertThat(publicationRequest.getCreatedDate(), is(equalTo(PUBLICATION_REQUEST_CREATION_TIME)));
        assertThat(publicationRequest, is(not(nullValue())));
    }

    @Test
    void createPublicationRequestThrowsBadRequestForPublishedResource() throws ApiGatewayException {
        Publication publication = createPublishedPublication(owner);
        Executable action = () -> publicationRequestService.createPublicationRequest(publication);
        assertThrows(BadRequestException.class, action);
    }


    @Test
    void getPublicationRequestThrowsNotFoundExceptionWhenPublicationRequestWasNotFound() {
        UserInstance someUser = randomUserInstance();
        Executable action = () -> publicationRequestService
                .getPublicationRequest(someUser, SortableIdentifier.next());
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void listPublicationRequestsForUserReturnsPublicationRequestsWithStatusPending() throws ApiGatewayException {
        Publication publication = createPublication(owner);
        createPublicationRequest(publication);

        List<PublicationRequest> result = publicationRequestService.listPublicationRequestsForUser(owner);

        var expectedPublicationRequest =
                publicationRequestService.getPublicationRequest(owner, publication.getIdentifier());

        assertThat(result, is(equalTo(List.of(expectedPublicationRequest))));
    }

    @Test
    void updatePublicationRequestUpdatesPublicationRequestStatusInDatabase()
            throws ApiGatewayException {
        var publication = createPublication(owner);
        UserInstance userInstance = createUserInstance(publication);
        publicationRequestService.createPublicationRequest(publication);

        PublicationRequestStatus expectedNewPublicationRequestStatus = PublicationRequestStatus.APPROVED;
        publicationRequestService.updatePublicationRequest(userInstance, publication.getIdentifier(), expectedNewPublicationRequestStatus);

        PublicationRequest updatedPublicationRequest = publicationRequestService.getPublicationRequest(userInstance, publication.getIdentifier());
        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(expectedNewPublicationRequestStatus)));
    }

    @Test
    void updatePublicationRequestThrowsBadRequestExceptionWhenItemUpdateFails() throws ApiGatewayException {

        var publication = createPublication(owner);
        UserInstance userInstance = createUserInstance(publication);
        publicationRequestService.createPublicationRequest(publication);

        UpdateItemRequest updateItemRequest = mock(UpdateItemRequest.class);

        PublicationRequestService publicationRequestServiceSpy = spy(publicationRequestService);

        doReturn(updateItemRequest).when(publicationRequestServiceSpy).createRequestForUpdatingPublicationRequest(
                any(UserInstance.class),
                any(SortableIdentifier.class),
                any(PublicationRequestStatus.class));

        doReturn(Boolean.TRUE).when(publicationRequestServiceSpy).updateConditionFailed(any());

        Executable action =
                () -> publicationRequestServiceSpy.updatePublicationRequest(userInstance, publication.getIdentifier(),
                        PublicationRequestStatus.APPROVED);
        assertThrows(no.unit.nva.publication.exception.BadRequestException.class, action);
    }

    @Test
    void updatePublicationRequestThrowsDyamoDbExceptionWhenItemUpdateFails() throws ApiGatewayException {

        var publication = createPublication(owner);
        UserInstance userInstance = createUserInstance(publication);
        publicationRequestService.createPublicationRequest(publication);

        UpdateItemRequest updateItemRequest = mock(UpdateItemRequest.class);

        PublicationRequestService publicationRequestServiceSpy = spy(publicationRequestService);

        doReturn(updateItemRequest).when(publicationRequestServiceSpy).createRequestForUpdatingPublicationRequest(
                any(UserInstance.class),
                any(SortableIdentifier.class),
                any(PublicationRequestStatus.class));


        Executable action =
                () -> publicationRequestServiceSpy.updatePublicationRequest(userInstance, publication.getIdentifier(),
                        PublicationRequestStatus.APPROVED);
        assertThrows(no.unit.nva.publication.exception.DynamoDBException.class, action);
    }


    private PublicationRequest getPublicationRequest(Publication publication) throws NotFoundException {
        return publicationRequestService
                .getPublicationRequest(
                        createUserInstance(publication),
                        publication.getIdentifier()
                );
    }

    private Publication createPublication(UserInstance owner) throws ApiGatewayException {
        Publication publication = createPublicationForUser(owner);
        return resourceService.createPublication(owner, publication);
    }

    private Publication updatePublication( Publication publication ) throws ApiGatewayException {
        return resourceService.updatePublication(publication);
    }


    private Publication createPublishedPublication(UserInstance owner)
            throws ApiGatewayException {
        Publication publication = createPublicationForUser(owner);
        publication = resourceService.createPublication(owner, publication);
        publication.setStatus(PublicationStatus.PUBLISHED);
        return publication;
    }

    private void createPublicationRequest(Publication publication)
            throws TransactionFailedException, BadRequestException {
        publicationRequestService.createPublicationRequest(publication);
    }

    private UserInstance createUserInstance(Publication publication) {
        return UserInstance.create(publication.getResourceOwner().getOwner(), publication.getPublisher().getId());
    }

}

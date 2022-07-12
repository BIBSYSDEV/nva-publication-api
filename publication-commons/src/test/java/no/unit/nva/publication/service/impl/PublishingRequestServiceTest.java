package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.TestingUtils.createPublicationForUser;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

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
        var createdRequest = createPublishingRequest(publication);
        var publishingRequest = publishingRequestService.getPublishingRequest(createdRequest);
        assertThat(publishingRequest.getCreatedDate(), is(equalTo(PUBLICATION_REQUEST_CREATION_TIME)));
        assertThat(publishingRequest, is(not(nullValue())));
        assertThat(publishingRequest, is(equalTo(createdRequest)));
    }

    // This action fails with a TransactionFailedException which contains no information about why the transaction
    // failed, which may fail because of multiple reasons including what we are testing for here.
    @Test
    void shouldThrowExceptionOnMoreThanOnePublishingRequestsForTheSamePublication() throws ApiGatewayException {
        var publication = createPublication(owner);
        attempt(() -> createPublishingRequest(publication))
            .map(created -> publishingRequestService.getPublishingRequest(created))
            .orElseThrow();
        assertThrows(TransactionFailedException.class, () -> createPublishingRequest(publication));
    }

    @Test
    void shouldThrowConflictExceptionWhenPublicationIsAlreadyPublished() throws ApiGatewayException {
        var publication = createPublishedPublication(owner);
        var publishingRequest = TestingUtils.createPublishingRequest(publication);
        Executable action = () -> publishingRequestService.createPublishingRequest(publishingRequest);
        assertThrows(ConflictException.class, action);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenPublicationRequestWasNotFound() {
        var userInstance = UserInstance.create(randomString(), randomUri());
        var queryObject =
            PublishingRequestCase.createQuery(userInstance, SortableIdentifier.next(), SortableIdentifier.next());
        Executable action = () -> publishingRequestService.getPublishingRequest(queryObject);
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldPersistUpdatedStatusWhenPublishingRequestUpdateUpdatesStatus()
        throws ApiGatewayException {
        var publication = createPublication(owner);

        var publishingRequest = createPublishingRequest(publication);
        var expectedNewPublicationRequestStatus = PublishingRequestStatus.APPROVED;
        var requestUpdate = PublishingRequestCase.createStatusUpdate(UserInstance.fromPublication(publication),
                                                                     publication.getIdentifier(),
                                                                     publishingRequest.getIdentifier(),
                                                                     expectedNewPublicationRequestStatus);

        publishingRequestService.updatePublishingRequest(requestUpdate);
        var updatedPublicationRequest = publishingRequestService.getPublishingRequest(requestUpdate);
        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(expectedNewPublicationRequestStatus)));
    }



//    //TODO: add the expected functionality test in the UpdateHandler.
//    @Test
//    void shouldFailWhenPublicationRequestStatusIsSetToRejectedAfterApprove()
//        throws ApiGatewayException {
//        var publication = createPublication(owner);
//        var userInstance = createUserInstance(publication);
//        var publishingRequest =
//            publishingRequestService.createPublishingRequest(TestingUtils.createPublishingRequest(publication));
//
//        var expectedNewPublicationRequestStatus = PublishingRequestStatus.APPROVED;
//        var approvePublishingRequest = PublishingRequestCase.createStatusUpdate(userInstance,
//                                                                                publication.getIdentifier(),
//                                                                                publishingRequest.getIdentifier(),
//                                                                                expectedNewPublicationRequestStatus);
//        publishingRequestService.updatePublishingRequest(approvePublishingRequest);
//
//        var updatedPublicationRequest = publishingRequestService.getPublishingRequest(approvePublishingRequest);
//        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(expectedNewPublicationRequestStatus)));
//        var rejectPublishingRequest = rejectPublishingRequest(updatedPublicationRequest);
//        Executable action = () -> publishingRequestService.updatePublishingRequest(rejectPublishingRequest);
//        assertThrows(IllegalArgumentException.class, action);
//    }

    private PublishingRequestCase rejectPublishingRequest(PublishingRequestCase publishingRequest) {

        var newRequest = new PublishingRequestCase();
        newRequest.setStatus(PublishingRequestStatus.REJECTED);
        newRequest.setResourceIdentifier(publishingRequest.getResourceIdentifier());
        newRequest.setIdentifier(publishingRequest.getIdentifier());
        newRequest.setCreatedDate(publishingRequest.getCreatedDate());
        newRequest.setOwner(publishingRequest.getOwner());
        newRequest.setCustomerId(publishingRequest.getCustomerId());
        newRequest.setRowVersion(publishingRequest.getRowVersion());
        newRequest.setModifiedDate(publishingRequest.getModifiedDate());
        return newRequest;
    }

    private Publication createPublication(UserInstance owner) throws ApiGatewayException {
        var publication = createPublicationForUser(owner);
        return resourceService.createPublication(owner, publication);
    }

    private Publication createPublishedPublication(UserInstance owner) throws ApiGatewayException {
        var publication = createPublication(owner);
        resourceService.publishPublication(owner, publication.getIdentifier());
        return resourceService.getPublication(owner, publication.getIdentifier());
    }

    private PublishingRequestCase createPublishingRequest(Publication publication) throws ApiGatewayException {
        return publishingRequestService.createPublishingRequest(TestingUtils.createPublishingRequest(publication));
    }

    private UserInstance createUserInstance(Publication publication) {
        return UserInstance.create(publication.getResourceOwner().getOwner(), publication.getPublisher().getId());
    }
}

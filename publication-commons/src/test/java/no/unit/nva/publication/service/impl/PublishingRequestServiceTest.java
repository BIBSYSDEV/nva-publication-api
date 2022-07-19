package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.TestingUtils.createPublicationForUser;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingRequestStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.PublishingRequestDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class PublishingRequestServiceTest extends ResourcesLocalTest {
    
    public static final int ONE_FOR_PUBLCATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL = 3;
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
        var requestUpdate = publishingRequest.approve();
        
        publishingRequestService.updatePublishingRequest(requestUpdate);
        var updatedPublicationRequest = publishingRequestService.getPublishingRequest(requestUpdate);
        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(PublishingRequestStatus.APPROVED)));
    }
    
    @Test
    void shouldRetrievePublishingRequestBasedOnPublicationAndPublishingRequestIdentifier() throws ApiGatewayException {
        var publication = createPublication(owner);
        var publishingRequest = createPublishingRequest(publication);
        var retrievedPublishingRequest = publishingRequestService
            .getPublishingRequestByPublicationAndRequestIdentifiers(publication.getIdentifier(),
                publishingRequest.getIdentifier());
        
        assertThat(retrievedPublishingRequest, is(equalTo(publishingRequest)));
    }
    
    @Test
    void shouldRetrieveEventuallyConsistentPublishingRequest() throws ApiGatewayException {
        var client = mock(AmazonDynamoDB.class);
        when(client.transactWriteItems(any())).thenReturn(new TransactWriteItemsResult());
        var mockedResponse = new PublishingRequestDao(randomPublishingRequest()).toDynamoFormat();
        when(client.getItem(any()))
            .thenReturn(new GetItemResult().withItem(mockedPublicationResponse()))
            .thenThrow(ResourceNotFoundException.class)
            .thenReturn(new GetItemResult().withItem(mockedResponse));
        var service = new PublishingRequestService(client, Clock.systemDefaultZone());
        var response = service.createPublishingRequest(randomPublishingRequest());
        var expectedDao = DynamoEntry.parseAttributeValuesMap(mockedResponse, PublishingRequestDao.class);
        var expectedRequest = expectedDao.getData();
        assertThat(response, is(equalTo(expectedRequest)));
        verify(client, times(ONE_FOR_PUBLCATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL)).getItem(any());
    }
    
    @Test
    void shouldRetrievePublishingRequestByCustomerIdAndResourceIdentifier() throws ApiGatewayException {
        var publication = createPublication(owner);
        var publishingRequest = createPublishingRequest(publication);
        var retrievedRequest =
            publishingRequestService.getPublishingRequestByResourceIdentifier(publication.getPublisher().getId(),
                publication.getIdentifier());
        assertThat(retrievedRequest, is(equalTo(publishingRequest)));
    }
    
    private Map<String, AttributeValue> mockedPublicationResponse() {
        var publication = randomPublication();
        publication.setStatus(PublicationStatus.DRAFT);
        var resource = Resource.fromPublication(publication);
        var dao = new ResourceDao(resource);
        return dao.toDynamoFormat();
    }
    
    private PublishingRequestCase randomPublishingRequest() {
        var request = new PublishingRequestCase();
        request.setIdentifier(SortableIdentifier.next());
        request.setOwner(randomString());
        request.setResourceIdentifier(SortableIdentifier.next());
        request.setStatus(PublishingRequestStatus.APPROVED);
        request.setCreatedDate(randomInstant());
        request.setModifiedDate(randomInstant());
        request.setCustomerId(randomUri());
        request.setStatus(randomElement(PublishingRequestStatus.values()));
        return request;
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
}

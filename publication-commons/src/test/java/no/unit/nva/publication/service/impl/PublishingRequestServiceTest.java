package no.unit.nva.publication.service.impl;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.TestingUtils.createUnpersistedPublication;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingRequestStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.PublishingRequestDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;

class PublishingRequestServiceTest extends ResourcesLocalTest {
    
    public static final int ONE_FOR_PUBLICATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL = 3;
    private static final Instant PUBLICATION_CREATION_TIME = randomInstant();
    private static final Instant PUBLICATION_MODIFICATION_TIME = randomInstant(PUBLICATION_CREATION_TIME);
    private static final Instant PUBLICATION_REQUEST_CREATION_TIME = randomInstant(PUBLICATION_MODIFICATION_TIME);
    private static final Instant PUBLICATION_REQUEST_UPDATE_TIME = randomInstant(PUBLICATION_CREATION_TIME);
    private ResourceService resourceService;
    private PublishingRequestService ticketService;
    private UserInstance owner;
    
    public static Stream<Arguments> ticketProvider() {
        return Stream.of(Arguments.of(DoiRequest.class), Arguments.of(PublishingRequestCase.class));
    }
    
    @BeforeEach
    public void initialize() {
        super.init();
        Clock mockClock = mock(Clock.class);
        this.owner = randomUserInstance();
        when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_MODIFICATION_TIME)
            .thenReturn(PUBLICATION_REQUEST_CREATION_TIME)
            .thenReturn(PUBLICATION_REQUEST_UPDATE_TIME);
        this.resourceService = new ResourceService(client, mockClock);
        this.ticketService = new PublishingRequestService(client, mockClock);
    }
    
    @ParameterizedTest(name = "Publication status: {0}")
    @DisplayName("should create Doi Request when Publication is eligible")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "PUBLISHED"}, mode = Mode.INCLUDE)
    void shouldCreateDoiRequestWhenPublicationIsEligible(PublicationStatus status) throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        publication.setStatus(status);
        resourceService.updatePublication(publication);
        publication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket = DoiRequest.fromPublication(publication);
        var persistedTicket = ticketService.createTicket(ticket, DoiRequest.class);
        
        copyServiceControlledFields(ticket, persistedTicket);
        
        assertThat(persistedTicket.getCreatedDate(), is(equalTo(PUBLICATION_REQUEST_CREATION_TIME)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValues());
    }
    
    @ParameterizedTest(name = "Publication status: {0}")
    @DisplayName("should throw Error when Doi is requested for ineligible publication ")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "PUBLISHED"}, mode = Mode.EXCLUDE)
    void shouldThrowErrorWhenDoiIsRequestedForIneligiblePublication(PublicationStatus status)
        throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        publication.setStatus(status);
        resourceService.updatePublication(publication);
        
        publication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket = DoiRequest.fromPublication(publication);
        Executable action = () -> ticketService.createTicket(ticket, DoiRequest.class);
        assertThrows(ConflictException.class, action);
    }
    
    @Test
    void shouldCreatePublishingRequestForDraftPublication() throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        publication.getEntityDescription().setMainTitle(randomString());
        resourceService.updatePublication(publication); // tick the clock
        var ticket = TestingUtils.createPublishingRequest(publication);
        var persistedTicket = ticketService.createTicket(ticket, PublishingRequestCase.class);
        
        copyServiceControlledFields(ticket, persistedTicket);
        assertThat(persistedTicket.getCreatedDate(), is(equalTo(PUBLICATION_REQUEST_CREATION_TIME)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValues());
    }
    
    // This action fails with a TransactionFailedException which contains no information about why the transaction
    // failed, which may fail because of multiple reasons including what we are testing for here.
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should throw Error when mor than one tickets exist for one publication for type")
    @MethodSource("ticketProvider")
    void shouldThrowExceptionOnMoreThanOnePublishingRequestsForTheSamePublication(
        Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        
        var firstTicket = createUnpersistedTicket(ticketType, publication);
        attempt(() -> ticketService.createTicket(firstTicket, ticketType)).orElseThrow();
        
        var secondTicket = createUnpersistedTicket(ticketType, publication);
        Executable action = () -> ticketService.createTicket(secondTicket, ticketType);
        assertThrows(TransactionFailedException.class, action);
    }
    
    @Test
    void shouldThrowConflictExceptionWhenRequestingToPublishAlreadyPublishedPublication() throws ApiGatewayException {
        var publication = createPublishedPublication(owner);
        var publishingRequest = TestingUtils.createPublishingRequest(publication);
        Executable action = () -> ticketService.createTicket(publishingRequest, PublishingRequestCase.class);
        assertThrows(ConflictException.class, action);
    }
    
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should throw NotFound Exception when ticket was not found")
    @MethodSource("ticketProvider")
    void shouldThrowNotFoundExceptionWhenTicketWasNotFound(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        var queryObject = createUnpersistedTicket(ticketType, publication);
        Executable action = () -> ticketService.fetchTicket(queryObject, ticketType);
        assertThrows(NotFoundException.class, action);
    }
    
    @Test
    void shouldPersistUpdatedStatusWhenPublishingRequestUpdateUpdatesStatus()
        throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        var publishingRequest = createPublishingRequest(publication);
        var requestUpdate = publishingRequest.approve();
        
        ticketService.updatePublishingRequest(requestUpdate);
        var updatedPublicationRequest =
            ticketService.fetchTicket(requestUpdate, PublishingRequestCase.class);
        assertThat(updatedPublicationRequest.getStatus(), is(equalTo(PublishingRequestStatus.APPROVED)));
    }
    
    @Test
    void shouldRetrievePublishingRequestBasedOnPublicationAndPublishingRequestIdentifier() throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        var publishingRequest = createPublishingRequest(publication);
        var retrievedPublishingRequest = ticketService
            .fetchTicketByPublicationAndRequestIdentifiers(publication.getIdentifier(),
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
        var response = service.createTicket(randomPublishingRequest(), PublishingRequestCase.class);
        var expectedDao = DynamoEntry.parseAttributeValuesMap(mockedResponse, PublishingRequestDao.class);
        var expectedRequest = expectedDao.getData();
        assertThat(response, is(equalTo(expectedRequest)));
        verify(client, times(ONE_FOR_PUBLICATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL)).getItem(any());
    }
    
    @Test
    void shouldRetrievePublishingRequestByCustomerIdAndResourceIdentifier() throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        var publishingRequest = createPublishingRequest(publication);
        var retrievedRequest =
            ticketService.getTicketByResourceIdentifier(publication.getPublisher().getId(),
                publication.getIdentifier(), PublishingRequestCase.class);
        assertThat(retrievedRequest, is(equalTo(publishingRequest)));
    }
    
    private TicketEntry createUnpersistedTicket(Class<?> ticketType, Publication publication) {
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequest.fromPublication(publication);
        }
        if (PublishingRequestCase.class.equals(ticketType)) {
            return crateRandomPublishingRequest(publication);
        }
        throw new UnsupportedOperationException();
    }
    
    private PublishingRequestCase crateRandomPublishingRequest(Publication publication) {
        var publishingRequest = TestingUtils.createPublishingRequest(publication);
        publishingRequest.setIdentifier(SortableIdentifier.next());
        return publishingRequest;
    }
    
    private void copyServiceControlledFields(TicketEntry doiRequestCreationRequest, TicketEntry persistedTicket) {
        doiRequestCreationRequest.setCreatedDate(persistedTicket.getCreatedDate());
        doiRequestCreationRequest.setModifiedDate(persistedTicket.getModifiedDate());
        doiRequestCreationRequest.setIdentifier(persistedTicket.getIdentifier());
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
    
    private Publication createPersistedPublication(UserInstance owner) throws ApiGatewayException {
        var publication = createUnpersistedPublication(owner);
        return resourceService.createPublication(owner, publication);
    }
    
    private Publication createPublishedPublication(UserInstance owner) throws ApiGatewayException {
        var publication = createPersistedPublication(owner);
        resourceService.publishPublication(owner, publication.getIdentifier());
        return resourceService.getPublication(owner, publication.getIdentifier());
    }
    
    private PublishingRequestCase createPublishingRequest(Publication publication) throws ApiGatewayException {
        return ticketService
            .createTicket(TestingUtils.createPublishingRequest(publication), PublishingRequestCase.class);
    }
}

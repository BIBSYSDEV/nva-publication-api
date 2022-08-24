package no.unit.nva.publication.service.impl;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.TestingUtils.createOrganization;
import static no.unit.nva.publication.TestingUtils.createUnpersistedPublication;
import static no.unit.nva.publication.TestingUtils.randomOrgUnitId;
import static no.unit.nva.publication.TestingUtils.randomPublicationWithoutDoi;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;

class TicketServiceTest extends ResourcesLocalTest {
    
    public static final int ONE_FOR_PUBLICATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL = 3;
    
    private static final Instant TICKET_CREATION_TIME = randomInstant();
    private static final Instant TICKET_UPDATE_TIME = randomInstant(TICKET_CREATION_TIME);
    private ResourceService resourceService;
    private TicketService ticketService;
    private UserInstance owner;
    private Clock clock;
    
    public static Stream<Class<?>> ticketProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    @BeforeEach
    public void initialize() {
        super.init();
        clock = mock(Clock.class);
        this.owner = randomUserInstance();
        when(clock.instant())
            .thenReturn(TICKET_CREATION_TIME)
            .thenReturn(TICKET_UPDATE_TIME);
        this.resourceService = new ResourceService(client, clock);
        this.ticketService = new TicketService(client, clock);
    }
    
    @ParameterizedTest(name = "Publication status: {0}")
    @DisplayName("should create Doi Request when Publication is eligible")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "PUBLISHED"}, mode = Mode.INCLUDE)
    void shouldCreateDoiRequestWhenPublicationIsEligible(PublicationStatus status) throws ApiGatewayException {
        var publication = persistPublication(owner, status);
        publication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket = DoiRequest.fromPublication(publication);
        var persistedTicket = ticketService.createTicket(ticket, DoiRequest.class);
        
        copyServiceControlledFields(ticket, persistedTicket);
        
        assertThat(persistedTicket.getCreatedDate(), is(equalTo(TICKET_CREATION_TIME)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValuesIgnoringFields(Set.of("doi")));
    }
    
    @ParameterizedTest(name = "Publication status: {0}")
    @DisplayName("should throw Error when Doi is requested for ineligible publication ")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "PUBLISHED"}, mode = Mode.EXCLUDE)
    void shouldThrowErrorWhenDoiIsRequestedForIneligiblePublication(PublicationStatus status)
        throws ApiGatewayException {
        var publication = persistPublication(owner, status);
        
        publication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket = DoiRequest.fromPublication(publication);
        Executable action = () -> ticketService.createTicket(ticket, DoiRequest.class);
        assertThrows(ConflictException.class, action);
    }
    
    @Test
    void shouldCreatePublishingRequestForDraftPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TestingUtils.createPublishingRequest(publication);
        var persistedTicket = ticketService.createTicket(ticket, PublishingRequestCase.class);
        
        copyServiceControlledFields(ticket, persistedTicket);
        assertThat(persistedTicket.getCreatedDate(), is(equalTo(TICKET_CREATION_TIME)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValues());
    }
    
    // This action fails with a TransactionFailedException which contains no information about why the transaction
    // failed, which may fail because of multiple reasons including what we are testing for here.
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should throw Error when more than one tickets exist for one publication for type")
    @MethodSource("ticketProvider")
    void shouldThrowExceptionOnMoreThanOnePublishingRequestsForTheSamePublication(
        Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
    
        var firstTicket = createUnpersistedTicket(publication, ticketType);
        attempt(() -> ticketService.createTicket(firstTicket, ticketType)).orElseThrow();
    
        var secondTicket = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> ticketService.createTicket(secondTicket, ticketType);
        assertThrows(TransactionFailedException.class, action);
    }
    
    @Test
    void shouldThrowConflictExceptionWhenRequestingToPublishAlreadyPublishedPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, PUBLISHED);
        var publishingRequest = TestingUtils.createPublishingRequest(publication);
        Executable action = () -> ticketService.createTicket(publishingRequest, PublishingRequestCase.class);
        assertThrows(ConflictException.class, action);
    }
    
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should throw NotFound Exception when ticket was not found")
    @MethodSource("ticketProvider")
    void shouldThrowNotFoundExceptionWhenTicketWasNotFound(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var queryObject = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> ticketService.fetchTicket(queryObject);
        assertThrows(NotFoundException.class, action);
    }
    
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should fetch ticket for user and identifier without specifying the ticket type")
    @MethodSource("ticketProvider")
    void shouldFetchTicketForUserAndIdentifierWithoutSpecifyingTheTicketType(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var persistedTicket = createPersistedTicket(publication, ticketType);
        var userInstance = UserInstance.fromTicket(persistedTicket);
        var ticketIdentifier = persistedTicket.getIdentifier();
        var retrievedTicker = ticketService.fetchTicket(userInstance, ticketIdentifier);
        assertThat(retrievedTicker, is(equalTo(persistedTicket)));
    }
    
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should throw Exception when specified ticket does not belong to requesting user")
    @MethodSource("ticketProvider")
    void shouldThrowExceptionWhenRequestedTicketDoesNotBelongToRequestingUser(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var persistedTicket = createPersistedTicket(publication, ticketType);
        var userInstance = UserInstance.create(randomString(), randomUri());
        var ticketIdentifier = persistedTicket.getIdentifier();
        assertThrows(NotFoundException.class, () -> ticketService.fetchTicket(userInstance, ticketIdentifier));
    }
    
    @Test
    void shouldCreateNewDoiRequestForPublicationWithoutMetadata()
        throws ApiGatewayException {
        var emptyPublication = persistEmptyPublication(owner);
        var doiRequest = DoiRequest.fromPublication(emptyPublication);
        var ticket = ticketService.createTicket(doiRequest, DoiRequest.class);
        var actualDoiRequest = ticketService.fetchTicket(ticket);
        var expectedDoiRequest = expectedDoiRequestForEmptyPublication(emptyPublication, actualDoiRequest);
        
        assertThat(actualDoiRequest, is(equalTo(expectedDoiRequest)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw Exception when user is not the resource owner")
    @MethodSource("ticketProvider")
    void shouldThrowExceptionWhenTheUserIsNotTheResourceOwner(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        publication.setResourceOwner(new ResourceOwner(randomString(), randomUri()));
        var ticket = createUnpersistedTicket(publication, ticketType);
        
        Executable action = () -> ticketService.createTicket(ticket, ticketType);
        assertThrows(ForbiddenException.class, action);
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw Exception when duplicate ticket identifier is created")
    @MethodSource("ticketProvider")
    void shouldThrowExceptionWhenDuplicateTicketIdentifierIsCreated(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var duplicateIdentifier = SortableIdentifier.next();
        ticketService = new TicketService(client, clock, () -> duplicateIdentifier);
        var ticketEntry = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> ticketService.createTicket(ticketEntry, ticketType);
        assertDoesNotThrow(action);
        assertThrows(TransactionFailedException.class, action);
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should persist updated ticket status when ticket status is updated")
    @MethodSource("ticketProvider")
    void shouldPersistUpdatedStatusWhenTicketStatusIsUpdated(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publicationStatus = validPublicationStatusForTicketApproval(ticketType);
        var publication = persistPublication(owner, publicationStatus);
    
        var ticketRequest = createUnpersistedTicket(publication, ticketType);
    
        var persistedTicket = ticketService.createTicket(ticketRequest, ticketType);
    
        ticketService.completeTicket(persistedTicket);
        var updatedTicket = ticketService.fetchTicket(persistedTicket);
    
        var expectedTicket = persistedTicket.copy();
        expectedTicket.setStatus(TicketStatus.COMPLETED);
        expectedTicket.setVersion(updatedTicket.getVersion());
        expectedTicket.setModifiedDate(TICKET_UPDATE_TIME);
    
        assertThat(updatedTicket, is(equalTo(expectedTicket)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should retrieve ticket by Identifier.")
    @MethodSource("ticketProvider")
    void shouldRetrieveTicketByIdentifier(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var expectedTicketEntry = createPersistedTicket(publication, ticketType);
        var actualTicketEntry =
            ticketService.fetchTicketByIdentifier(expectedTicketEntry.getIdentifier());
        
        assertThat(actualTicketEntry, is(equalTo(expectedTicketEntry)));
    }
    
    private PublicationStatus validPublicationStatusForTicketApproval(Class<? extends TicketEntry> ticketType) {
        return PublishingRequestCase.class.equals(ticketType)
                   ? DRAFT
                   : PUBLISHED;
    }
    
    @Test
    void updateDoiRequestThrowsBadRequestExceptionWhenPublicationIsDraft()
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        assertThrows(BadRequestException.class, () -> ticketService.completeTicket(ticket));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should retrieve eventually consistent ticket")
    @MethodSource("ticketProvider")
    void shouldRetrieveEventuallyConsistentTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var client = mock(AmazonDynamoDB.class);
        var expectedTicketEntry = createMockResponsesImitatingEventualConsistency(ticketType, client);
        var service = new TicketService(client, Clock.systemDefaultZone());
        var response = service.createTicket(randomPublishingRequest(), ticketType);
        assertThat(response, is(equalTo(expectedTicketEntry)));
        verify(client, times(ONE_FOR_PUBLICATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL)).getItem(any());
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should retrieve ticket by customer id and resource identifier")
    @MethodSource("ticketProvider")
    void shouldRetrieveTicketByCustomerIdAndResourceIdentifier(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var expectedTicket = createPersistedTicket(publication, ticketType);
        var retrievedRequest =
            ticketService.fetchTicketByResourceIdentifier(
                    publication.getPublisher().getId(),
                    publication.getIdentifier(),
                    ticketType)
                .orElseThrow();
        assertThat(retrievedRequest, is(equalTo(expectedTicket)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should get ticket by identifier without needing to specify type")
    @MethodSource("ticketProvider")
    void shouldGetTicketByIdentifierWithoutNeedingToSpecifyType(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var expectedTicket = createPersistedTicket(publication, ticketType);
        var retrievedRequest = ticketService.fetchTicketByIdentifier(expectedTicket.getIdentifier());
        assertThat(retrievedRequest, is(equalTo(expectedTicket)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw Exception when trying to fetch non existing ticket by identifier")
    @MethodSource("ticketProvider")
    void shouldThrowExceptionWhenTryingToFetchNonExistingTicketByIdentifier(Class<? extends TicketEntry> ignored)
        throws ApiGatewayException {
        persistPublication(owner, DRAFT);
        
        assertThrows(NotFoundException.class, () -> ticketService.fetchTicketByIdentifier(SortableIdentifier.next()));
    }
    
    private TicketEntry createMockResponsesImitatingEventualConsistency(Class<? extends TicketEntry> ticketType,
                                                                        AmazonDynamoDB client) {
        var mockedGetPublicationResponse = new GetItemResult().withItem(mockedPublicationResponse());
        var mockedResponseWhenItemNotYetInPlace = ResourceNotFoundException.class;
    
        var ticketEntry = createUnpersistedTicket(randomPublicationWithoutDoi(), ticketType);
        var mockedResponseWhenItemFinallyInPlace = new GetItemResult().withItem(ticketEntry.toDao().toDynamoFormat());
    
        when(client.transactWriteItems(any())).thenReturn(new TransactWriteItemsResult());
        when(client.getItem(any()))
            .thenReturn(mockedGetPublicationResponse)
            .thenThrow(mockedResponseWhenItemNotYetInPlace)
            .thenReturn(mockedResponseWhenItemFinallyInPlace);
        return ticketEntry;
    }
    
    private TicketEntry createPersistedTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var ticket = createUnpersistedTicket(publication, ticketType);
        return ticketService.createTicket(ticket, ticketType);
    }
    
    private Publication persistEmptyPublication(UserInstance owner) {
        var publication = new Publication.Builder()
                              .withResourceOwner(new ResourceOwner(owner.getUserIdentifier(), randomOrgUnitId()))
                              .withPublisher(createOrganization(owner.getOrganizationUri()))
                              .withStatus(DRAFT)
                              .build();
        
        return resourceService.createPublication(owner, publication);
    }
    
    private DoiRequest expectedDoiRequestForEmptyPublication(Publication emptyPublication,
                                                             TicketEntry actualDoiRequest) {
        return DoiRequest.builder()
                   .withIdentifier(actualDoiRequest.getIdentifier())
                   .withResourceIdentifier(emptyPublication.getIdentifier())
                   .withOwner(emptyPublication.getResourceOwner().getOwner())
                   .withCustomerId(emptyPublication.getPublisher().getId())
                   .withStatus(TicketStatus.PENDING)
                   .withResourceStatus(DRAFT)
                   .withCreatedDate(actualDoiRequest.getCreatedDate())
                   .withModifiedDate(actualDoiRequest.getModifiedDate())
                   .withResourceModifiedDate(emptyPublication.getModifiedDate())
                   .build();
    }
    
    private TicketEntry createUnpersistedTicket(Publication publication, Class<?> ticketType) {
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
        var publication = randomPublicationWithoutDoi().copy().withStatus(DRAFT).build();
        var resource = Resource.fromPublication(publication);
        var dao = new ResourceDao(resource);
        return dao.toDynamoFormat();
    }
    
    private PublishingRequestCase randomPublishingRequest() {
        var request = new PublishingRequestCase();
        request.setIdentifier(SortableIdentifier.next());
        request.setOwner(randomString());
        request.setResourceIdentifier(SortableIdentifier.next());
        request.setStatus(TicketStatus.COMPLETED);
        request.setCreatedDate(randomInstant());
        request.setModifiedDate(randomInstant());
        request.setCustomerId(randomUri());
        request.setStatus(randomElement(TicketStatus.values()));
        return request;
    }
    
    private Publication persistPublication(UserInstance owner, PublicationStatus publicationStatus)
        throws ApiGatewayException {
        var publication = createUnpersistedPublication(owner);
        publication.setStatus(publicationStatus);
        var persistedPublication = resourceService.insertPreexistingPublication(publication);
        
        return resourceService.getPublication(persistedPublication);
    }
}

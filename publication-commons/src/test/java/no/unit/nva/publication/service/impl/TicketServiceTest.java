package no.unit.nva.publication.service.impl;

import static java.lang.StrictMath.ceil;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.TestingUtils.createGeneralSupportRequest;
import static no.unit.nva.publication.TestingUtils.createOrganization;
import static no.unit.nva.publication.TestingUtils.createUnpersistedPublication;
import static no.unit.nva.publication.TestingUtils.randomOrgUnitId;
import static no.unit.nva.publication.TestingUtils.randomPublicationWithoutDoi;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.publication.model.business.TicketStatus.CLOSED;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.model.business.UserInstance.fromTicket;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UntypedTicketQueryObject;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class TicketServiceTest extends ResourcesLocalTest {
    
    public static final int ONE_FOR_PUBLICATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL = 3;
    public static final int TIMEOUT_TEST_IF_LARGE_PAGE_SIZE_IS_SET = 5;
    
    private ResourceService resourceService;
    private TicketService ticketService;
    private UserInstance owner;
    private Instant now;
    private MessageService messageService;
    
    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    public static Stream<Class<?>> uniqueTicketsProvider() {
        return Stream.of(DoiRequest.class, PublishingRequestCase.class);
    }
    
    @BeforeEach
    public void initialize() {
        super.init();
        this.now = Instant.now();
        this.owner = randomUserInstance();
        Clock clock = Clock.systemDefaultZone();
        this.resourceService = new ResourceService(client, clock);
        this.ticketService = new TicketService(client);
        this.messageService = new MessageService(client, clock);
    }
    
    @ParameterizedTest(name = "Publication status: {0}")
    @DisplayName("should create Doi Request when Publication is eligible")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "PUBLISHED"}, mode = Mode.INCLUDE)
    void shouldCreateDoiRequestWhenPublicationIsEligible(PublicationStatus status) throws ApiGatewayException {
        var publication = persistPublication(owner, status);
        publication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket = DoiRequest.fromPublication(publication);
        var persistedTicket = ticket.persistNewTicket(ticketService);
        copyServiceControlledFields(ticket, persistedTicket);
        
        assertThat(persistedTicket.getCreatedDate(), is(greaterThanOrEqualTo(now)));
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
        Executable action = () -> ticket.persistNewTicket(ticketService);
        assertThrows(ConflictException.class, action);
    }
    
    @Test
    void shouldCreatePublishingRequestForDraftPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TestingUtils.createPublishingRequest(publication);
        
        var persistedTicket = ticket.persistNewTicket(ticketService);
        
        copyServiceControlledFields(ticket, persistedTicket);
        assertThat(persistedTicket.getCreatedDate(), is(greaterThanOrEqualTo(now)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValues());
    }
    
    @Test
    void shouldCreateGeneralSupportCaseForAnyPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TestingUtils.createGeneralSupportRequest(publication);
        var persistedTicket = ticket.persistNewTicket(ticketService);
        copyServiceControlledFields(ticket, persistedTicket);
        assertThat(persistedTicket.getCreatedDate(), is(greaterThanOrEqualTo(now)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValues());
    }
    
    // This action fails with a TransactionFailedException which contains no information about why the transaction
    // failed, which may fail because of multiple reasons including what we are testing for here.
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should throw Error when more than one tickets exist for one publication for type")
    @MethodSource("uniqueTicketsProvider")
    void shouldThrowExceptionOnMoreThanOnePublishingRequestsForTheSamePublication(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        
        var firstTicket = createUnpersistedTicket(publication, ticketType);
        attempt(() -> firstTicket.persistNewTicket(ticketService)).orElseThrow();
    
        var secondTicket = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> secondTicket.persistNewTicket(ticketService);
        assertThrows(TransactionFailedException.class, action);
    }
    
    @Test
    void shouldThrowConflictExceptionWhenRequestingToPublishAlreadyPublishedPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, PUBLISHED);
        var ticket = TestingUtils.createPublishingRequest(publication);
        Executable action = () -> ticket.persistNewTicket(ticketService);
        assertThrows(ConflictException.class, action);
    }
    
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should throw NotFound Exception when ticket was not found")
    @MethodSource("ticketTypeProvider")
    void shouldThrowNotFoundExceptionWhenTicketWasNotFound(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var queryObject = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> ticketService.fetchTicket(queryObject);
        assertThrows(NotFoundException.class, action);
    }
    
    @Test
    void shouldPersistAllTypesOfTicketsForAResourceWithoutConflictsAndAlsoBeingAbleToRetrieveAllTickets()
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var tickets = ticketTypeProvider().map(ticketType -> createPersistedTicket(publication, ticketType))
                          .collect(Collectors.toList());
        var retrievedTickets =
            tickets.stream()
                .map(attempt(ticket -> ticketService.fetchTicket(fromTicket(ticket), ticket.getIdentifier())))
                .map(Try::orElseThrow)
                .collect(Collectors.toList());
        assertThat(retrievedTickets, containsInAnyOrder(tickets.toArray(TicketEntry[]::new)));
    }
    
    @ParameterizedTest(name = "type: {0}")
    @DisplayName("should throw Exception when specified ticket does not belong to requesting user")
    @MethodSource("ticketTypeProvider")
    void shouldThrowExceptionWhenRequestedTicketDoesNotBelongToRequestingUser(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var persistedTicket = createPersistedTicket(publication, ticketType);
        var userInstance = UserInstance.create(randomString(), randomUri());
        var ticketIdentifier = persistedTicket.getIdentifier();
        assertThrows(NotFoundException.class, () -> ticketService.fetchTicket(userInstance, ticketIdentifier));
    }
    
    @Test
    void shouldCreateNewDoiRequestForPublicationWithoutMetadata() throws ApiGatewayException {
        var emptyPublication = persistEmptyPublication(owner);
        var doiRequest = DoiRequest.fromPublication(emptyPublication).persistNewTicket(ticketService);
        var actualDoiRequest = ticketService.fetchTicket(doiRequest);
        var expectedDoiRequest = expectedDoiRequestForEmptyPublication(emptyPublication, actualDoiRequest);
        
        assertThat(actualDoiRequest, is(equalTo(expectedDoiRequest)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw Exception when user is not the resource owner")
    @MethodSource("ticketTypeProvider")
    void shouldThrowExceptionWhenTheUserIsNotTheResourceOwner(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        publication.setResourceOwner(new ResourceOwner(randomString(), randomUri()));
        var ticket = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> ticket.persistNewTicket(ticketService);
        assertThrows(ForbiddenException.class, action);
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw Exception when duplicate ticket identifier is created")
    @MethodSource("ticketTypeProvider")
    void shouldThrowExceptionWhenDuplicateTicketIdentifierIsCreated(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var duplicateIdentifier = SortableIdentifier.next();
        ticketService = new TicketService(client, () -> duplicateIdentifier);
        var ticket = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> ticket.persistNewTicket(ticketService);
        assertDoesNotThrow(action);
        assertThrows(TransactionFailedException.class, action);
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should persist updated ticket status when ticket status is updated")
    @MethodSource("ticketTypeProvider")
    void shouldPersistUpdatedStatusWhenTicketStatusIsUpdated(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publicationStatus = validPublicationStatusForTicketApproval(ticketType);
        var publication = persistPublication(owner, publicationStatus);
        var persistedTicket = createUnpersistedTicket(publication, ticketType).persistNewTicket(ticketService);
        
        ticketService.updateTicketStatus(persistedTicket, COMPLETED);
        var updatedTicket = ticketService.fetchTicket(persistedTicket);
        
        var expectedTicket = persistedTicket.copy();
        expectedTicket.setStatus(COMPLETED);
        expectedTicket.setModifiedDate(updatedTicket.getModifiedDate());
        
        assertThat(updatedTicket, is(equalTo(expectedTicket)));
        assertThat(updatedTicket.getModifiedDate(), is(greaterThan(updatedTicket.getCreatedDate())));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should retrieve ticket by Identifier.")
    @MethodSource("ticketTypeProvider")
    void shouldRetrieveTicketByIdentifier(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var expectedTicketEntry = createPersistedTicket(publication, ticketType);
        var actualTicketEntry = ticketService.fetchTicketByIdentifier(expectedTicketEntry.getIdentifier());
        
        assertThat(actualTicketEntry, is(equalTo(expectedTicketEntry)));
    }
    
    @Test
    void shouldThrowBadRequestExceptionWhenTryingToCompleteDoiReqeuestForDraftPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        assertThrows(BadRequestException.class, () -> ticketService.updateTicketStatus(ticket, COMPLETED));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should retrieve eventually consistent ticket")
    @MethodSource("ticketTypeProvider")
    void shouldRetrieveEventuallyConsistentTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var client = mock(AmazonDynamoDB.class);
        var expectedTicketEntry = createMockResponsesImitatingEventualConsistency(ticketType, client);
        var service = new TicketService(client);
        var response = randomPublishingRequest().persistNewTicket(service);
        assertThat(response, is(equalTo(expectedTicketEntry)));
        verify(client, times(ONE_FOR_PUBLICATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL)).getItem(any());
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should retrieve ticket by customer id and resource identifier")
    @MethodSource("ticketTypeProvider")
    void shouldRetrieveTicketByCustomerIdAndResourceIdentifier(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var expectedTicket = createPersistedTicket(publication, ticketType);
        var retrievedRequest = ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
            publication.getIdentifier(), ticketType).orElseThrow();
        assertThat(retrievedRequest, is(equalTo(expectedTicket)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should get ticket by identifier without needing to specify type")
    @MethodSource("ticketTypeProvider")
    void shouldGetTicketByIdentifierWithoutNeedingToSpecifyType(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var expectedTicket = createPersistedTicket(publication, ticketType);
        var retrievedRequest = ticketService.fetchTicketByIdentifier(expectedTicket.getIdentifier());
        assertThat(retrievedRequest, is(equalTo(expectedTicket)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw Exception when trying to fetch non existing ticket by identifier")
    @MethodSource("ticketTypeProvider")
    void shouldThrowExceptionWhenTryingToFetchNonExistingTicketByIdentifier(Class<? extends TicketEntry> ignored)
        throws ApiGatewayException {
        persistPublication(owner, DRAFT);
        
        assertThrows(NotFoundException.class, () -> ticketService.fetchTicketByIdentifier(SortableIdentifier.next()));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should close ticket when ticket is pending and request action is to close the ticket")
    @MethodSource("ticketTypeProvider")
    void shouldCloseTicketWhenTicketIsPendingAndRequestedActionIsToCloseTheTicket(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var pendingTicket = createPersistedTicket(publication, ticketType);
        ticketService.updateTicketStatus(pendingTicket, CLOSED);
        var completedTicket = ticketService.fetchTicket(pendingTicket);
        assertThat(completedTicket.getStatus(), is(equalTo(CLOSED)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should not allow closing ticket when ticket is completed")
    @MethodSource("ticketTypeProvider")
    void shouldNotAllowClosingTicketWhenTicketIsCompleted(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(ticketType));
        var pendingTicket = createPersistedTicket(publication, ticketType);
        
        ticketService.updateTicketStatus(pendingTicket, COMPLETED);
        assertThrows(BadRequestException.class, () -> ticketService.updateTicketStatus(pendingTicket, CLOSED));
        var actualTicket = ticketService.fetchTicket(pendingTicket);
        assertThat(actualTicket.getStatus(), is(equalTo(COMPLETED)));
    }
    
    @Test
    void shouldReturnTheMessagesBelongingToTheTicket() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var publicationOwner = UserInstance.fromPublication(publication);
        var ticketOfInterest = createPersistedTicket(publication, DoiRequest.class);
        var expectedMessage = messageService.createMessage(ticketOfInterest, publicationOwner, randomString());
        var unexpectedMessage = createOtherTicketWithMessage(publication, publicationOwner);
        
        var ticket = ticketService.fetchTicket(publicationOwner, ticketOfInterest.getIdentifier());
        var ticketMessages = ticket.fetchMessages(ticketService);
        assertThat(expectedMessage, is(in(ticketMessages)));
        assertThat(unexpectedMessage, is(not(in(ticketMessages))));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw NotFound Exception when trying to complete non existing ticket for existing publication")
    @MethodSource("ticketTypeProvider")
    void shouldThrowNotFoundExceptionWhenTryingToCompleteNonExistingTicketForExistingPublication(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var nonExisingTicket = createUnpersistedTicket(publication, ticketType);
        assertThrows(NotFoundException.class, () -> ticketService.completeTicket(nonExisingTicket));
    }
    
    //TODO: remove this test when ticket service is in place
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("Legacy functionality: should retrieve tickets that are unique to a publication by "
                 + "publication identifier")
    @MethodSource("uniqueTicketsProvider")
    void shouldCompleteTicketByResourceIdentifierWhenTicketIsUniqueForAPublication(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(ticketType));
        var ticket = createPersistedTicket(publication, ticketType);
        var ticketFetchedByResourceIdentifier = legacyQueryObject(ticketType, publication);
        var completedTicket = ticketService.completeTicket(ticketFetchedByResourceIdentifier);
        var expectedTicket = ticket.copy();
        expectedTicket.setStatus(COMPLETED);
        expectedTicket.setModifiedDate(completedTicket.getModifiedDate());
        assertThat(completedTicket, is(equalTo(expectedTicket)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should update modified date and version when refreshing a ticket")
    @MethodSource("ticketTypeProvider")
    void shouldUpdateModifiedDateAndVersionWhenRefreshing(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, InterruptedException {
        var publication = persistPublication(owner, DRAFT);
        var originalTicket = createPersistedTicket(publication, ticketType);
        var refreshed = ticketService.refreshTicket(originalTicket);
        Thread.sleep(1);
        assertThat(refreshed.getModifiedDate(), is(greaterThan(originalTicket.getModifiedDate())));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should update modified date and version when refreshing a ticket")
    @MethodSource("ticketTypeProvider")
    void shouldReturnTicketForElevatedUserOfSameInstitution(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = createPersistedTicket(publication, ticketType);
        var elevatedUser = UserInstance.create(randomString(), ticket.getCustomerId());
        var retrievedTicket = ticketService.fetchTicketForElevatedUser(elevatedUser, ticket.getIdentifier());
        assertThat(retrievedTicket, is(equalTo(ticket)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should update modified date and version when refreshing a ticket")
    @MethodSource("ticketTypeProvider")
    void shouldThrowNotFoundExceptionWhenUserIsElevatedUserOfAlienInstitution(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = createPersistedTicket(publication, ticketType);
        var elevatedUser = UserInstance.create(randomString(), randomUri());
        Executable action = () -> ticketService.fetchTicketForElevatedUser(elevatedUser, ticket.getIdentifier());
        assertThrows(NotFoundException.class, action);
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should mark ticket as Unread by owner when requested")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsUnreadByOwnerWhenRequested(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = createPersistedTicket(publication, ticketType);
        var owner = ticket.getOwner();
        assertThat(ticket.getViewedBy(), hasItem(owner));
        ticket.copy().markUnreadByOwner().persistUpdate(ticketService);
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), not(hasItem(owner)));
        assertThat(updatedTicket.getModifiedDate(), is(greaterThan(ticket.getModifiedDate())));
    }
    
    @ParameterizedTest(name = "number of tickets:{0}")
    @DisplayName("should list all tickets for a user")
    @Timeout(TIMEOUT_TEST_IF_LARGE_PAGE_SIZE_IS_SET)
    @ValueSource(doubles = {0.5, 1.0, 1.5, 2.0, 2.5})
    void shouldListTicketsForUser(double timesTheResultSetSize) {
        int numberOfTickets = (int) ceil(
            PublicationServiceConfig.RESULT_SET_SIZE_FOR_DYNAMODB_QUERIES * timesTheResultSetSize);
        var expectedTickets = IntStream.range(0, numberOfTickets)
                                  .boxed()
                                  .map(attempt(ignored -> persistPublication(owner, DRAFT)))
                                  .flatMap(Try::stream)
                                  .map(this::persistGeneralSupportRequest)
                                  .collect(Collectors.toList());
    
        var actualTickets = ticketService.fetchTicketsForUser(owner).collect(Collectors.toList());
        assertThat(actualTickets, containsInAnyOrder(expectedTickets.toArray(TicketEntry[]::new)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should mark ticket as Unread for owner")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsUnreadForOwner(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType)
                         .persistNewTicket(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        ticket.markUnreadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(ticket.getOwner())));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should mark ticket as Read for owner")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsReadForOwner(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
        ticket.markUnreadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(ticket.getOwner())));
        ticket.markReadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should mark ticket as read for curator")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsReadForCurator(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType)
                         .persistNewTicket(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT)));
        ticket.markReadForCurators().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should mark ticket as unread for curator")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsUnreadForCurator(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType)
                         .persistNewTicket(ticketService);
        ticket.markReadForCurators().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));
        ticket.markUnreadForCurators().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT)));
    }
    
    @Test
    void shouldReturnEmptyListWhenUserHasNoTickets() throws ApiGatewayException {
        persistPublication(owner, DRAFT);
        var actualTickets = ticketService.fetchTicketsForUser(owner).collect(Collectors.toList());
        assertThat(actualTickets, is(empty()));
    }
    
    @Test
    void shouldReturnAllResultsOfaQuery() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var tickets = IntStream.range(0, 2)
                          .boxed()
                          .map(ticketType -> createPersistedTicket(publication, GeneralSupportRequest.class))
                          .collect(Collectors.toList());
        var query = UntypedTicketQueryObject.create(UserInstance.fromPublication(publication));
        var retrievedTickets = query.fetchTicketsForUser(client).collect(Collectors.toList());
        assertThat(retrievedTickets.size(), is(equalTo(tickets.size())));
    }
    
    private GeneralSupportRequest persistGeneralSupportRequest(Publication publication) {
        return attempt(() -> createGeneralSupportRequest(publication).persistNewTicket(ticketService))
                   .map(GeneralSupportRequest.class::cast).orElseThrow();
    }
    
    private TicketEntry legacyQueryObject(Class<? extends TicketEntry> ticketType, Publication publication) {
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequest.builder()
                       .withCustomerId(publication.getPublisher().getId())
                       .withResourceIdentifier(publication.getIdentifier())
                       .build();
        }
        if (PublishingRequestCase.class.equals(ticketType)) {
            return PublishingRequestCase.createOpeningCaseObject(UserInstance.fromPublication(publication),
                publication.getIdentifier());
        }
        throw new UnsupportedOperationException(
            "Legacy access pattern supported for strictly only DoiRequests and " + "PublishingRequests");
    }
    
    private PublicationStatus validPublicationStatusForTicketApproval(Class<? extends TicketEntry> ticketType) {
        return DoiRequest.class.equals(ticketType) ? PUBLISHED : DRAFT;
    }
    
    private Message createOtherTicketWithMessage(Publication publication, UserInstance publicationOwner) {
        var someOtherTicket = createPersistedTicket(publication, PublishingRequestCase.class);
        return messageService.createMessage(someOtherTicket, publicationOwner, randomString());
    }
    
    private TicketEntry createMockResponsesImitatingEventualConsistency(Class<? extends TicketEntry> ticketType,
                                                                        AmazonDynamoDB client) {
        var mockedGetPublicationResponse = new GetItemResult().withItem(mockedPublicationResponse());
        var mockedResponseWhenItemNotYetInPlace = ResourceNotFoundException.class;
        
        var ticketEntry = createUnpersistedTicket(randomPublicationWithoutDoi(), ticketType);
        var mockedResponseWhenItemFinallyInPlace = new GetItemResult().withItem(ticketEntry.toDao().toDynamoFormat());
        
        when(client.transactWriteItems(any())).thenReturn(new TransactWriteItemsResult());
        when(client.getItem(any())).thenReturn(mockedGetPublicationResponse)
            .thenThrow(mockedResponseWhenItemNotYetInPlace)
            .thenReturn(mockedResponseWhenItemFinallyInPlace);
        return ticketEntry;
    }
    
    private TicketEntry createPersistedTicket(Publication publication, Class<?> ticketType) {
        return attempt(() -> createUnpersistedTicket(publication, ticketType).persistNewTicket(ticketService))
                   .orElseThrow();
    }
    
    private Publication persistEmptyPublication(UserInstance owner) {
        var publication = new Publication.Builder().withResourceOwner(
                new ResourceOwner(owner.getUsername(), randomOrgUnitId()))
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
                   .withOwner(new User(emptyPublication.getResourceOwner().getOwner()))
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
            return createRandomPublishingRequest(publication);
        }
        if (GeneralSupportRequest.class.equals(ticketType)) {
            return createGeneralSupportRequest(publication);
        }
        
        throw new UnsupportedOperationException();
    }
    
    private PublishingRequestCase createRandomPublishingRequest(Publication publication) {
        var publishingRequest = TestingUtils.createPublishingRequest(publication);
        publishingRequest.setIdentifier(SortableIdentifier.next());
        return publishingRequest;
    }
    
    private void copyServiceControlledFields(TicketEntry originalTicket, TicketEntry persistedTicket) {
        originalTicket.setCreatedDate(persistedTicket.getCreatedDate());
        originalTicket.setModifiedDate(persistedTicket.getModifiedDate());
        originalTicket.setIdentifier(persistedTicket.getIdentifier());
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
        request.setOwner(new User(randomString()));
        request.setResourceIdentifier(SortableIdentifier.next());
        request.setStatus(COMPLETED);
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

package no.unit.nva.publication.service.impl;

import static java.lang.StrictMath.ceil;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.TestingUtils.createGeneralSupportRequest;
import static no.unit.nva.publication.TestingUtils.createOrganization;
import static no.unit.nva.publication.TestingUtils.createUnpersistedPublication;
import static no.unit.nva.publication.TestingUtils.randomOrgUnitId;
import static no.unit.nva.publication.TestingUtils.randomPublicationWithoutDoi;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.publication.model.business.TicketStatus.CLOSED;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;
import static no.unit.nva.publication.model.business.UserInstance.fromTicket;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UntypedTicketQueryObject;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TicketServiceTest extends ResourcesLocalTest {

    private static final int ONE_FOR_PUBLICATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL = 3;
    private static final int TIMEOUT_TEST_IF_LARGE_PAGE_SIZE_IS_SET = 5;
    private static final Username USERNAME = new Username(randomString());
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String ASSIGNEE = "assignee";
    private static final String FINALIZED_BY = "finalizedBy";
    private static final String DOI = "doi";

    private ResourceService resourceService;
    private TicketService ticketService;
    private UserInstance owner;
    private Instant now;
    private MessageService messageService;
    public static final String SOME_ASSIGNEE = "some@user";

    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }

    @BeforeEach
    public void initialize() {
        super.init();
        this.now = Instant.now();
        this.owner = randomUserInstance();
        Clock clock = Clock.systemDefaultZone();
        this.resourceService = new ResourceService(client, clock);
        this.ticketService = new TicketService(client);
        this.messageService = new MessageService(client);
    }

    @ParameterizedTest(name = "Publication status: {0}")
    @DisplayName("should create Doi Request when Publication is eligible")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "PUBLISHED",
        "PUBLISHED_METADATA"}, mode = Mode.INCLUDE)
    void shouldCreateDoiRequestWhenPublicationIsEligible(PublicationStatus status) throws ApiGatewayException {
        var publication = persistPublication(owner, status);
        publication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket = DoiRequest.fromPublication(publication);
        var persistedTicket = ticket.persistNewTicket(ticketService);
        copyServiceControlledFields(ticket, persistedTicket);

        assertThat(persistedTicket.getCreatedDate(), is(greaterThanOrEqualTo(now)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValuesIgnoringFields(Set.of(DOI, ASSIGNEE, FINALIZED_BY,
                                                                                FINALIZED_DATE)));
    }

    @ParameterizedTest(name = "Publication status: {0}")
    @DisplayName("should throw Error when Doi is requested for ineligible publication ")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "PUBLISHED", "PUBLISHED_METADATA"},
        mode = Mode.EXCLUDE)
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
        var ticket = PublishingRequestCase.createOpeningCaseObject(publication);
        ticket.setWorkflow(PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var persistedTicket = ticket.persistNewTicket(ticketService);

        copyServiceControlledFields(ticket, persistedTicket);
        assertThat(persistedTicket.getCreatedDate(), is(greaterThanOrEqualTo(now)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValuesIgnoringFields(Set.of(ASSIGNEE, FINALIZED_BY,
                                                                                FINALIZED_DATE)));    }

    @Test
    void shouldCreateGeneralSupportCaseForAnyPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TestingUtils.createGeneralSupportRequest(publication);
        var persistedTicket = ticket.persistNewTicket(ticketService);
        copyServiceControlledFields(ticket, persistedTicket);
        assertThat(persistedTicket.getCreatedDate(), is(greaterThanOrEqualTo(now)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket, doesNotHaveEmptyValuesIgnoringFields(Set.of(ASSIGNEE, FINALIZED_BY,
                                                                                FINALIZED_DATE)));
    }

    // This action fails with a TransactionFailedException which contains no information about why the transaction
    // failed, which may fail because of multiple reasons including what we are testing for here.
    @Test
    void shouldThrowExceptionOnMoreThanOneDoiRequestsForTheSamePublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);

        var firstTicket = createUnpersistedTicket(publication, DoiRequest.class);
        attempt(() -> firstTicket.persistNewTicket(ticketService)).orElseThrow();

        var secondTicket = createUnpersistedTicket(publication, DoiRequest.class);
        Executable action = () -> secondTicket.persistNewTicket(ticketService);
        assertThrows(TransactionFailedException.class, action);
    }

    @Test
    void shouldAllowCreationOfPublishingRequestTicketForAlreadyPublishedPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, PUBLISHED);
        var ticket = PublishingRequestCase.createOpeningCaseObject(publication);
        var actualTicket = ticket.persistNewTicket(ticketService);
        assertThat(actualTicket, is(instanceOf(PublishingRequestCase.class)));
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
        publication.setResourceOwner(new ResourceOwner(randomUsername(), randomUri()));
        var ticket = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> ticket.persistNewTicket(ticketService);
        assertThrows(ForbiddenException.class, action);
    }

    private Username randomUsername() {
        return new Username(randomString());
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

        ticketService.updateTicketStatus(persistedTicket, COMPLETED, USERNAME);
        var updatedTicket = ticketService.fetchTicket(persistedTicket);

        var expectedTicket = persistedTicket.copy();
        expectedTicket.setStatus(COMPLETED);
        expectedTicket.setModifiedDate(updatedTicket.getModifiedDate());
        expectedTicket.setAssignee(USERNAME);
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
        assertThrows(BadRequestException.class, () -> ticketService.updateTicketStatus(ticket, COMPLETED, USERNAME));
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
                                                                             publication.getIdentifier(), ticketType)
            .orElseThrow();
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
        ticketService.updateTicketStatus(pendingTicket, CLOSED, USERNAME);
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

        ticketService.updateTicketStatus(pendingTicket, COMPLETED, USERNAME);
        assertThrows(BadRequestException.class, () -> ticketService.updateTicketStatus(pendingTicket, CLOSED, USERNAME));
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
        assertThrows(NotFoundException.class, () -> ticketService.completeTicket(nonExisingTicket, USERNAME));
    }

    //TODO: remove this test when ticket service is in place
    @Test
    void shouldCompleteTicketByResourceIdentifierWhenTicketIsUniqueForAPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(DoiRequest.class));
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        var ticketFetchedByResourceIdentifier = legacyQueryObject(publication);
        var completedTicket = ticketService.completeTicket(ticketFetchedByResourceIdentifier, USERNAME);
        var expectedTicket = ticket.copy();
        expectedTicket.setStatus(COMPLETED);
        expectedTicket.setModifiedDate(completedTicket.getModifiedDate());
        expectedTicket.setAssignee(USERNAME);
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

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForOwner(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        ticket.markUnreadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(ticket.getOwner())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsReadForOwner(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.markUnreadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(ticket.getOwner())));
        ticket.markReadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsReadForAssignee(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService);
        var ticket = setUpPersistedTicketWithAssignee(ticketType, publication);
        var expectedAssigneeUsername = new User(ticket.getAssignee().toString());
        assertThat(ticket.getViewedBy(), not(hasItem(expectedAssigneeUsername)));
        ticket.markReadForAssignee().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(expectedAssigneeUsername));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForAssignee(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService);
        var ticket = setUpPersistedTicketWithAssignee(ticketType, publication);
        ticket.markReadForAssignee().persistUpdate(ticketService);
        var expectedAssigneeUsername = new User(ticket.getAssignee().toString());
        assertThat(ticket.getViewedBy(), hasItem(expectedAssigneeUsername));
        ticket.markUnReadForAssignee().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(expectedAssigneeUsername)));
    }

    private TicketEntry setUpPersistedTicketWithAssignee(Class<? extends TicketEntry> ticketType, Publication publication)
        throws ApiGatewayException {
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var assigneeUsername = new Username(randomString());
        ticket.setAssignee(assigneeUsername);
        ticket.persistUpdate(ticketService);
        return ticket;
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

    @Test
    void shouldUpdateDenormalizedPublicationTitleInTicketsWhenPublicationIsUpdated() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var originalTickets = createAllTypesOfTickets(publication);

        var updatedPublication = updatePublicationTile(publication);
        var expectedUpdatedTitle = updatedPublication.getEntityDescription().getMainTitle();
        resourceService.updatePublication(updatedPublication);

        var updatedTicketTitle = originalTickets
            .stream()
            .map(attempt(ticket -> ticket.fetch(ticketService)))
            .map(Try::orElseThrow)
            .map(TicketEntry::extractPublicationTitle)
            .collect(Collectors.toSet())
            .stream()
            .collect(SingletonCollector.collect());

        assertThat(updatedTicketTitle, is(equalTo(expectedUpdatedTitle)));
    }

    @Test
    void shouldReturnAllTicketsForPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var originalTickets = createAllTypesOfTickets(publication);
        var fetchedTickets = Resource.fromPublication(publication)
            .fetchAllTickets(resourceService)
            .collect(Collectors.toList());
        assertThat(fetchedTickets, containsInAnyOrder(originalTickets.toArray(TicketEntry[]::new)));
    }

    @Test
    void shouldReturnAllTicketsForPublicationWhenRequesterIsPublicationOwner() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var originalTickets = createAllTypesOfTickets(publication);
        var fetchedTickets = resourceService
            .fetchAllTicketsForPublication(owner, publication.getIdentifier())
            .collect(Collectors.toList());

        assertThat(fetchedTickets, containsInAnyOrder(originalTickets.toArray(TicketEntry[]::new)));
    }

    @Test
    void shouldReturnAllTicketsForPublicationWhenRequesterIsElevatedUser() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var originalTickets = createAllTypesOfTickets(publication);
        var elevatedUser = UserInstance.create(randomString(), publication.getPublisher().getId());
        var fetchedTickets = resourceService
            .fetchAllTicketsForElevatedUser(elevatedUser, publication.getIdentifier())
            .collect(Collectors.toList());
        assertThat(fetchedTickets, containsInAnyOrder(originalTickets.toArray(TicketEntry[]::new)));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAlienElevatedUserAttemptsToFetchPublicationTickets()
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        createAllTypesOfTickets(publication);
        var elevatedUser = UserInstance.create(randomString(), randomUri());
        Executable action =
            () -> resourceService.fetchAllTicketsForElevatedUser(elevatedUser, publication.getIdentifier());
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldReturnEmptyTicketListWhenPublicationHasNoTickets() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var fetchedTickets = Resource.fromPublication(publication).fetchAllTickets(resourceService)
            .collect(Collectors.toList());
        assertThat(fetchedTickets, is(empty()));
    }

    @Test
    void shouldBePossibleToCreateSeveralPublishingRequestTicketsForSinglePublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        assertDoesNotThrow(() -> IntStream.range(0, 2)
            .boxed()
            .map(ticketType -> createPersistedTicket(publication,
                                                     PublishingRequestCase.class))
            .collect(Collectors.toList()));
        var ticketsFromDatabase = resourceService.fetchAllTicketsForElevatedUser(owner, publication.getIdentifier())
            .collect(
                Collectors.toList());
        assertThat(ticketsFromDatabase, allOf(hasSize(2), everyItem(instanceOf(PublishingRequestCase.class))));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should persist updated ticket assignee when ticket assignee is updated")
    @MethodSource("ticketTypeProvider")
    void shouldPersistUpdatedAssigneeWhenTicketAssigneeIsUpdated(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publicationStatus = validPublicationStatusForTicketApproval(ticketType);
        var publication = persistPublication(owner, publicationStatus);
        var persistedTicket = createUnpersistedTicket(publication, ticketType).persistNewTicket(ticketService);
        ticketService.updateTicketAssignee(persistedTicket, getUsername(publication));
        var updatedTicket = ticketService.fetchTicket(persistedTicket);

        var expectedTicket = persistedTicket.copy();
        expectedTicket.setAssignee(getUsername(publication));
        expectedTicket.setModifiedDate(updatedTicket.getModifiedDate());

        assertThat(updatedTicket.getAssignee(), is(equalTo(expectedTicket.getAssignee())));
        assertThat(updatedTicket.getModifiedDate(), is(greaterThan(updatedTicket.getCreatedDate())));
    }

    private static Username getUsername(Publication publication) {
        return new Username(UserInstance.fromPublication(publication).getUsername());
    }

    @Test
    void shouldReturnTicketWithNoAssigneeForDoiRequestForDraftPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        assertThat(ticket.getAssignee(), is(equalTo(null)));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should be able to claim a ticket from ticket with assignee")
    @MethodSource("ticketTypeProvider")
    void shouldClaimTicketFromTicketWithAssignee(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publicationStatus = validPublicationStatusForTicketApproval(ticketType);
        var publication = persistPublication(owner, publicationStatus);
        var persistedTicket = createUnpersistedTicket(publication, ticketType).persistNewTicket(ticketService);
        var assignee = UserInstance.create(SOME_ASSIGNEE, publication.getPublisher().getId());
        persistedTicket.setAssignee(getUsername(publication));

        ticketService.updateTicketAssignee(persistedTicket, new Username(UserInstance.fromTicket(persistedTicket).getUsername()));
        var updatedTicket = ticketService.fetchTicket(persistedTicket);

        var expectedTicket = persistedTicket.copy();
        expectedTicket.setAssignee(getUsername(publication));
        expectedTicket.setModifiedDate(updatedTicket.getModifiedDate());

        assertThat(updatedTicket, is(equalTo(expectedTicket)));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should allow un claiming a ticket with assignee")
    @MethodSource("ticketTypeProvider")
    void shouldAllowUnclaimingTicketWithAssignee(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(ticketType));
        var pendingTicket = createPersistedTicket(publication, ticketType);
        var assignee = UserInstance.create(SOME_ASSIGNEE, publication.getPublisher().getId());
        pendingTicket.setAssignee(getUsername(publication));
        ticketService.updateTicketAssignee(pendingTicket, null);
        var actualTicket = ticketService.fetchTicket(pendingTicket);
        assertThat(actualTicket.getAssignee(), is(equalTo(null)));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @MethodSource("ticketTypeProvider")
    void shouldThrowBadRequestExceptionWhenUpdatingStatusToPending(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(ticketType));
        var pendingTicket = createPersistedTicket(publication, ticketType);
        assertThrows(BadRequestException.class,
                     () -> ticketService.updateTicketStatus(pendingTicket, PENDING, USERNAME));
    }

    private List<TicketEntry> createAllTypesOfTickets(Publication publication) {
        return TicketTestUtils.ticketTypeAndPublicationStatusProvider()
            .map(Arguments::get)
            .filter(argument -> Arrays.asList(argument).get(1) == publication.getStatus())
            .map(arg -> TicketEntry.requestNewTicket(publication,
                                                     (Class<? extends TicketEntry>) Arrays.asList(arg).get(0)))
            .map(attempt(ticket -> ticket.persistNewTicket(ticketService)))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private Publication updatePublicationTile(Publication publication) {
        var copy = publication.copy().build();
        copy.getEntityDescription().setMainTitle(randomString());
        return copy;
    }

    private GeneralSupportRequest persistGeneralSupportRequest(Publication publication) {
        return attempt(() -> createGeneralSupportRequest(publication).persistNewTicket(ticketService))
            .map(GeneralSupportRequest.class::cast).orElseThrow();
    }

    private TicketEntry legacyQueryObject(Publication publication) {
        return DoiRequest.builder()
            .withCustomerId(publication.getPublisher().getId())
            .withPublicationDetails(PublicationDetails.create(publication))
            .build();
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

    private Publication persistEmptyPublication(UserInstance owner) throws BadRequestException {

        var publication = new Publication.Builder().withResourceOwner(
                new ResourceOwner(new Username(owner.getUsername()), randomOrgUnitId()))
            .withPublisher(createOrganization(owner.getOrganizationUri()))
            .withStatus(DRAFT)
            .build();

        return Resource.fromPublication(publication).persistNew(resourceService, owner);
    }

    private DoiRequest expectedDoiRequestForEmptyPublication(Publication emptyPublication,
                                                             TicketEntry actualDoiRequest) {
        return DoiRequest.builder()
            .withIdentifier(actualDoiRequest.getIdentifier())
            .withPublicationDetails(PublicationDetails.create(emptyPublication))
            .withOwner(new User(emptyPublication.getResourceOwner().getOwner().getValue()))
            .withCustomerId(emptyPublication.getPublisher().getId())
            .withStatus(TicketStatus.PENDING)
            .withResourceStatus(DRAFT)
            .withCreatedDate(actualDoiRequest.getCreatedDate())
            .withModifiedDate(actualDoiRequest.getModifiedDate())
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
        var publishingRequest = PublishingRequestCase.createOpeningCaseObject(publication);
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
        request.setPublicationDetails(PublicationDetails.create(randomPublication()));
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
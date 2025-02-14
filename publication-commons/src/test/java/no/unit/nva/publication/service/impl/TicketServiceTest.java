package no.unit.nva.publication.service.impl;

import static java.lang.StrictMath.ceil;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.TestingUtils.createGeneralSupportRequest;
import static no.unit.nva.publication.TestingUtils.createOrganization;
import static no.unit.nva.publication.TestingUtils.createUnpersistedPublication;
import static no.unit.nva.publication.TestingUtils.createUnpublishRequest;
import static no.unit.nva.publication.TestingUtils.randomOrgUnitId;
import static no.unit.nva.publication.TestingUtils.randomPublicationWithoutDoi;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.publication.model.business.TicketStatus.CLOSED;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;
import static no.unit.nva.publication.model.business.TicketStatus.REMOVED;
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
import com.amazonaws.services.dynamodbv2.model.ItemResponse;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
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
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
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
    public static final String SOME_ASSIGNEE = "some@user";
    private static final int TIMEOUT_TEST_IF_LARGE_PAGE_SIZE_IS_SET = 5;
    private static final UserInstance USER_INSTANCE = UserInstance.create(randomString(), randomUri());
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String ASSIGNEE = "assignee";
    private static final String OWNER_AFFILIATION = "ownerAffiliation";
    private static final String FINALIZED_BY = "finalizedBy";
    private static final String DOI = "doi";
    public static final String APPROVED_FILES = "approvedFiles";
    public static final String FILES_FOR_APPROVAL = "filesForApproval";
    private static final String RESPONSIBILITY_AREA = "responsibilityArea";
    private static final String TICKET_EVENT = "ticketEvent";
    private ResourceService resourceService;
    private TicketService ticketService;
    private UserInstance owner;
    private Instant now;
    private MessageService messageService;

    public static Stream<Named<Class<?>>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }

    @BeforeEach
    public void initialize() {
        super.init();
        this.now = Instant.now();
        this.owner = randomUserInstance();
        this.resourceService = getResourceServiceBuilder().build();
        this.ticketService = getTicketService();
        this.messageService = getMessageService();
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
        assertThat(persistedTicket,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(OWNER_AFFILIATION, DOI, ASSIGNEE, FINALIZED_BY,
                                                               FINALIZED_DATE, RESPONSIBILITY_AREA, TICKET_EVENT)));
    }

    @ParameterizedTest(name = "Publication status: {0}")
    @DisplayName("should throw Error when Doi is requested for ineligible publication ")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "PUBLISHED",
        "PUBLISHED_METADATA"}, mode = Mode.EXCLUDE)
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
        var ticket = (PublishingRequestCase) PublishingRequestCase
                                                 .fromPublication(publication)
                                                 .withOwner(randomString());
        ticket.setWorkflow(PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var persistedTicket = ticket.persistNewTicket(ticketService);

        copyServiceControlledFields(ticket, persistedTicket);
        assertThat(persistedTicket.getCreatedDate(), is(greaterThanOrEqualTo(now)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(OWNER_AFFILIATION, ASSIGNEE, FINALIZED_BY,
                                                               FINALIZED_DATE, APPROVED_FILES, FILES_FOR_APPROVAL, RESPONSIBILITY_AREA)));
    }

    @Test
    void shouldCreateGeneralSupportCaseForAnyPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = TestingUtils.createGeneralSupportRequest(publication);
        var persistedTicket = ticket.persistNewTicket(ticketService);
        copyServiceControlledFields(ticket, persistedTicket);
        assertThat(persistedTicket.getCreatedDate(), is(greaterThanOrEqualTo(now)));
        assertThat(persistedTicket, is(equalTo(ticket)));
        assertThat(persistedTicket,
                   doesNotHaveEmptyValuesIgnoringFields(Set.of(OWNER_AFFILIATION, ASSIGNEE, FINALIZED_BY,
                                                               FINALIZED_DATE, RESPONSIBILITY_AREA)));
    }

    @Test
    void shouldAllowCreationOfPublishingRequestTicketForAlreadyPublishedPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, PUBLISHED);
        var ticket = PublishingRequestCase.fromPublication(publication)
                         .withOwner(randomString())
                         .persistNewTicket(ticketService);
        assertThat(ticket, is(instanceOf(PublishingRequestCase.class)));
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
        var tickets = ticketTypeProvider()
                          .map(ticketType -> createPersistedTicket(publication, ticketType.getPayload()))
            .toList();
        var retrievedTickets =
            tickets.stream()
                .map(attempt(ticket -> ticketService.fetchTicket(fromTicket(ticket), ticket.getIdentifier())))
                .map(Try::orElseThrow)
                .toList();
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
    void shouldNotThrowExceptionWhenTheUserIsNotTheResourceOwner(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        publication.setResourceOwner(new ResourceOwner(randomUsername(), randomUri()));
        var ticket = createUnpersistedTicket(publication, ticketType);

        assertDoesNotThrow(() -> ticket.persistNewTicket(ticketService));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw Exception when duplicate ticket identifier is created")
    @MethodSource("ticketTypeProvider")
    void shouldThrowExceptionWhenDuplicateTicketIdentifierIsCreated(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var duplicateIdentifier = SortableIdentifier.next();
        ticketService = new TicketService(client, () -> duplicateIdentifier, uriRetriever);
        var ticket = createUnpersistedTicket(publication, ticketType);
        Executable action = () -> ticket.withOwner(randomString()).persistNewTicket(ticketService);
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
        var persistedTicket = createUnpersistedTicket(publication, ticketType)
                                  .withOwner(randomString())
                                  .persistNewTicket(ticketService);

        ticketService.updateTicketStatus(persistedTicket, COMPLETED, USER_INSTANCE);
        var updatedTicket = ticketService.fetchTicket(persistedTicket);

        var expectedTicket = persistedTicket.copy();
        expectedTicket.setStatus(COMPLETED);
        expectedTicket.setModifiedDate(updatedTicket.getModifiedDate());
        expectedTicket.setAssignee(new Username(USER_INSTANCE.getUsername()));
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
    void shouldThrowBadRequestExceptionWhenTryingToCompleteDoiRequestForDraftPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        assertThrows(BadRequestException.class, () -> ticketService.updateTicketStatus(ticket, COMPLETED, USER_INSTANCE));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should retrieve ticket by customerId id and resource identifier")
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
        ticketService.updateTicketStatus(pendingTicket, CLOSED, USER_INSTANCE);
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

        ticketService.updateTicketStatus(pendingTicket, COMPLETED, USER_INSTANCE);
        assertThrows(BadRequestException.class,
                     () -> ticketService.updateTicketStatus(pendingTicket, CLOSED, USER_INSTANCE));
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
    @DisplayName("should retrieve eventually consistent ticket")
    @MethodSource("ticketTypeProvider")
    void shouldRetrieveEventuallyConsistentTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var client = mock(AmazonDynamoDB.class);
        var expectedTicketEntry = createMockResponsesImitatingEventualConsistency(ticketType, client);
        var service = new TicketService(client, uriRetriever);
        var response = randomPublishingRequest().persistNewTicket(service);
        assertThat(response, is(equalTo(expectedTicketEntry)));
        verify(client, times(ONE_FOR_PUBLICATION_ONE_FAILING_FOR_NEW_CASE_AND_ONE_SUCCESSFUL)).getItem(any());
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should throw NotFound Exception when trying to complete non existing ticket for existing publication")
    @MethodSource("ticketTypeProvider")
    void shouldThrowNotFoundExceptionWhenTryingToCompleteNonExistingTicketForExistingPublication(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var nonExisingTicket = createUnpersistedTicket(publication, ticketType);
        assertThrows(NotFoundException.class, () -> ticketService.completeTicket(nonExisingTicket, USER_INSTANCE));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @MethodSource("ticketTypeProvider")
    void shouldNotUpdateAssigneeWhenCompletingTicketForAnotherAssignee(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, PUBLISHED);
        var ticket = createPersistedTicket(publication, ticketType);
        ticket.setAssignee(randomUsername());
        ticket.persistUpdate(ticketService);
        var completedTicket = ticketService.completeTicket(ticket, USER_INSTANCE);

        assertThat(completedTicket.getAssignee(), is(not(equalTo(USER_INSTANCE))));
    }

    //TODO: remove this test when ticket service is in place
    @Test
    void shouldCompleteTicketByResourceIdentifierWhenTicketIsUniqueForAPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(DoiRequest.class));
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        var ticketFetchedByResourceIdentifier = legacyQueryObject(publication);
        var completedTicket = ticketService.completeTicket(ticketFetchedByResourceIdentifier, USER_INSTANCE);
        var expectedTicket = ticket.copy();
        expectedTicket.setStatus(COMPLETED);
        expectedTicket.setModifiedDate(completedTicket.getModifiedDate());
        expectedTicket.setAssignee(new Username(USER_INSTANCE.getUsername()));
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
                                  .toList();

        var actualTickets = ticketService.fetchTicketsForUser(owner).toList();
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

    @Test
    void shouldReturnEmptyListWhenUserHasNoTickets() throws ApiGatewayException {
        persistPublication(owner, DRAFT);
        var actualTickets = ticketService.fetchTicketsForUser(owner).toList();
        assertThat(actualTickets, is(empty()));
    }

    @Test
    void shouldReturnAllResultsOfaQuery() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var tickets = IntStream.range(0, 2)
            .boxed()
            .map(ticketType -> createPersistedTicket(publication, GeneralSupportRequest.class))
            .toList();
        var query = UntypedTicketQueryObject.create(UserInstance.fromPublication(publication));
        var retrievedTickets = query.fetchTicketsForUser(client).toList();
        assertThat(retrievedTickets.size(), is(equalTo(tickets.size())));
    }

    @Test
    void shouldReturnAllTicketsForPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var originalTickets = createAllTypesOfTickets(publication);
        var fetchedTickets = Resource.fromPublication(publication)
                                 .fetchAllTickets(resourceService)
                                 .toList();
        assertThat(fetchedTickets, containsInAnyOrder(originalTickets.toArray(TicketEntry[]::new)));
    }

    @Test
    void shouldReturnAllTicketsForPublicationWhenRequesterIsPublicationOwner() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var originalTickets = createAllTypesOfTickets(publication);
        var fetchedTickets = resourceService.fetchAllTicketsForPublication(owner, publication.getIdentifier())
                                 .toList();

        assertThat(fetchedTickets, containsInAnyOrder(originalTickets.toArray(TicketEntry[]::new)));
    }

    @Test
    void shouldReturnEmptyTicketListWhenPublicationHasNoTickets() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var fetchedTickets = Resource.fromPublication(publication)
                                 .fetchAllTickets(resourceService)
                                 .toList();
        assertThat(fetchedTickets, is(empty()));
    }

    @Test
    void shouldBePossibleToCreateSeveralPublishingRequestTicketsForSinglePublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        assertDoesNotThrow(() -> IntStream.range(0, 2)
                                     .boxed()
                                     .map(ticketType -> createPersistedTicket(publication, PublishingRequestCase.class))
                                     .toList());
        var ticketsFromDatabase = resourceService.fetchAllTicketsForPublication(owner, publication.getIdentifier())
                                      .toList();
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

    @Test
    void shouldReturnTicketWithNoAssigneeForDoiRequestForDraftPublication() throws ApiGatewayException {
        var publication = persistPublication(owner, DRAFT);
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        assertThat(ticket.getAssignee(), is(equalTo(null)));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should be able to claim a ticket from ticket with assignee")
    @MethodSource("ticketTypeProvider")
    void shouldClaimTicketFromTicketWithAssignee(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publicationStatus = validPublicationStatusForTicketApproval(ticketType);
        var publication = persistPublication(owner, publicationStatus);
        var persistedTicket = createUnpersistedTicket(publication, ticketType)
                                  .withOwner(UserInstance.fromPublication(publication).getUsername())
                                  .persistNewTicket(ticketService);
        persistedTicket.setAssignee(getUsername(publication));

        ticketService.updateTicketAssignee(persistedTicket,
                                           new Username(UserInstance.fromTicket(persistedTicket).getUsername()));
        var updatedTicket = ticketService.fetchTicket(persistedTicket);

        var expectedTicket = persistedTicket.copy();
        expectedTicket.setAssignee(getUsername(publication));
        expectedTicket.setModifiedDate(updatedTicket.getModifiedDate());

        assertThat(updatedTicket, is(equalTo(expectedTicket)));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should allow un claiming a ticket with assignee")
    @MethodSource("ticketTypeProvider")
    void shouldAllowUnclaimingTicketWithAssignee(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(ticketType));
        var pendingTicket = createPersistedTicket(publication, ticketType);
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
                     () -> ticketService.updateTicketStatus(pendingTicket, PENDING, USER_INSTANCE));
    }

    @Test
    void publishingRequestCaseShouldBeAutoCompleted() throws ApiGatewayException {
        var ticketType = PublishingRequestCase.class;
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(ticketType));
        var ticket = TicketTestUtils.createNonPersistedTicket(publication, ticketType);

        var persistedCompletedTicket = ((PublishingRequestCase) ticket)
                                           .persistAutoComplete(ticketService,
                                                                publication,
                                                                UserInstance.create(owner.getUsername(), randomUri()));

        assertThat(persistedCompletedTicket.getStatus(), is(equalTo(COMPLETED)));
    }

    @ParameterizedTest(name = "ticket type:{0}")
    @MethodSource("ticketTypeProvider")
    void shouldBeAbleToRemoveTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(ticketType));
        var ticket = TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                         .withOwner(randomString())
                         .persistNewTicket(ticketService);
        ticket.remove(UserInstance.fromTicket(ticket)).persistUpdate(ticketService);

        var persistedTicket = ticket.fetch(ticketService);
        assertThat(persistedTicket.getStatus(), is(equalTo(REMOVED)));
    }

    @Test
    void shouldBeAbleToRemoveDoiRequestAndCreateNewDoiRequest() throws ApiGatewayException {
        var ticketType = DoiRequest.class;
        var publication = persistPublication(owner, validPublicationStatusForTicketApproval(ticketType));
        var ticket = TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                         .persistNewTicket(ticketService);
        ticket.remove(UserInstance.fromTicket(ticket)).persistUpdate(ticketService);

        var secondTicket = TicketEntry.createNewTicket(publication, ticketType, SortableIdentifier::next)
                       .persistNewTicket(ticketService);
        assertThat(secondTicket.getStatus(), is(equalTo(PENDING)));
    }

    @Test
    void shouldSetFilesForApprovalOnPublishingRequestCreationWhenPublicationHasPendingOpenFile()
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(DRAFT, resourceService);
        var ticket = persistPublishingRequestContainingExistingUnpublishedFiles(publication);

        var expectedFilesForApproval = publication.getAssociatedArtifacts().stream()
                                           .filter(PendingOpenFile.class::isInstance)
                                           .map(PendingOpenFile.class::cast)
                                           .toArray();

        assertThat(((PublishingRequestCase) ticket).getFilesForApproval(),
                   containsInAnyOrder(expectedFilesForApproval));
    }

    @Test
    void shouldRefreshTicketByUpdatingVersion()
        throws ApiGatewayException {
        var ticketType = PublishingRequestCase.class;
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(DRAFT, resourceService);
        var ticket = PublishingRequestCase.createNewTicket(publication, ticketType, SortableIdentifier::next)
                         .withOwner(randomString())
                         .persistNewTicket(ticketService);
        var version = ticket.toDao().getVersion();
        ticketService.refresh(ticket.getIdentifier());
        var updatedTicket = ticketService.fetchTicket(ticket);
        var updatedVersion = updatedTicket.toDao().getVersion();

        assertThat(updatedVersion, is(not(equalTo(version))));
    }

    @Test
    void finalizedDateShouldBeEqualCreatedDateWhenAutoCompletingTicket() throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(DRAFT, resourceService);
        var ticket = (PublishingRequestCase) PublishingRequestCase.createNewTicket(
            publication, PublishingRequestCase.class, SortableIdentifier::next).withOwner(randomString());
        var completedTicket = ticket.persistAutoComplete(ticketService, publication,
                                                         UserInstance.create(randomString(), randomUri()));

        assertThat(completedTicket.getFinalizedDate(), is(equalTo(completedTicket.getCreatedDate())));
    }

    @Test
    void shouldConvertUnpublishRequestToPublication() throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(DRAFT, resourceService);
        var unpublishRequest = UnpublishRequest.createNewUnpublishRequest(publication,SortableIdentifier::next);
        var publicationFromUnpublishRequest = unpublishRequest.toPublication(resourceService);

        assertThat(publicationFromUnpublishRequest, is(equalTo(publication)));
    }

    private static Username getUsername(Publication publication) {
        return new Username(UserInstance.fromPublication(publication).getUsername());
    }

    private Username randomUsername() {
        return new Username(randomString());
    }

    private TicketEntry setUpPersistedTicketWithAssignee(Class<? extends TicketEntry> ticketType,
                                                         Publication publication) throws ApiGatewayException {
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setAssignee(new Username(SOME_ASSIGNEE));
        ticket.persistUpdate(ticketService);
        return ticket;
    }

    private List<TicketEntry> createAllTypesOfTickets(Publication publication) {
        return TicketTestUtils.ticketTypeAndPublicationStatusProvider()
                   .map(Arguments::get)
                   .filter(argument -> Arrays.asList(argument).get(1) == publication.getStatus())
                   .map(arg -> TicketEntry.requestNewTicket(publication,
                                                            (Class<? extends TicketEntry>) Arrays.asList(arg).getFirst()))
                   .map(attempt(ticket -> ticket.withOwner(randomString()).persistNewTicket(ticketService)))
                   .map(Try::orElseThrow)
                   .toList();
    }

    private GeneralSupportRequest persistGeneralSupportRequest(Publication publication) {
        return attempt(() -> createGeneralSupportRequest(publication).persistNewTicket(ticketService)).map(
            GeneralSupportRequest.class::cast).orElseThrow();
    }

    private TicketEntry legacyQueryObject(Publication publication) {
        return DoiRequest.builder()
                   .withCustomerId(publication.getPublisher().getId())
                   .withResourceIdentifier(publication.getIdentifier())
                   .build();
    }

    private PublicationStatus validPublicationStatusForTicketApproval(Class<? extends TicketEntry> ticketType) {
        return DoiRequest.class.equals(ticketType) ? PUBLISHED : DRAFT;
    }

    private Message createOtherTicketWithMessage(Publication publication, UserInstance publicationOwner) {
        var someOtherTicket = createPersistedTicket(publication, PublishingRequestCase.class);
        return messageService.createMessage(someOtherTicket, publicationOwner, randomString());
    }

    private TicketEntry createPersistedTicket(Publication publication, Class<?> ticketType) {
        return attempt(
            () -> createUnpersistedTicket(publication, ticketType)
                      .withOwner(UserInstance.fromPublication(publication).getUsername())
                      .persistNewTicket(ticketService)).orElseThrow();
    }

    private Publication persistEmptyPublication(UserInstance owner) throws BadRequestException {

        var publication = new Publication.Builder().withResourceOwner(
                new ResourceOwner(new Username(owner.getUsername()), randomOrgUnitId()))
                              .withPublisher(createOrganization(owner.getCustomerId()))
                              .withStatus(DRAFT)
                              .build();

        return Resource.fromPublication(publication).persistNew(resourceService, owner);
    }

    private DoiRequest expectedDoiRequestForEmptyPublication(Publication emptyPublication,
                                                             TicketEntry actualDoiRequest) {
        return DoiRequest.builder()
                   .withIdentifier(actualDoiRequest.getIdentifier())
                   .withResourceIdentifier(emptyPublication.getIdentifier())
                   .withOwner(new User(emptyPublication.getResourceOwner().getOwner().getValue()))
                   .withCustomerId(emptyPublication.getPublisher().getId())
                   .withStatus(TicketStatus.PENDING)
                   .withResourceStatus(DRAFT)
                   .withCreatedDate(actualDoiRequest.getCreatedDate())
                   .withModifiedDate(actualDoiRequest.getModifiedDate())
                   .build();
    }

    private TicketEntry createUnpersistedTicket(Publication publication, Class<?> ticketType) {
        var owner = UserInstance.fromPublication(publication).getUsername();
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequest.fromPublication(publication).withOwner(owner);
        }
        if (PublishingRequestCase.class.equals(ticketType)) {
            return createRandomPublishingRequest(publication).withOwner(owner);
        }
        if (GeneralSupportRequest.class.equals(ticketType)) {
            return createGeneralSupportRequest(publication).withOwner(owner);
        }
        if (UnpublishRequest.class.equals(ticketType)) {
            return createUnpublishRequest(publication).withOwner(owner);
        }

        throw new UnsupportedOperationException();
    }

    private PublishingRequestCase createRandomPublishingRequest(Publication publication) {
        var publishingRequest = PublishingRequestCase.fromPublication(publication);
        publishingRequest.setIdentifier(SortableIdentifier.next());
        return publishingRequest;
    }

    private void copyServiceControlledFields(TicketEntry originalTicket, TicketEntry persistedTicket) {
        originalTicket.setCreatedDate(persistedTicket.getCreatedDate());
        originalTicket.setModifiedDate(persistedTicket.getModifiedDate());
        originalTicket.setIdentifier(persistedTicket.getIdentifier());
    }

    private Publication persistPublication(UserInstance owner, PublicationStatus publicationStatus)
        throws ApiGatewayException {
        var publication = createUnpersistedPublication(owner);
        publication.setStatus(publicationStatus);
        publication.setCuratingInstitutions(getCuratingInstitutions(publication));
        var persistedPublication = resourceService.insertPreexistingPublication(publication);

        return resourceService.getPublication(persistedPublication);
    }

    private TicketEntry createMockResponsesImitatingEventualConsistency(Class<? extends TicketEntry> ticketType,
                                                                        AmazonDynamoDB client) {

        var publication = mockedPublicationResponse();
        var mockedGetPublicationResponse = new GetItemResult().withItem(publication);
        new TransactGetItemsResult().withResponses(new ItemResponse().withItem(mockedPublicationResponse()));
        var ticketEntry = createUnpersistedTicket(randomPublicationWithoutDoi(), ticketType);
        var mockedResponseWhenItemFinallyInPlace = new GetItemResult().withItem(ticketEntry.toDao().toDynamoFormat());

        when(client.transactWriteItems(any())).thenReturn(new TransactWriteItemsResult());
        when(client.getItem(any())).thenReturn(mockedGetPublicationResponse)
            .thenThrow(RuntimeException.class)
            .thenReturn(mockedResponseWhenItemFinallyInPlace);

        var queryResult = new QueryResult().withItems(publication);
        when(client.query(any(QueryRequest.class))).thenReturn(queryResult);

        return ticketEntry;
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

    private static Set<CuratingInstitution> getCuratingInstitutions(Publication publication) {
        return publication.getEntityDescription().getContributors().stream()
                   .map(Contributor::getAffiliations)
                   .flatMap(Collection::stream)
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId)
                   .map(id -> new CuratingInstitution(id, Set.of()))
                   .collect(Collectors.toSet());
    }

    private TicketEntry persistPublishingRequestContainingExistingUnpublishedFiles(Publication publication)
        throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createNewTicket(publication, PublishingRequestCase.class,
                                                                                              SortableIdentifier::next)
                                                            .withOwner(UserInstance.fromPublication(publication).getUsername())
                                                            .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation())
                                                            .withOwnerResponsibilityArea(randomUri());
        publishingRequest.withFilesForApproval(TicketTestUtils.getFilesForApproval(publication));
        return publishingRequest.persistNewTicket(ticketService);
    }

}
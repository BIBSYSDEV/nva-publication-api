package no.unit.nva.expansion;

import static no.unit.nva.expansion.model.ExpandedTicket.extractIdentifier;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedGeneralSupportRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.expansion.model.ExpandedPublishingRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.expansion.model.ExpandedTicketStatus;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceExpansionServiceTest extends ResourcesLocalTest {

    public static final URI ORGANIZATION =
        URI.create("https://api.dev.nva.aws.unit.no/cristin/person/myCristinId/myOrganization");
    public static final UserInstance USER = UserInstance.create(new User("12345"), ORGANIZATION);
    private static final Clock CLOCK = Clock.systemDefaultZone();
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String WORKFLOW = "workflow";
    private static final String ASSIGNEE = "assignee";
    private static final String FINALIZED_BY = "finalizedBy";
    private ResourceExpansionService expansionService;
    private ResourceService resourceService;
    private MessageService messageService;
    private TicketService ticketService;
    private UriRetriever uriRetriever;

    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }

    @BeforeEach
    void setUp() {
        super.init();
        initializeServices();
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingTheOrganizationIdOfTheOwnersAffiliationAsIs(
        Class<? extends TicketEntry> ticketType,
        PublicationStatus status)
        throws Exception {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var userAffiliation = publication.getResourceOwner().getOwnerAffiliation();
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(userAffiliation, is(in(expandedTicket.getOrganizationIds())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnExpandedTicketContainingFinalizedByValue(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException,
                                                                                  JsonProcessingException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setFinalizedBy(new Username(randomString()));
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(ticket.getFinalizedBy(), is(equalTo(expandedTicket.getFinalizedBy())));
    }

    @DisplayName("should copy all publicly visible fields from Ticket to ExpandedTicket")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldCopyAllPubliclyVisibleFieldsFromTicketToExpandedTicket(Class<? extends TicketEntry> ticketType,
                                                                      PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        var regeneratedTicket = toTicketEntry(expandedTicket);

        assertThat(regeneratedTicket, is(equalTo(ticket)));
        assertThat(ticket, doesNotHaveEmptyValuesIgnoringFields(Set.of(WORKFLOW, ASSIGNEE, FINALIZED_BY,
                                                                       FINALIZED_DATE)));
        var expectedPublicationId = constructExpectedPublicationId(publication);
        assertThat(expandedTicket.getPublication().getPublicationId(), is(equalTo(expectedPublicationId)));
    }

    @DisplayName("should update associated Ticket when a Message is created")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldExpandAssociatedTicketWhenMessageIsCreated(Class<? extends TicketEntry> ticketType,
                                                          PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        var messages = expandedTicket.getMessages();
        var expectedExpandedMessage = messageToExpandedMessage(message);
        assertThat(messages, contains(expectedExpandedMessage));
    }

    @ParameterizedTest(name = "should return framed index document for resources. Instance type:{0}")
    @MethodSource("listPublicationInstanceTypes")
    void shouldReturnFramedIndexDocumentFromResource(Class<?> instanceType)
        throws JsonProcessingException, NotFoundException {

        Publication publication = PublicationGenerator.randomPublication(instanceType)
                                      .copy().withEntityDescription(new EntityDescription()).build();

        Resource resourceUpdate = Resource.fromPublication(publication);
        ExpandedResource indexDoc = (ExpandedResource) expansionService.expandEntry(resourceUpdate);
        assertThat(indexDoc.fetchId(), is(not(nullValue())));
    }

    @Test
    void shouldIncludedOnlyMessagesAssociatedToExpandedTicket() throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var owner = UserInstance.fromPublication(publication);

        var ticketToBeExpanded = TicketEntry
                                     .requestNewTicket(publication, GeneralSupportRequest.class)
                                     .persistNewTicket(ticketService);

        var message = messageService.createMessage(ticketToBeExpanded, owner, randomString());
        var expectedExpandedMessage = messageToExpandedMessage(message);
        var unexpectedMessages = messagesOfDifferentTickets(publication, owner, GeneralSupportRequest.class);
        var expandedEntry = (ExpandedTicket) expansionService.expandEntry(ticketToBeExpanded);
        assertThat(expandedEntry.getMessages(), hasItem(expectedExpandedMessage));
        assertThat(unexpectedMessages, everyItem(not(in(expandedEntry.getMessages()))));
    }

    @Test
    void shouldExpandAssociatedTicketAndNotTheMessageItselfWhenNewMessageArrivesForExpansion()
        throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var owner = UserInstance.fromPublication(publication);

        var ticketToBeExpanded = TicketEntry
                                     .requestNewTicket(publication, GeneralSupportRequest.class)
                                     .persistNewTicket(ticketService);

        var messageThatWillLeadToTicketExpansion =
            messageService.createMessage(ticketToBeExpanded, owner, randomString());
        var expectedExpandedMessage = messageToExpandedMessage(messageThatWillLeadToTicketExpansion);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(messageThatWillLeadToTicketExpansion);
        assertThat(expandedTicket.getMessages(), hasItem(expectedExpandedMessage));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldAddResourceTitleToExpandedTicket(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);

        var expectedTitle = publication.getEntityDescription().getMainTitle();
        assertThat(expandedTicket.getPublication().getTitle(), is(equalTo(expectedTitle)));
    }

    @Test
    void shouldThrowIfUnsupportedType() {
        var unsupportedImplementation = mock(Entity.class);
        assertThrows(UnsupportedOperationException.class,
                     () -> expansionService.expandEntry(unsupportedImplementation));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldGetAllOrganizationIdsForAffiliations(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

        var expectedOrgIds = Set.of(publication.getResourceOwner().getOwnerAffiliation());
        var orgIds = expansionService.getOrganizationIds(ticket);

        assertThat(orgIds, is(equalTo(expectedOrgIds)));
    }

    @Test
    void shouldReturnEmptySetIfNotTicketEntry() throws NotFoundException {
        var message = Message.builder()
                          .withResourceIdentifier(SortableIdentifier.next())
                          .withTicketIdentifier(SortableIdentifier.next())
                          .withIdentifier(SortableIdentifier.next())
                          .build();

        var actual = expansionService.getOrganizationIds(message);

        assertThat(actual, is(equalTo(Collections.emptySet())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldExpandTicketOwner(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, USER, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);

        expansionService = mockedExpansionService();

        var ticketOwner = ticket.getOwner();
        var expandedOwner = expansionService.expandPerson(ticketOwner);
        var expectedExpandedOwner = getExpectedExpandedPerson(ticketOwner);
        assertThat(expandedOwner, is(equalTo(expectedExpandedOwner)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldExpandAssigneeWhenPresent(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, USER, resourceService);
        var ticket = ticketWithAssignee(ticketType, publication);

        expansionService = mockedExpansionService();

        var assignee = ticket.getAssignee().getValue();
        var expectedExpandedAssignee = getExpectedExpandedPerson(new User(assignee));
        var expandedAssignee = expansionService.expandPerson(new User(assignee));
        assertThat(expandedAssignee, is(equalTo(expectedExpandedAssignee)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldCopyAllPubliclyVisibleFieldsFromMessageToExpandedMessage(Class<? extends TicketEntry> ticketType,
                                                                        PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        var expandedMessage = expansionService.expandMessage(message);
        var regeneratedMessage = expandedMessage.toMessage();
        assertThat(regeneratedMessage, is(equalTo(message)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Resource", "TicketEntry", "Message"})
    void shouldLogTypeAndIdentifierWhenEntityIsExpanded(String type)
        throws ApiGatewayException, JsonProcessingException {
        final var logAppender = LogUtils.getTestingAppender(ResourceExpansionServiceImpl.class);

        var entity = findEntity(type);
        expansionService.expandEntry(entity);

        assertThat(logAppender.getMessages(), containsString(type + ": " + entity.getIdentifier().toString()));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusNewWhenTicketStatusIsPendingWithoutAssignee(
        Class<? extends TicketEntry> ticketType,
        PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.NEW)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusClosedWhenTicketStatusIsClosed(
        Class<? extends TicketEntry> ticketType,
        PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setStatus(TicketStatus.CLOSED);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.CLOSED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldUpdateExpandedTicketStatusClosedWhenTicketStatusIsCompleted(
        Class<? extends TicketEntry> ticketType,
        PublicationStatus status)
        throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setStatus(TicketStatus.COMPLETED);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(expandedTicket.getStatus(), is(equalTo(ExpandedTicketStatus.COMPLETED)));
    }

    private static URI constructExpectedPublicationId(Publication publication) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild("publication")
                   .addChild(publication.getIdentifier().toString())
                   .getUri();
    }

    private static List<Class<?>> listPublicationInstanceTypes() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends TicketEntry> someOtherTicketTypeBesidesDoiRequest(
        Class<? extends TicketEntry> ticketType) {
        return (Class<? extends TicketEntry>)
                   ticketTypeProvider().filter(type -> !ticketType.equals(type) && !type.equals(DoiRequest.class))
                       .findAny().orElseThrow();
    }

    private static ExpandedPerson getExpectedExpandedPerson(User user) {
        return new ExpandedPerson(
            "someFirstName",
            "somePreferredFirstName",
            "someLastName",
            "somePreferredLastName",
            user);
    }

    private static TicketStatus getTicketStatus(ExpandedTicketStatus expandedTicketStatus) {
        return ExpandedTicketStatus.NEW.equals(expandedTicketStatus) ? TicketStatus.PENDING
                   : TicketStatus.parse(expandedTicketStatus.toString());
    }

    private ExpandedMessage messageToExpandedMessage(Message message) {
        return ExpandedMessage.builder()
                   .withCreatedDate(message.getCreatedDate())
                   .withModifiedDate(message.getModifiedDate())
                   .withOwner(message.getOwner())
                   .withResourceTitle(message.getResourceTitle())
                   .withCustomerId(message.getCustomerId())
                   .withSender(ExpandedPerson.defaultExpandedPerson(message.getSender()))
                   .withText(message.getText())
                   .withTicketIdentifier(message.getTicketIdentifier())
                   .withResourceIdentifier(message.getResourceIdentifier())
                   .withIdentifier(message.getIdentifier())
                   .build();
    }

    private Entity findEntity(String type) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(PUBLISHED, USER, resourceService);
        publication.setEntityDescription(new EntityDescription());
        var ticket = TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);

        switch (type) {
            case "Resource":
                return Resource.fromPublication(publication);
            case "TicketEntry":
                return ticket;
            case "Message":
                return messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
            default:
                throw new IllegalArgumentException("Unknown Entity type");
        }
    }

    private TicketEntry ticketWithAssignee(Class<? extends TicketEntry> ticketType, Publication publication)
        throws ApiGatewayException {
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticketService.updateTicketAssignee(ticket, new Username(USER.getUsername()));
        return ticketService.fetchTicket(ticket);
    }

    private ResourceExpansionService mockedExpansionService() {
        uriRetriever = mock(UriRetriever.class);
        when(uriRetriever.getRawContent(any(), any()))
            .thenReturn(Optional.of(IoUtils.stringFromResources(Path.of("cristin_person.json"))));
        return new ResourceExpansionServiceImpl(resourceService, ticketService, uriRetriever);
    }

    @SuppressWarnings("SameParameterValue")
    //Currently only GeneralSupportCase supports multiple simultaneous entries.
    // This may change in the future, so the warning is suppressed.
    private List<ExpandedMessage> messagesOfDifferentTickets(Publication publication, UserInstance owner,
                                                             Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {

        var differentTicketSameType = TicketEntry.requestNewTicket(publication, ticketType)
                                          .persistNewTicket(ticketService);
        var firstUnexpectedMessage = ExpandedMessage
                                         .createEntry(messageService.createMessage(
                                             differentTicketSameType, owner, randomString()), expansionService);
        var differentTicketType = someOtherTicketTypeBesidesDoiRequest(ticketType);
        var differentTicketDifferentType =
            TicketEntry.requestNewTicket(publication, differentTicketType).persistNewTicket(ticketService);
        var secondUnexpectedMessage = ExpandedMessage.createEntry(
            messageService.createMessage(differentTicketDifferentType, owner, randomString()), expansionService);
        return new ArrayList<>(List.of(firstUnexpectedMessage, secondUnexpectedMessage));
    }

    private void initializeServices() {
        resourceService = new ResourceService(client, CLOCK);
        messageService = new MessageService(client);
        ticketService = new TicketService(client);
        uriRetriever = new UriRetriever();
        expansionService = new ResourceExpansionServiceImpl(resourceService, ticketService, uriRetriever);
    }

    private Publication persistDraftPublicationWithoutDoi() throws BadRequestException {
        var publication =
            randomPublication().copy()
                .withDoi(null)
                .withStatus(DRAFT)
                .build();
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }

    private DoiRequest toTicketEntry(ExpandedDoiRequest expandedDoiRequest) {
        DoiRequest doiRequest = new DoiRequest();
        doiRequest.setCreatedDate(expandedDoiRequest.getCreatedDate());
        doiRequest.setIdentifier(expandedDoiRequest.identifyExpandedEntry());
        doiRequest.setCustomerId(expandedDoiRequest.getCustomerId());
        doiRequest.setModifiedDate(expandedDoiRequest.getModifiedDate());
        doiRequest.setOwner(expandedDoiRequest.getOwner().getUsername());
        doiRequest.setResourceIdentifier(expandedDoiRequest.getPublication().getIdentifier());
        doiRequest.setResourceStatus(expandedDoiRequest.getPublication().getStatus());
        doiRequest.setStatus(getTicketStatus(expandedDoiRequest.getStatus()));
        doiRequest.setAssignee(extractAssigneeUsername(expandedDoiRequest));
        return doiRequest;
    }

    private GeneralSupportRequest toTicketEntry(ExpandedGeneralSupportRequest expandedGeneralSupportRequest) {
        var ticketEntry = new GeneralSupportRequest();
        ticketEntry.setModifiedDate(expandedGeneralSupportRequest.getModifiedDate());
        ticketEntry.setCreatedDate(expandedGeneralSupportRequest.getCreatedDate());
        ticketEntry.setCustomerId(expandedGeneralSupportRequest.getCustomerId());
        ticketEntry.setIdentifier(extractIdentifier(expandedGeneralSupportRequest.getId()));
        ticketEntry.setResourceIdentifier(expandedGeneralSupportRequest.getPublication().getIdentifier());
        ticketEntry.setStatus(getTicketStatus(expandedGeneralSupportRequest.getStatus()));
        ticketEntry.setOwner(expandedGeneralSupportRequest.getOwner().getUsername());
        ticketEntry.setAssignee(extractAssigneeUsername(expandedGeneralSupportRequest));
        return ticketEntry;
    }

    private TicketEntry toTicketEntry(ExpandedPublishingRequest expandedPublishingRequest) {
        var publishingRequest = new PublishingRequestCase();
        publishingRequest.setResourceIdentifier(expandedPublishingRequest.getPublication().getIdentifier());
        publishingRequest.setCustomerId(expandedPublishingRequest.getCustomerId());
        publishingRequest.setIdentifier(extractIdentifier(expandedPublishingRequest.getId()));
        publishingRequest.setOwner(expandedPublishingRequest.getOwner().getUsername());
        publishingRequest.setModifiedDate(expandedPublishingRequest.getModifiedDate());
        publishingRequest.setCreatedDate(expandedPublishingRequest.getCreatedDate());
        publishingRequest.setStatus(getTicketStatus(expandedPublishingRequest.getStatus()));
        publishingRequest.setFinalizedBy(expandedPublishingRequest.getFinalizedBy());
        publishingRequest.setAssignee(extractAssigneeUsername(expandedPublishingRequest));
        return publishingRequest;
    }

    private TicketEntry toTicketEntry(ExpandedTicket expandedTicket) {
        if (expandedTicket instanceof ExpandedDoiRequest) {
            return toTicketEntry((ExpandedDoiRequest) expandedTicket);
        }
        if (expandedTicket instanceof ExpandedPublishingRequest) {
            return toTicketEntry((ExpandedPublishingRequest) expandedTicket);
        }
        if (expandedTicket instanceof ExpandedGeneralSupportRequest) {
            return toTicketEntry((ExpandedGeneralSupportRequest) expandedTicket);
        }
        return null;
    }

    private Username extractAssigneeUsername(ExpandedTicket expandedTicket) {
        return Optional.ofNullable(expandedTicket.getAssignee())
                   .map(ExpandedPerson::getUsername)
                   .map(User::toString)
                   .map(Username::new)
                   .orElse(null);
    }
}

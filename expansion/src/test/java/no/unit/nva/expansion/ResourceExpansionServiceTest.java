package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.*;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ResourceExpansionServiceTest extends ResourcesLocalTest {

    private static final Clock CLOCK = Clock.systemDefaultZone();
    private static final String FINALIZED_DATE = "finalizedDate";
    private static final String WORKFLOW = "workflow";
    private static final String ASSIGNEE = "assignee";
    private static final String FINALIZED_BY = "finalizedBy";

    private ResourceExpansionService expansionService;
    private ResourceService resourceService;
    private MessageService messageService;
    private TicketService ticketService;

    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
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
        var regeneratedTicket = expandedTicket.toTicketEntry();

        assertThat(regeneratedTicket.getIdentifier(), is(equalTo(ticket.getIdentifier())));
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
        assertThat(messages, contains(message));
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

        var expectedMessage = messageService.createMessage(ticketToBeExpanded, owner, randomString());

        var unexpectedMessages = messagesOfDifferentTickets(publication, owner, GeneralSupportRequest.class);
        var expandedEntry = (ExpandedTicket) expansionService.expandEntry(ticketToBeExpanded);
        assertThat(expandedEntry.getMessages(), contains(expectedMessage));
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

        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(messageThatWillLeadToTicketExpansion);
        assertThat(expandedTicket.getMessages(), contains(messageThatWillLeadToTicketExpansion));
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

    @DisplayName("should not update ExpandedTicket status when ticket status is not pending")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotUpdateExpandedTicketStatusWhenTicketStatusIsNotPending(Class<? extends TicketEntry> ticketType,
                                                                      PublicationStatus status)
            throws ApiGatewayException, JsonProcessingException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.setStatus(TicketStatus.COMPLETED);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        var regeneratedTicket = expandedTicket.toTicketEntry();

        assertThat(ticket, doesNotHaveEmptyValuesIgnoringFields(Set.of(WORKFLOW, ASSIGNEE, FINALIZED_BY,
                FINALIZED_DATE)));
        assertThat(regeneratedTicket.getStatus(), is(equalTo(ticket.getStatus())));
    }

    @SuppressWarnings("SameParameterValue")
    //Currently only GeneralSupportCase supports multiple simultaneous entries.
    // This may change in the future, so the warning is suppressed.
    private List<Message> messagesOfDifferentTickets(Publication publication, UserInstance owner,
                                                     Class<? extends TicketEntry> ticketType)
            throws ApiGatewayException {

        var differentTicketSameType = TicketEntry.requestNewTicket(publication, ticketType)
                .persistNewTicket(ticketService);
        var firstUnexpectedMessage = messageService.createMessage(differentTicketSameType, owner, randomString());
        var differentTicketType = someOtherTicketTypeBesidesDoiRequest(ticketType);
        var differentTicketDifferentType =
                TicketEntry.requestNewTicket(publication, differentTicketType).persistNewTicket(ticketService);
        var secondUnexpectedMessage = messageService.createMessage(differentTicketDifferentType, owner, randomString());
        return new ArrayList<>(List.of(firstUnexpectedMessage, secondUnexpectedMessage));
    }

    private void initializeServices() {
        resourceService = new ResourceService(client, CLOCK);
        messageService = new MessageService(client);
        ticketService = new TicketService(client);
        UriRetriever uriRetriever = mock(UriRetriever.class);
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
}

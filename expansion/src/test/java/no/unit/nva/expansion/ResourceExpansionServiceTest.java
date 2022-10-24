package no.unit.nva.expansion;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceExpansionServiceTest extends ResourcesLocalTest {
    
    public static final Clock CLOCK = Clock.systemDefaultZone();
    
    private ResourceExpansionService expansionService;
    private ResourceService resourceService;
    private MessageService messageService;
    private TicketService ticketService;
    
    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    @BeforeEach
    void setUp() {
        super.init();
        initializeServices();
    }
    
    @Test
    void shouldReturnExpandedTicketContainingTheOrganizationIdOfTheOwnersAffiliationAsIs()
        throws Exception {
        var publication = persistDraftPublicationWithoutDoi();
        var ticket = createTicket(publication, DoiRequest.class);
        var userAffiliation = publication.getResourceOwner().getOwnerAffiliation();
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        assertThat(userAffiliation, is(in(expandedTicket.getOrganizationIds())));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should copy all publicly visible fields from Ticket to ExpandedTicket")
    @MethodSource("ticketTypeProvider")
    void shouldCopyAllPubliclyVisibleFieldsFromTicketToExpandedTicket(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var ticket = TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        var regeneratedTicket = expandedTicket.toTicketEntry();
        
        assertThat(ticket, doesNotHaveEmptyValues());
        assertThat(regeneratedTicket, is(equalTo(ticket)));
        var expectedPublicationId = constructExpectedPublicationId(publication);
        assertThat(expandedTicket.getPublication().getPublicationId(), is(equalTo(expectedPublicationId)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should update associated Ticket when a Message is created")
    @MethodSource("ticketTypeProvider")
    void shouldExpandAssociatedTicketWhenMessageIsCreated(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var ticket = TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
        
        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        var messages = expandedTicket.getMessages();
        assertThat(messages, contains(message));
    }
    
    @ParameterizedTest(name = "should return framed index document for resources. Instance type:{0}")
    @MethodSource("listPublicationInstanceTypes")
    void shouldReturnFramedIndexDocumentFromResource(Class<?> instanceType)
        throws JsonProcessingException, NotFoundException {
        Publication publication = PublicationGenerator.randomPublication(instanceType);
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
    
    @ParameterizedTest(name = "should add resource title to expanded ticket:{0}")
    @MethodSource("ticketTypeProvider")
    void shouldAddResourceTitleToExpandedTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException,
                                                                                                JsonProcessingException {
        var publication = persistDraftPublicationWithoutDoi();
        var ticket = TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
        var expandedTicket = (ExpandedTicket) expansionService.expandEntry(ticket);
        
        var expectedTitle = publication.getEntityDescription().getMainTitle();
        assertThat(expandedTicket.getPublication().getTitle(), is(equalTo(expectedTitle)));
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
    private static Class<? extends TicketEntry> someOtherTicketType(Class<? extends TicketEntry> ticketType) {
        return (Class<? extends TicketEntry>) ticketTypeProvider().filter(type -> !ticketType.equals(type))
                                                  .findAny().orElseThrow();
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
        var differentTicketType = someOtherTicketType(ticketType);
        var differentTicketDifferentType =
            TicketEntry.requestNewTicket(publication, differentTicketType).persistNewTicket(ticketService);
        var secondUnexpectedMessage = messageService.createMessage(differentTicketDifferentType, owner, randomString());
        return new ArrayList<>(List.of(firstUnexpectedMessage, secondUnexpectedMessage));
    }
    
    private TicketEntry createTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
    }
    
    private void initializeServices() {
        resourceService = new ResourceService(client, CLOCK);
        messageService = new MessageService(client);
        ticketService = new TicketService(client);
        expansionService = new ResourceExpansionServiceImpl(resourceService, ticketService);
    }
    
    private Publication persistDraftPublicationWithoutDoi() {
        var publication =
            randomPreFilledPublicationBuilder()
                .withDoi(null)
                .withStatus(DRAFT)
                .build();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}

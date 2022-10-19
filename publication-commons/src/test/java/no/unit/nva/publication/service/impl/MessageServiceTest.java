package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.TestingUtils.createUnpersistedPublication;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.publication.model.business.TicketEntry.SUPPORT_SERVICE_CORRESPONDENT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MessageServiceTest extends ResourcesLocalTest {
    
    public static final String SOME_SENDER = "some@user";
    public static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Instant MESSAGE_CREATION_TIME = PUBLICATION_CREATION_TIME.plus(Period.ofDays(2));
    public static final Instant SECOND_MESSAGE_CREATION_TIME = MESSAGE_CREATION_TIME.plus(Period.ofDays(2));
    public static final Instant THIRD_MESSAGE_CREATION_TIME = SECOND_MESSAGE_CREATION_TIME.plus(Period.ofDays(2));
    private MessageService messageService;
    private ResourceService resourceService;
    private UserInstance owner;
    private TicketService ticketService;
    
    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    @BeforeEach
    public void initialize() {
        super.init();
        var clock = mockClock();
        messageService = new MessageService(client);
        resourceService = new ResourceService(client, clock);
        ticketService = new TicketService(client);
        owner = randomUserInstance();
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should persist message with reference to a ticket")
    @MethodSource("ticketTypeProvider")
    void shouldPersistMessageWithReferenceToATicket(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var ticket = createTicket(publication, ticketType);
        var message = publicationOwnerSendsMessage(ticket, randomString());
        var persistedMessage = messageService.getMessageByIdentifier(message.getIdentifier()).orElseThrow();
        assertThat(persistedMessage.getTicketIdentifier(), is(equalTo(ticket.getIdentifier())));
        assertThat(persistedMessage.getText(), is(equalTo(message.getText())));
    }
    
    @Test
    void shouldSetRecipientAsOwnerWhenSenderIsNotOwner() throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var ticket = TicketEntry
                         .requestNewTicket(publication, DoiRequest.class)
                         .persistNewTicket(ticketService);
        var sender = UserInstance.create(SOME_SENDER, publication.getPublisher().getId());
        var message =
            messageService.createMessage(ticket, sender, randomString());
        var retrievedMessage = messageService.getMessageByIdentifier(message.getIdentifier())
                                   .orElseThrow();
        assertThat(retrievedMessage.getRecipient(), is(equalTo(new User(publication.getResourceOwner().getOwner()))));
    }
    
    //TODO: discuss with product owner what the actual requirements are here.
    @Test
    void shouldSetRecipientAsSupportServiceWhenSenderIsOwner() throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var ticket = TicketEntry.requestNewTicket(publication, DoiRequest.class)
                         .persistNewTicket(ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());
        var retrievedMessage = messageService.getMessage(owner, persistedMessage.getIdentifier());
        assertThat(retrievedMessage.getRecipient(), is(equalTo(SUPPORT_SERVICE_CORRESPONDENT)));
    }
    
    private Message publicationOwnerSendsMessage(TicketEntry ticket, String messageText) {
        var userInfo = UserInstance.fromTicket(ticket);
        return messageService.createMessage(ticket, userInfo, messageText);
    }
    
    private TicketEntry createTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
    }
    
    private Publication createDraftPublication(UserInstance owner) {
        var publication = createUnpersistedPublication(owner);
        return resourceService.createPublication(owner, publication);
    }
    
    private Clock mockClock() {
        var clock = mock(Clock.class);
        when(clock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(MESSAGE_CREATION_TIME)
            .thenReturn(SECOND_MESSAGE_CREATION_TIME)
            .thenReturn(THIRD_MESSAGE_CREATION_TIME);
        return clock;
    }
}
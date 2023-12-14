package no.unit.nva.publication.service.impl;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.TestingUtils;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MessageServiceTest extends ResourcesLocalTest {
    
    public static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Instant MESSAGE_CREATION_TIME = PUBLICATION_CREATION_TIME.plus(Period.ofDays(2));
    public static final Instant SECOND_MESSAGE_CREATION_TIME = MESSAGE_CREATION_TIME.plus(Period.ofDays(2));
    public static final Instant THIRD_MESSAGE_CREATION_TIME = SECOND_MESSAGE_CREATION_TIME.plus(Period.ofDays(2));
    private MessageService messageService;
    private ResourceService resourceService;
    private UserInstance owner;
    private TicketService ticketService;

    @BeforeEach
    public void initialize() {
        super.init();
        var clock = mockClock();
        messageService = new MessageService(client);
        resourceService = new ResourceService(client, clock);
        ticketService = new TicketService(client);
        owner = TestingUtils.randomUserInstance();
    }


    @DisplayName("should persist message with reference to a ticket")
    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldPersistMessageWithReferenceToATicket(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var message = publicationOwnerSendsMessage(ticket, randomString());
        var persistedMessage = messageService.getMessageByIdentifier(message.getIdentifier()).orElseThrow();
        assertThat(persistedMessage.getTicketIdentifier(), is(equalTo(ticket.getIdentifier())));
        assertThat(persistedMessage.getText(), is(equalTo(message.getText())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForEveryoneExceptSenderWhenMessageIsCreated(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());
        var retrievedMessage = messageService.getMessage(owner, persistedMessage.getIdentifier());
        assertThat(retrievedMessage.getSender(), is(equalTo(owner.getUser())));
    }

    @Test
    void shouldDeleteMessageForMessageOwner() throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());
        messageService.deleteMessage(UserInstance.fromMessage(persistedMessage), persistedMessage);
        var deletedMessage = messageService.getMessage(owner, persistedMessage.getIdentifier());

        assertThat(deletedMessage.getStatus(), is(equalTo(MessageStatus.DELETED)));
    }

    @Test
    void shouldThrowUnauthorizedWhenAttemptingToDeleteMessageUserDoesNotOwn()
        throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(
            PublicationStatus.PUBLISHED, owner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
        var persistedMessage = messageService.createMessage(ticket, owner, randomString());

        assertThrows(UnauthorizedException.class,
                     () -> messageService.deleteMessage(randomUserInstance(), persistedMessage));
    }

    private static UserInstance randomUserInstance() {
        return UserInstance.create(new User(randomString()), randomUri());
    }

    private Message publicationOwnerSendsMessage(TicketEntry ticket, String messageText) {
        var userInfo = UserInstance.fromTicket(ticket);
        return messageService.createMessage(ticket, userInfo, messageText);
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
package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.TestingUtils.createUnpersistedPublication;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.function.Supplier;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class MessageServiceTest extends ResourcesLocalTest {
    
    public static final String SOME_SENDER = "some@user";
    public static final SortableIdentifier SOME_IDENTIFIER = SortableIdentifier.next();
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
        messageService = new MessageService(client, clock);
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
    
    //not relevant
    @ParameterizedTest(name = "should persist message of type {0}")
    @EnumSource(MessageType.class)
    void shouldPersistMessageOfType(MessageType messageType) throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var messageText = randomString();
        var sender = UserInstance.create(SOME_SENDER, publication.getPublisher().getId());
        var messageIdentifier =
            messageService.createMessage(sender, publication, messageText, messageType);
        var message = messageService.getMessage(owner, messageIdentifier);
        var expectedMessage = constructExpectedMessage(messageIdentifier, publication, messageText, messageType);
        assertThat(expectedMessage, is(equalTo(message)));
    }
    
    @ParameterizedTest(name = "should throw Exception when type is {0} and identifier is duplicate")
    @EnumSource(MessageType.class)
    void shouldThrowExceptionWhenDuplicateIdentifierIsInserted(MessageType messageType) {
        messageService = serviceProducingDuplicateIdentifiers();
        var publication = createDraftPublication(owner);
        
        var actualIdentifier = createSimpleMessage(publication, randomString(), messageType);
        
        assertThat(actualIdentifier, is(equalTo(SOME_IDENTIFIER)));
        
        Executable action = () -> createSimpleMessage(publication, randomString(), messageType);
        assertThrows(TransactionFailedException.class, action);
    }
    
    @Test
    void shouldSetRecipientAsOwnerWhenSenderIsNotOwner() throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var sender = UserInstance.create(SOME_SENDER, publication.getPublisher().getId());
        var messageIdentifier =
            messageService.createMessage(sender, publication, randomString(), randomElement(MessageType.values()));
        var message = messageService.getMessage(owner, messageIdentifier);
        assertThat(message.getRecipient(), is(equalTo(publication.getResourceOwner().getOwner())));
    }
    
    //TODO: discuss with product owner what the actual requirements are here.
    @Test
    void shouldSetRecipientAsSupportServiceWhenSenderIsOwner() throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var messageIdentifier =
            messageService.createMessage(owner, publication, randomString(), randomElement(MessageType.values()));
        var message = messageService.getMessage(owner, messageIdentifier);
        assertThat(message.getRecipient(), is(equalTo(Message.SUPPORT_SERVICE_CORRESPONDENT)));
    }
    
    @Test
    void shouldBeAbleToMarkMessageAnReadWhenInputMessageExists() throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var messageIdentifier =
            messageService.createMessage(owner, publication, randomString(), randomElement(MessageType.values()));
        var originalMessage = messageService.getMessage(owner, messageIdentifier);
        messageService.markAsRead(originalMessage);
        var updatedMessage = messageService.getMessage(owner, messageIdentifier);
        assertThat(originalMessage.getStatus(), is(equalTo(MessageStatus.UNREAD)));
        assertThat(updatedMessage.getStatus(), is(equalTo(MessageStatus.READ)));
    }
    
    @Test
    void shouldThrowExceptionWhenTryingToMarkNonExistentMessageAsRead() {
        var publication = createDraftPublication(owner);
        var nonPersistedMessage = Message.create(owner, publication, randomString(), SortableIdentifier.next(),
            Clock.systemDefaultZone(), MessageType.SUPPORT);
        
        assertThrows(NotFoundException.class, () -> messageService.markAsRead(nonPersistedMessage));
    }
    
    private Message publicationOwnerSendsMessage(TicketEntry ticket, String messageText) {
        var userInfo = UserInstance.fromTicket(ticket);
        return messageService.createMessage(ticket, userInfo, messageText);
    }
    
    private TicketEntry createTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        return ticketService.createTicket(ticket, ticketType);
    }
    
    
    
    private MessageService serviceProducingDuplicateIdentifiers() {
        return new MessageService(client, mockClock(), duplicateIdentifierSupplier());
    }
    
    
    
    
    private Supplier<SortableIdentifier> duplicateIdentifierSupplier() {
        return () -> SOME_IDENTIFIER;
    }
    
    private Publication createDraftPublication(UserInstance owner) {
        var publication = createUnpersistedPublication(owner);
        return resourceService.createPublication(owner, publication);
    }
    
    
    
    private SortableIdentifier createSimpleMessage(Publication publication, String message, MessageType messageType) {
        var publicationOwner = UserInstance.fromPublication(publication);
        var sender = UserInstance.create(SOME_SENDER, publicationOwner.getOrganizationUri());
        return attempt(() -> messageService.createMessage(sender, publication, message, messageType)).orElseThrow();
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
    
    private Message constructExpectedMessage(SortableIdentifier messageIdentifier,
                                             Publication publication,
                                             String messageText,
                                             MessageType messageType) {
        var sender = UserInstance.create(SOME_SENDER, publication.getPublisher().getId());
        var clock = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
        return Message.create(sender, publication, messageText, messageIdentifier, clock, messageType);
    }
}
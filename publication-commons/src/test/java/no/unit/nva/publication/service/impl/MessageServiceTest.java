package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.TestingUtils.createUnpersistedPublication;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
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
    public static final int NUMBER_OF_SAMPLE_MESSAGES = 3;
    
    public static final String SOME_OTHER_OWNER = "someOther@owner";
    public static final URI SOME_OTHER_ORG = URI.create("https://some.other.example.org/98765");
    public static final ResourceOwner RANDOM_RESOURCE_OWNER = new ResourceOwner(SOME_OTHER_OWNER, SOME_OTHER_ORG);
    
    public static final int FIRST_ELEMENT = 0;
    private static final int SINGLE_EXPECTED_ELEMENT = 0;
    
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
    
    //not relevant
    @Test
    void getMessagesByResourceIdentifierReturnsAllMessagesRelatedToResource() {
        var insertedPublication = createDraftPublication(owner);
        var insertedMessages = insertSampleMessages(insertedPublication);
        
        var resourceConversationOpt = messageService.getMessagesForResource(owner, insertedPublication.getIdentifier());
        
        assertThat(resourceConversationOpt.isPresent(), is(true));
        var resourceConversation = resourceConversationOpt.orElseThrow();
        var actualPublication = resourceConversation.getPublicationSummary();
        var expectedPublication = PublicationSummary.create(constructExpectedPublication(insertedPublication));
        
        assertThat(actualPublication, is(equalTo(expectedPublication)));
        
        var expectedMessages = toArray(insertedMessages);
        assertThat(resourceConversation.allMessages(), containsInAnyOrder(expectedMessages));
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
    
    //not relevant
    @Test
    void listMessagesForCustomerAndStatusListsAllMessagesForGivenCustomerAndStatus() throws NotFoundException {
        var createdPublications = createPublicationsOfDifferentOwnersInSameOrg();
        var savedMessages = createOneMessagePerPublication(createdPublications);
        
        var publisherId = createdPublications.get(FIRST_ELEMENT).getPublisher().getId();
        var actualConversation = messageService.listMessagesForCurator(publisherId, TicketStatus.UNREAD);
        
        var expectedConversation = constructExpectedCuratorsMessageView(publisherId, savedMessages);
        assertThat(actualConversation, contains(expectedConversation));
    }
    
    //not relevant
    @Test
    void listMessagesForCustomerAndStatusReturnsMessagesOfSingleCustomer() throws NotFoundException {
        var createdPublications = createPublicationsOfDifferentOwnersInDifferentOrg();
        var allMessagesOfAllCustomers = createOneMessagePerPublication(createdPublications);
        
        var customerId = createdPublications.get(FIRST_ELEMENT).getPublisher().getId();
        var actualConversations = messageService.listMessagesForCurator(customerId, TicketStatus.UNREAD);
        
        var expectedConversations = constructExpectedCuratorsMessageView(customerId, allMessagesOfAllCustomers);
        
        assertThat(actualConversations, contains(expectedConversations));
    }
    
    //TODO: there should be a listing of tickets instead of messages.
    @Test
    void listMessagesForUserReturnsAllMessagesConnectedToUser() {
        var publication1 = createDraftPublication(owner);
        var publication2 = createDraftPublication(owner);
        
        var messagesForPublication1 = insertSampleMessages(publication1);
        var messagesForPublication2 = insertSampleMessages(publication2);
        
        var actualMessages = messageService.listMessagesForUser(UserInstance.fromPublication(publication1));
        var expectedMessagesForPublication1 = constructExpectedMessages(messagesForPublication1);
        var expectedMessagesFromPublication2 = constructExpectedMessages(messagesForPublication2);
        var expectedMessages = List.of(
            expectedMessagesForPublication1,
            expectedMessagesFromPublication2
        );
        
        assertThat(actualMessages, is(equalTo(expectedMessages)));
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
        assertThat(message.getRecipient(), is(equalTo(Message.SUPPORT_SERVICE_RECIPIENT)));
    }
    
    @Test
    void shouldBeAbleToMarkMessageAnReadWhenInputMessageExists() throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var messageIdentifier =
            messageService.createMessage(owner, publication, randomString(), randomElement(MessageType.values()));
        var originalMessage = messageService.getMessage(owner, messageIdentifier);
        messageService.markAsRead(originalMessage);
        var updatedMessage = messageService.getMessage(owner, messageIdentifier);
        assertThat(originalMessage.getStatus(), is(equalTo(TicketStatus.UNREAD)));
        assertThat(updatedMessage.getStatus(), is(equalTo(TicketStatus.READ)));
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
    
    private ResourceConversation constructExpectedMessages(List<Message> messagesForPublication) {
        return ResourceConversation.fromMessageList(messagesForPublication).get(SINGLE_EXPECTED_ELEMENT);
    }
    
    private ResourceConversation[] constructExpectedCuratorsMessageView(URI customerId,
                                                                        List<Message> allMessagesOfAllOwnersAndCustomers) {
        var messagesOfSpecifiedCustomer = filterBasedOnCustomerId(customerId, allMessagesOfAllOwnersAndCustomers);
        var conversationList = ResourceConversation.fromMessageList(messagesOfSpecifiedCustomer);
        return conversationList.toArray(new ResourceConversation[0]);
    }
    
    private List<Message> filterBasedOnCustomerId(URI customerId, List<Message> allMessagesOfAllOwnersAndCustomers) {
        return allMessagesOfAllOwnersAndCustomers
                   .stream()
                   .filter(message -> message.getCustomerId().equals(customerId))
                   .collect(Collectors.toList());
    }
    
    private Message[] toArray(List<Message> insertedMessages) {
        return insertedMessages.toArray(Message[]::new);
    }
    
    private Publication constructExpectedPublication(Publication insertedPublication) {
        var entityDescription =
            new EntityDescription.Builder()
                .withMainTitle(insertedPublication.getEntityDescription().getMainTitle())
                .withContributors(Collections.emptyList())
                .build();
    
        return new Publication.Builder()
                   .withIdentifier(insertedPublication.getIdentifier())
                   .withResourceOwner(insertedPublication.getResourceOwner())
                   .withPublisher(insertedPublication.getPublisher())
                   .withEntityDescription(entityDescription)
                   .build();
    }
    
    private MessageService serviceProducingDuplicateIdentifiers() {
        return new MessageService(client, mockClock(), duplicateIdentifierSupplier());
    }
    
    private List<Publication> createPublicationsOfDifferentOwnersInDifferentOrg() {
        var publicationOfSomeOrg = PublicationGenerator.publicationWithoutIdentifier();
        var someOtherOrg = new Organization.Builder().withId(SOME_OTHER_ORG).build();
        var publicationOfDifferentOrg = publicationOfSomeOrg
                                            .copy()
                                            .withResourceOwner(RANDOM_RESOURCE_OWNER)
                                            .withPublisher(someOtherOrg)
                                            .build();
        var newPublications = List.of(publicationOfSomeOrg, publicationOfDifferentOrg);
        return persistPublications(newPublications);
    }
    
    private List<Message> createOneMessagePerPublication(List<Publication> createdPublications)
        throws NotFoundException {
        var savedMessages = new ArrayList<Message>();
        for (Publication createdPublication : createdPublications) {
            var messageIdentifier = createSimpleMessage(createdPublication, randomString(), randomMessageType());
            var owner = UserInstance.fromPublication(createdPublication);
            var savedMessage = fetchMessage(owner, messageIdentifier);
            savedMessages.add(savedMessage);
        }
        return savedMessages;
    }
    
    private List<Publication> createPublicationsOfDifferentOwnersInSameOrg() {
        var publicationOfSomeOwner = PublicationGenerator.publicationWithoutIdentifier();
        var publicationOfDifferentOwner = PublicationGenerator.publicationWithoutIdentifier()
                                              .copy().withResourceOwner(RANDOM_RESOURCE_OWNER).build();
        var newPublications = List.of(publicationOfSomeOwner, publicationOfDifferentOwner);
        return persistPublications(newPublications);
    }
    
    private List<Publication> persistPublications(List<Publication> newPublications) {
        return newPublications.stream()
                   .map(attempt(pub -> createPublication(resourceService, pub)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }
    
    private Supplier<SortableIdentifier> duplicateIdentifierSupplier() {
        return () -> SOME_IDENTIFIER;
    }
    
    private Publication createDraftPublication(UserInstance owner) {
        var publication = createUnpersistedPublication(owner);
        return resourceService.createPublication(owner, publication);
    }
    
    private Publication createPublication(ResourceService resourceService, Publication publication) {
        UserInstance userInstance = UserInstance.fromPublication(publication);
        return resourceService.createPublication(userInstance, publication);
    }
    
    private List<Message> insertSampleMessages(Publication publication) {
        var publicationOwner = UserInstance.fromPublication(publication);
        return IntStream.range(0, NUMBER_OF_SAMPLE_MESSAGES).boxed()
                   .map(ignoredValue -> randomString())
                   .map(message -> createSimpleMessage(publication, message, randomMessageType()))
                   .map(attempt(messageIdentifier -> fetchMessage(publicationOwner, messageIdentifier)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }
    
    private MessageType randomMessageType() {
        return randomElement(MessageType.values());
    }
    
    private Message fetchMessage(UserInstance publicationOwner, SortableIdentifier messageIdentifier)
        throws NotFoundException {
        return messageService.getMessage(publicationOwner, messageIdentifier);
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
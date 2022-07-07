package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.TestingUtils.createPublicationForUser;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.github.javafaker.Faker;
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
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class MessageServiceTest extends ResourcesLocalTest {

    public static final Faker FAKER = new Faker();
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

    public static final String SAMPLE_HOST = "https://localhost/messages/";

    public static final int FIRST_ELEMENT = 0;
    private static final int SINGLE_EXPECTED_ELEMENT = 0;

    private MessageService messageService;
    private ResourceService resourceService;
    private UserInstance owner;

    @BeforeEach
    public void initialize() {
        super.init();
        Clock clock = mockClock();
        messageService = new MessageService(client, clock);
        resourceService = new ResourceService(client, clock);
        owner = randomUserInstance();
    }

    @Test
    void createSimpleMessageStoresNewMessageInDatabase() throws ApiGatewayException {

        var publication = createDraftPublication(owner);
        var messageText = randomString();

        var messageIdentifier = createSimpleMessage(publication, messageText);
        var savedMessage = fetchMessage(owner, messageIdentifier);

        var expectedMessage =
            constructExpectedSimpleMessage(savedMessage.getIdentifier(), publication, messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    void createDoiRequestMessageStoresNewMessageInDatabaseIndicatingThatIsConnectedToTheRespectiveDoiRequest()
        throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var messageText = randomString();
        var messageIdentifier = createDoiRequestMessage(publication, messageText);
        var savedMessage = fetchMessage(owner, messageIdentifier);
        var expectedMessage = constructExpectedDoiRequestMessage(
            messageIdentifier,
            publication,
            messageText);

        assertThat(savedMessage.getMessageType(), is(MessageType.DOI_REQUEST));

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    void getMessagesByResourceIdentifierReturnsAllMessagesRelatedToResource()
        throws ApiGatewayException {
        var insertedPublication = createDraftPublication(owner);
        var insertedMessages = insertSampleMessages(insertedPublication);

        var resourceConversationOpt =
            messageService.getMessagesForResource(owner, insertedPublication.getIdentifier());

        assertThat(resourceConversationOpt.isPresent(), is(true));
        var resourceConversation = resourceConversationOpt.orElseThrow();
        var actualPublication = resourceConversation.getPublicationSummary();
        var expectedPublication = PublicationSummary.create(constructExpectedPublication(insertedPublication));

        assertThat(actualPublication, is(equalTo(expectedPublication)));

        MessageDto[] expectedMessages = constructExpectedMessagesDtos(insertedMessages);
        assertThat(resourceConversation.allMessages(), containsInAnyOrder(expectedMessages));
    }

    @Test
    void createSimpleMessageThrowsExceptionWhenDuplicateIdentifierIsInserted()
        throws ApiGatewayException {
        messageService = serviceProducingDuplicateIdentifiers();
        var publication = createDraftPublication(owner);

        var actualIdentifier = createSimpleMessage(publication, randomString());

        assertThat(actualIdentifier, is(equalTo(SOME_IDENTIFIER)));

        Executable action = () -> createSimpleMessage(publication, randomString());
        assertThrows(TransactionFailedException.class, action);
    }

    @Test
    void getMessageByOwnerAndIdReturnsStoredMessage() throws ApiGatewayException {
        Publication publication = createDraftPublication(owner);
        String messageText = randomString();
        var messageIdentifier = createSimpleMessage(publication, messageText);
        var savedMessage = fetchMessage(UserInstance.fromPublication(publication), messageIdentifier);
        var expectedMessage = constructExpectedSimpleMessage(savedMessage.getIdentifier(), publication,
                                                             messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    void getMessageByKeyReturnsStoredMessage() throws ApiGatewayException {
        var publication = createDraftPublication(owner);
        var messageText = randomString();
        var messageIdentifier = createSimpleMessage(publication, messageText);

        var savedMessage = messageService.getMessage(UserInstance.fromPublication(publication), messageIdentifier);
        var expectedMessage = constructExpectedSimpleMessage(savedMessage.getIdentifier(), publication,
                                                             messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    void getMessageByIdAndOwnerReturnsStoredMessage() throws ApiGatewayException {
        Publication publication = createDraftPublication(owner);
        String messageText = randomString();
        var messageIdentifier = createSimpleMessage(publication, messageText);
        var sampleMessageUri = URI.create(SAMPLE_HOST + messageIdentifier.toString());
        var savedMessage = fetchMessage(UserInstance.fromPublication(publication), sampleMessageUri);
        var expectedMessage = constructExpectedSimpleMessage(savedMessage.getIdentifier(), publication,
                                                             messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    void listMessagesForCustomerAndStatusListsAllMessagesForGivenCustomerAndStatus() throws NotFoundException {
        var createdPublications = createPublicationsOfDifferentOwnersInSameOrg();
        var savedMessages = createOneMessagePerPublication(createdPublications);

        var publisherId = createdPublications.get(FIRST_ELEMENT).getPublisher().getId();
        var actualConversation = messageService.listMessagesForCurator(publisherId, MessageStatus.UNREAD);

        var expectedConversation = constructExpectedCuratorsMessageView(publisherId, savedMessages);
        assertThat(actualConversation, contains(expectedConversation));
    }

    @Test
    void listMessagesForCustomerAndStatusReturnsMessagesOfSingleCustomer() throws NotFoundException {
        var createdPublications = createPublicationsOfDifferentOwnersInDifferentOrg();
        var allMessagesOfAllCustomers = createOneMessagePerPublication(createdPublications);

        var customerId = createdPublications.get(FIRST_ELEMENT).getPublisher().getId();
        var actualConversations = messageService.listMessagesForCurator(customerId, MessageStatus.UNREAD);

        var expectedConversations = constructExpectedCuratorsMessageView(customerId, allMessagesOfAllCustomers);

        assertThat(actualConversations, contains(expectedConversations));
    }

    @Test
    void listMessagesForUserReturnsAllMessagesConnectedToUser() throws ApiGatewayException {
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

    private ResourceConversation constructExpectedMessages(List<Message> messagesForPublication) {
        return ResourceConversation.fromMessageList(messagesForPublication).get(SINGLE_EXPECTED_ELEMENT);
    }

    private ResourceConversation[] constructExpectedCuratorsMessageView(
        URI customerId,
        List<Message> allMessagesOfAllOwnersAndCustomers) {
        var messagesOfSpecifiedCustomer =
            filterBasedOnCustomerId(customerId, allMessagesOfAllOwnersAndCustomers);
        var conversationList = ResourceConversation.fromMessageList(messagesOfSpecifiedCustomer);
        return conversationList.toArray(new ResourceConversation[0]);
    }

    private List<Message> filterBasedOnCustomerId(URI customerId, List<Message> allMessagesOfAllOwnersAndCustomers) {
        return allMessagesOfAllOwnersAndCustomers
            .stream()
            .filter(message -> message.getCustomerId().equals(customerId))
            .collect(Collectors.toList());
    }

    private MessageDto[] constructExpectedMessagesDtos(List<Message> insertedMessages) {
        return insertedMessages.stream()
            .map(MessageDto::fromMessage)
            .toArray(MessageDto[]::new);
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

    private String randomString() {
        return FAKER.lorem().sentence();
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
        List<Message> savedMessages = new ArrayList<>();

        for (Publication createdPublication : createdPublications) {
            SortableIdentifier messageIdentifier = createSimpleMessage(createdPublication, randomString());
            UserInstance owner = UserInstance.fromPublication(createdPublication);
            Message savedMessage = fetchMessage(owner, messageIdentifier);
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

    private Publication createDraftPublication(UserInstance owner) throws ApiGatewayException {
        var publication = createPublicationForUser(owner);
        return resourceService.createPublication(owner, publication);
    }

    private Publication createPublication(ResourceService resourceService, Publication publication)
        throws ApiGatewayException {
        UserInstance userInstance = UserInstance.fromPublication(publication);
        return resourceService.createPublication(userInstance, publication);
    }

    private List<Message> insertSampleMessages(Publication publication) {
        var publicationOwner = UserInstance.fromPublication(publication);
        return IntStream.range(0, NUMBER_OF_SAMPLE_MESSAGES).boxed()
            .map(ignoredValue -> randomString())
            .map(message -> createSimpleMessage(publication, message))
            .map(attempt(messageIdentifier -> fetchMessage(publicationOwner, messageIdentifier)))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private Message fetchMessage(UserInstance publicationOwner, SortableIdentifier messageIdentifier)
        throws NotFoundException {
        return messageService.getMessage(publicationOwner, messageIdentifier);
    }

    private Message fetchMessage(UserInstance publicationOwner, URI messageId) throws NotFoundException {
        return messageService.getMessage(publicationOwner, messageId);
    }

    private SortableIdentifier createDoiRequestMessage(Publication publication, String message) {
        var publicationOwner = UserInstance.fromPublication(publication);
        var sender = UserInstance.create(SOME_SENDER, publicationOwner.getOrganizationUri());
        return createDoiRequestMessage(publication, message, sender);
    }

    private SortableIdentifier createDoiRequestMessage(Publication publication, String message, UserInstance sender) {
        return attempt(() -> messageService.createDoiRequestMessage(sender, publication, message)).orElseThrow();
    }

    private SortableIdentifier createSimpleMessage(Publication publication, String message) {
        var publicationOwner = UserInstance.fromPublication(publication);
        var sender = UserInstance.create(SOME_SENDER, publicationOwner.getOrganizationUri());
        return createSimpleMessage(publication, message, sender);
    }

    private SortableIdentifier createSimpleMessage(Publication publication, String message, UserInstance sender) {
        return attempt(() -> messageService.createSimpleMessage(sender, publication, message)).orElseThrow();
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

    private Message constructExpectedSimpleMessage(SortableIdentifier messageIdentifier,
                                                   Publication publication,
                                                   String messageText) {
        var sender = UserInstance.create(SOME_SENDER, publication.getPublisher().getId());
        var clock = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
        return Message.supportMessage(sender, publication, messageText, messageIdentifier, clock);
    }

    private Message constructExpectedDoiRequestMessage(SortableIdentifier messageIdentifier,
                                                       Publication publication,
                                                       String messageText) {
        var sender = UserInstance.create(SOME_SENDER, publication.getPublisher().getId());
        var clock = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
        return Message.doiRequestMessage(sender, publication, messageText, messageIdentifier, clock);
    }
}
package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.github.javafaker.Faker;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class MessageServiceTest extends ResourcesDynamoDbLocalTest {

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

    public static final String SAMPLE_HOST = "https://localhost/messages/";

    public static final int FIRST_ELEMENT = 0;
    private static final int SINGLE_EXPECTED_ELEMENT = 0;

    private MessageService messageService;
    private ResourceService resourceService;

    @BeforeEach
    public void initialize() {
        super.init();
        Clock clock = mockClock();
        messageService = new MessageService(client, clock);
        var httpClient= new FakeHttpClient();
        resourceService = new ResourceService(client, httpClient, clock);
    }

    @Test
    public void createSimpleMessageStoresNewMessageInDatabase() throws TransactionFailedException, NotFoundException {
        var publication = createSamplePublication();
        var owner = extractOwner(publication);
        var messageText = randomString();

        var messageIdentifier = createSimpleMessage(publication, messageText);
        var savedMessage = fetchMessage(owner, messageIdentifier);

        var expectedMessage =
            constructExpectedSimpleMessage(savedMessage.getIdentifier(), publication, messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    public void createDoiRequestMessageStoresNewMessageInDatabaseIndicatingThatIsConnectedToTheRespectiveDoiRequest()
        throws TransactionFailedException, NotFoundException {
        var publication = createSamplePublication();
        var owner = extractOwner(publication);
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
    public void getMessagesByResourceIdentifierReturnsAllMessagesRelatedToResource()
        throws TransactionFailedException {
        var insertedPublication = createSamplePublication();
        var insertedMessages = insertSampleMessages(insertedPublication);

        var userInstance = extractOwner(insertedPublication);
        var resourceConversationOpt =
            messageService.getMessagesForResource(userInstance, insertedPublication.getIdentifier());

        assertThat(resourceConversationOpt.isPresent(), is(true));
        var resourceConversation = resourceConversationOpt.orElseThrow();
        var actualPublication = resourceConversation.getPublication();
        var expectedPublication = constructExpectedPublication(insertedPublication);

        assertThat(actualPublication, is(equalTo(expectedPublication)));

        MessageDto[] expectedMessages = constructExpectedMessagesDtos(insertedMessages);
        assertThat(resourceConversation.allMessages(), containsInAnyOrder(expectedMessages));
    }

    @Test
    public void createSimpleMessageThrowsExceptionWhenDuplicateIdentifierIsInserted()
        throws TransactionFailedException {
        messageService = serviceProducingDuplicateIdentifiers();
        var publication = createSamplePublication();

        var actualIdentifier = createSimpleMessage(publication, randomString());

        assertThat(actualIdentifier, is(equalTo(SOME_IDENTIFIER)));

        Executable action = () -> createSimpleMessage(publication, randomString());
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), is(instanceOf(TransactionFailedException.class)));
    }

    @Test
    public void getMessageByOwnerAndIdReturnsStoredMessage() throws TransactionFailedException, NotFoundException {
        Publication publication = createSamplePublication();
        String messageText = randomString();
        var messageIdentifier = createSimpleMessage(publication, messageText);
        var savedMessage = fetchMessage(extractOwner(publication), messageIdentifier);
        var expectedMessage = constructExpectedSimpleMessage(savedMessage.getIdentifier(), publication,
                                                             messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    public void getMessageByKeyReturnsStoredMessage() throws TransactionFailedException, NotFoundException {

        var publication = createSamplePublication();
        var messageText = randomString();
        var messageIdentifier = createSimpleMessage(publication, messageText);

        var savedMessage = messageService.getMessage(extractOwner(publication), messageIdentifier);
        var expectedMessage = constructExpectedSimpleMessage(savedMessage.getIdentifier(), publication,
                                                             messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    public void getMessageByIdAndOwnerReturnsStoredMessage() throws TransactionFailedException, NotFoundException {
        Publication publication = createSamplePublication();
        String messageText = randomString();
        var messageIdentifier = createSimpleMessage(publication, messageText);
        var sampleMessageUri = URI.create(SAMPLE_HOST + messageIdentifier.toString());
        var savedMessage = fetchMessage(extractOwner(publication), sampleMessageUri);
        var expectedMessage = constructExpectedSimpleMessage(savedMessage.getIdentifier(), publication,
                                                             messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    public void listMessagesForCustomerAndStatusListsAllMessagesForGivenCustomerAndStatus() throws NotFoundException {
        var createdPublications = createPublicationsOfDifferentOwnersInSameOrg();
        var savedMessages = createOneMessagePerPublication(createdPublications);

        var publisherId = createdPublications.get(FIRST_ELEMENT).getPublisher().getId();
        var actualConversation = messageService.listMessagesForCurator(publisherId, MessageStatus.UNREAD);

        var expectedConversation = constructExpectedCuratorsMessageView(publisherId, savedMessages);
        assertThat(actualConversation, contains(expectedConversation));
    }

    @Test
    public void listMessagesForCustomerAndStatusReturnsMessagesOfSingleCustomer() throws NotFoundException {
        var createdPublications = createPublicationsOfDifferentOwnersInDifferentOrg();
        var allMessagesOfAllCustomers = createOneMessagePerPublication(createdPublications);

        var customerId = createdPublications.get(FIRST_ELEMENT).getPublisher().getId();
        var actualConversations = messageService.listMessagesForCurator(customerId, MessageStatus.UNREAD);

        var expectedConversations = constructExpectedCuratorsMessageView(customerId, allMessagesOfAllCustomers);

        assertThat(actualConversations, contains(expectedConversations));
    }

    @Test
    public void listMessagesForUserReturnsAllMessagesConnectedToUser() throws TransactionFailedException {
        var publication1 = createSamplePublication();
        var publication2 = createSamplePublication();

        var messagesForPublication1 = insertSampleMessages(publication1);
        var messagesForPublication2 = insertSampleMessages(publication2);

        var actualMessages = messageService.listMessagesForUser(extractOwner(publication1));
        var expectedMessagesForPublication1 = constructExpectedMessages(messagesForPublication1);
        var expectedMessagesFromPublication2 = constructExpectedMessages(messagesForPublication2);
        var expectedMessages = List.of(
            expectedMessagesForPublication1,
            expectedMessagesFromPublication2
        );

        assertThat(actualMessages, is(equalTo(expectedMessages)));
    }

    public ResourceConversation constructExpectedMessages(List<Message> messagesForPublication) {
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
                .build();

        return new Publication.Builder()
                   .withIdentifier(insertedPublication.getIdentifier())
                   .withOwner(insertedPublication.getOwner())
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
                                            .withOwner(SOME_OTHER_OWNER)
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
            UserInstance owner = extractOwner(createdPublication);
            Message savedMessage = fetchMessage(owner, messageIdentifier);
            savedMessages.add(savedMessage);
        }
        return savedMessages;
    }

    private List<Publication> createPublicationsOfDifferentOwnersInSameOrg() {
        var publicationOfSomeOwner = PublicationGenerator.publicationWithoutIdentifier();
        var publicationOfDifferentOwner = PublicationGenerator.publicationWithoutIdentifier()
                                              .copy().withOwner(SOME_OTHER_OWNER).build();
        var newPublications = List.of(publicationOfSomeOwner, publicationOfDifferentOwner);
        return persistPublications(newPublications);
    }

    private List<Publication> persistPublications(List<Publication> newPublications) {
        return newPublications.stream()
                   .map(attempt(pub -> resourceService.createPublication(pub)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private Supplier<SortableIdentifier> duplicateIdentifierSupplier() {
        return () -> SOME_IDENTIFIER;
    }

    private Publication createSamplePublication() throws TransactionFailedException {
        return resourceService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }

    private List<Message> insertSampleMessages(Publication publication) {
        var publicationOwner = extractOwner(publication);
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
        var publicationOwner = extractOwner(publication);
        var sender = new UserInstance(SOME_SENDER, publicationOwner.getOrganizationUri());
        return createDoiRequestMessage(publication, message, sender);
    }

    private SortableIdentifier createDoiRequestMessage(Publication publication, String message, UserInstance sender) {
        return attempt(() -> messageService.createDoiRequestMessage(sender, publication, message)).orElseThrow();
    }

    private SortableIdentifier createSimpleMessage(Publication publication, String message) {
        var publicationOwner = extractOwner(publication);
        var sender = new UserInstance(SOME_SENDER, publicationOwner.getOrganizationUri());
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
        var sender = new UserInstance(SOME_SENDER, publication.getPublisher().getId());
        var clock = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
        return Message.supportMessage(sender, publication, messageText, messageIdentifier, clock);
    }

    private Message constructExpectedDoiRequestMessage(SortableIdentifier messageIdentifier,
                                                       Publication publication,
                                                       String messageText) {
        var sender = new UserInstance(SOME_SENDER, publication.getPublisher().getId());
        var clock = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
        return Message.doiRequestMessage(sender, publication, messageText, messageIdentifier, clock);
    }
}
package no.unit.nva.publication.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.github.javafaker.Faker;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class MessageServiceTest extends ResourcesDynamoDbLocalTest {

    public static final Faker FAKER = new Faker();
    public static final URI SOME_ORG = URI.create("https://example.org/1234");
    public static final String SOME_SENDER = "some@user";
    public static final UserInstance SAMPLE_SENDER_USER_INSTANCE = new UserInstance(SOME_SENDER, SOME_ORG);
    public static final SortableIdentifier SOME_IDENTIFIER = SortableIdentifier.next();
    public static final String SOME_OWNER = "some@owner";
    public static final UserInstance SAMPLE_OWNER_INSTANCE = new UserInstance(SOME_OWNER, SOME_ORG);
    public static final Instant MESSAGE_CREATION_TIME = Instant.parse("2007-12-03T10:15:30.00Z");
    public static final Instant SECOND_MESSAGE_CREATION_TIME = MESSAGE_CREATION_TIME.plus(Period.ofDays(2));
    public static final Instant THIRD_MESSAGE_CREATION_TIME = SECOND_MESSAGE_CREATION_TIME.plus(Period.ofDays(2));
    public static final int NUMBER_OF_SAMPLE_MESSAGES = 3;

    public static final String SOME_OTHER_OWNER = "someOther@owner";
    public static final URI SOME_OTHER_ORG = URI.create("https://some.other.example.org/98765");
    public static final int FIRST_ELEMENT = 0;

    private MessageService messageService;
    private ResourceService resourceService;

    @BeforeEach
    public void initialize() {
        super.init();
        Clock clock = mockClock();
        messageService = new MessageService(client, clock);
        resourceService = new ResourceService(client, clock);
    }

    @Test
    public void createMessageStoresNewMessageInDatabase() throws TransactionFailedException {
        String expectedMessageText = randomString();
        SortableIdentifier messageIdentifier = createMessage(expectedMessageText);
        Message savedMessage = messageService.getMessage(SAMPLE_OWNER_INSTANCE, messageIdentifier);
        Message expectedMessage = constructExpectedMessage(savedMessage.getIdentifier(), expectedMessageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    public void createMessageThrowsExceptionWhenDuplicateIdentifierIsInserted() throws TransactionFailedException {
        messageService = serviceProducingDuplicateIdentifiers();
        SortableIdentifier messageIdentifier = createMessage(randomString());

        assertThat(messageIdentifier, is(equalTo(SOME_IDENTIFIER)));

        Executable expectedFailingActio = () -> createMessage(randomString());
        TransactionFailedException exception = assertThrows(TransactionFailedException.class, expectedFailingActio);
        assertThat(exception.getCause(), is(instanceOf(TransactionCanceledException.class)));
    }

    @Test
    public void getMessageByKeyReturnsStoredMessage() throws TransactionFailedException {
        String expectedMessageText = randomString();
        SortableIdentifier messageIdentifier = createMessage(expectedMessageText);
        Message savedMessage = messageService.getMessage(SAMPLE_OWNER_INSTANCE, messageIdentifier);
        Message expectedMessage = constructExpectedMessage(savedMessage.getIdentifier(), expectedMessageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    public void getMessagesByResourceIdentifierReturnsAllMessagesRelatedToResource()
        throws TransactionFailedException {
        var insertedPublication = createSamplePublication();
        List<Message> insertedMessages = insertSampleMessages(insertedPublication);

        UserInstance userInstance = extractUserInstance(insertedPublication);
        ResourceMessages resourceMessages =
            messageService.getMessagesForResource(userInstance, insertedPublication.getIdentifier());

        Message[] expectedMessages = listToArray(insertedMessages);
        assertThat(resourceMessages.getMessages(), contains(expectedMessages));
    }

    @Test
    public void listMessagesForCustomerAndStatusListsAllMessagesForGivenCustomerAndStatus() {
        List<Publication> createdPublications = createPublicationsOfDifferentOwnersInSameOrg();
        List<Message> savedMessages = createOneMessagePerPublication(createdPublications);

        URI publisherId = createdPublications.get(FIRST_ELEMENT).getPublisher().getId();
        List<Message> actualMessages =
            messageService.listMessages(publisherId, MessageStatus.UNREAD);

        assertThat(actualMessages, is(equalTo(savedMessages)));
    }

    @Test
    public void listMessagesForCustomerAndStatusReturnsMessagesOfSingleCustomer() {
        var createdPublications = createPublicationsOfDifferentOwnersInDifferentOrg();
        var allMessagesOfAllCustomers = createOneMessagePerPublication(createdPublications);

        URI customerId = createdPublications.get(FIRST_ELEMENT).getPublisher().getId();
        List<Message> actualMessages =
            messageService.listMessages(customerId, MessageStatus.UNREAD);

        var expectedMessages = allMessagesOfAllCustomers.stream()
                                   .filter(message -> message.getCustomerId().equals(customerId))
                                   .collect(Collectors.toList());

        assertThat(actualMessages, is(equalTo(expectedMessages)));
    }

    public String randomString() {
        return FAKER.lorem().sentence();
    }

    private MessageService serviceProducingDuplicateIdentifiers() {
        return new MessageService(client, mockClock(), duplicateIdentifierSupplier());
    }

    private Message[] listToArray(List<Message> insertedMessages) {
        Message[] insertedMessagesArray = new Message[insertedMessages.size()];
        insertedMessages.toArray(insertedMessagesArray);
        return insertedMessagesArray;
    }

    private List<Publication> createPublicationsOfDifferentOwnersInDifferentOrg() {
        Publication publicationOfSomeOrg = PublicationGenerator.publicationWithoutIdentifier();
        Organization someOtherOrg = new Organization.Builder().withId(SOME_OTHER_ORG).build();
        Publication publicationOfDifferentOrg = publicationOfSomeOrg
                                                    .copy()
                                                    .withOwner(SOME_OTHER_OWNER)
                                                    .withPublisher(someOtherOrg)
                                                    .build();
        List<Publication> newPublications = List.of(publicationOfSomeOrg, publicationOfDifferentOrg);
        return persistPublications(newPublications);
    }

    private List<Message> createOneMessagePerPublication(List<Publication> createdPublications) {
        List<Message> savedMessages = new ArrayList<>();

        for (Publication createdPublication : createdPublications) {
            var messageIdentifier = createMessage(createdPublication, randomString());
            var owner = extractUserInstance(createdPublication);
            var savedMessage = fetchMessage(owner, messageIdentifier);
            savedMessages.add(savedMessage);
        }
        return savedMessages;
    }

    private Message fetchMessage(UserInstance owner, SortableIdentifier messageIdentifier) {
        return messageService.getMessage(owner, messageIdentifier);
    }

    private List<Publication> createPublicationsOfDifferentOwnersInSameOrg() {
        Publication publicationOfSomeOwner = PublicationGenerator.publicationWithoutIdentifier();
        Publication publicationOfDifferentOwner = PublicationGenerator.publicationWithoutIdentifier()
                                                      .copy().withOwner(SOME_OTHER_OWNER).build();
        List<Publication> newPublications = List.of(publicationOfSomeOwner, publicationOfDifferentOwner);
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

    private UserInstance extractUserInstance(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }

    private Publication createSamplePublication() throws TransactionFailedException {
        return resourceService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }

    private List<Message> insertSampleMessages(Publication publication) {
        UserInstance publicationOwner = extractUserInstance(publication);
        return IntStream.range(0, NUMBER_OF_SAMPLE_MESSAGES).boxed()
                   .map(i -> FAKER.lorem().sentence())
                   .map(attempt(message -> createMessage(publication, message)))
                   .map(Try::orElseThrow)
                   .map(identifier -> messageService.getMessage(publicationOwner, identifier))
                   .collect(Collectors.toList());
    }

    private SortableIdentifier createMessage(Publication publication, String message) {
        UserInstance publicationOwner = extractUserInstance(publication);
        UserInstance sender = new UserInstance(SOME_SENDER, publicationOwner.getOrganizationUri());
        return attempt(
            () -> messageService.createMessage(sender, publicationOwner, publication.getIdentifier(), message))
                   .orElseThrow();
    }


    private SortableIdentifier createMessage(String text) throws TransactionFailedException {
        return messageService.createMessage(SAMPLE_SENDER_USER_INSTANCE, SAMPLE_OWNER_INSTANCE,
            MessageServiceTest.SOME_IDENTIFIER, text);
    }

    private Clock mockClock() {
        Clock clock = mock(Clock.class);
        when(clock.instant())
            .thenReturn(MESSAGE_CREATION_TIME)
            .thenReturn(SECOND_MESSAGE_CREATION_TIME)
            .thenReturn(THIRD_MESSAGE_CREATION_TIME);
        return clock;
    }

    private Message constructExpectedMessage(SortableIdentifier savedMessageIdentifier, String text) {
        return Message.builder()
                   .withCreatedTime(MESSAGE_CREATION_TIME)
                   .withIdentifier(savedMessageIdentifier)
                   .withResourceIdentifier(SOME_IDENTIFIER)
                   .withCustomerId(SOME_ORG)
                   .withOwner(SOME_OWNER)
                   .withSender(SOME_SENDER)
                   .withStatus(MessageStatus.UNREAD)
                   .withText(text)
                   .build();
    }
}
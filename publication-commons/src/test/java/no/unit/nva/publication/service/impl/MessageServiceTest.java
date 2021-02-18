package no.unit.nva.publication.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.github.javafaker.Faker;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    public void createMessageStoresNewMessageInDatabase() {
        String expectedMessageText = randomString();
        SortableIdentifier messageIdentifier = createMessage(expectedMessageText);
        Message savedMessage = messageService.getMessage(SAMPLE_OWNER_INSTANCE, messageIdentifier);
        Message expectedMessage = constructExpectedMessage(savedMessage.getIdentifier(), expectedMessageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    public String randomString() {
        return FAKER.lorem().sentence();
    }

    @Test
    public void getMessageByKeyReturnsStoredMessage() {
        String expectedMeesageText = randomString();
        SortableIdentifier messageIdentifier = createMessage(expectedMeesageText);
        Message savedMessage = messageService.getMessage(SAMPLE_OWNER_INSTANCE, messageIdentifier);
        Message expectedMessage = constructExpectedMessage(savedMessage.getIdentifier(), expectedMeesageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
    }

    @Test
    public void getMessagesByResourceIdentifierReturnsAllMessagesRelatedToResource() throws TransactionFailedException {
        var insertedPublication = createSamplePublication();
        List<Message> insertedMessages = insertSampleMessages(insertedPublication);

        UserInstance userInstance = extractUserInstance(insertedPublication);
        ResourceMessages resourceMessages =
            messageService.getMessagesForResource(userInstance, insertedPublication.getIdentifier());

        Message[] expectedMessages = listToArray(insertedMessages);
        assertThat(resourceMessages.getMessages(), contains(expectedMessages));
    }

    private Message[] listToArray(List<Message> insertedMessages) {
        Message[] insertedMessagesArray = new Message[insertedMessages.size()];
        insertedMessages.toArray(insertedMessagesArray);
        return insertedMessagesArray;
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
                   .map(i -> createMessage(publication))
                   .map(identifier -> messageService.getMessage(publicationOwner, identifier))
                   .collect(Collectors.toList());
    }

    private SortableIdentifier createMessage(Publication publication) {
        UserInstance publicationOwner = extractUserInstance(publication);
        UserInstance sender = new UserInstance(SOME_SENDER, publicationOwner.getOrganizationUri());
        return messageService.createMessage(sender, publicationOwner, publication.getIdentifier(),
            randomString());
    }

    private SortableIdentifier createMessage(String text) {
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
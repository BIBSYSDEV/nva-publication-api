package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.StorageModelConstants;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.Environment;
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
    public static final int FIRST_ELEMENT = 0;
    public static final String SOME_VALID_HOST = "localhost";

    private MessageService messageService;
    private ResourceService resourceService;

    @BeforeEach
    public void initialize() {
        super.init();
        Clock clock = mockClock();
        Environment environment = setupEnvironment();
        StorageModelConstants.updateEnvironment(environment);
        messageService = new MessageService(client, clock);
        resourceService = new ResourceService(client, clock);
    }

    @Test
    public void createMessageStoresNewMessageInDatabase() throws TransactionFailedException {
        Publication publication = createSamplePublication();
        UserInstance owner = extractOwner(publication);
        String messageText = randomString();
        SortableIdentifier messageIdentifier = createSampleMessage(publication, messageText);

        Message savedMessage = fetchMessage(owner, messageIdentifier);
        Message expectedMessage = constructExpectedMessage(savedMessage.getIdentifier(), publication, messageText);
        assertThat(savedMessage, is(equalTo(expectedMessage)));

    }

    @Test
    public void getMessagesByResourceIdentifierReturnsAllMessagesRelatedToResource()
        throws TransactionFailedException {
        var insertedPublication = createSamplePublication();
        List<Message> insertedMessages = insertSampleMessages(insertedPublication);

        UserInstance publicationOwner = extractOwner(insertedPublication);
        ResourceMessages resourceMessages =
            messageService.getMessagesForResource(publicationOwner, insertedPublication.getIdentifier());

        Publication actualPublication = resourceMessages.getPublication();
        Publication expectedPublication = constructExpectedPublication(insertedPublication);
        assertThat(actualPublication, is(equalTo(expectedPublication)));

        MessageDto[] expectedMessages = constructExpectedMessagesDtos(insertedMessages);
        assertThat(resourceMessages.getMessages(), containsInAnyOrder(expectedMessages));
    }

    @Test
    public void createMessageThrowsExceptionWhenDuplicateIdentifierIsInserted() {
        messageService = serviceProducingDuplicateIdentifiers();
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        UserInstance sender = extractOwner(publication);
        SortableIdentifier messageIdentifier = createMessage(publication, randomString(), sender);

        assertThat(messageIdentifier, is(equalTo(SOME_IDENTIFIER)));

        Executable expectedFailingActio = () -> createMessage(publication, randomString(), sender);
        ;
        RuntimeException exception = assertThrows(RuntimeException.class, expectedFailingActio);
        assertThat(exception.getCause(), is(instanceOf(TransactionFailedException.class)));
    }

    @Test
    public void getMessageByKeyReturnsStoredMessage() throws TransactionFailedException {

        Publication publication = createSamplePublication();
        String messageText = randomString();
        SortableIdentifier messageIdentifier = createSampleMessage(publication, messageText);
        Message savedMessage = fetchMessage(extractOwner(publication), messageIdentifier);
        Message expectedMessage = constructExpectedMessage(savedMessage.getIdentifier(), publication, messageText);

        assertThat(savedMessage, is(equalTo(expectedMessage)));
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

        List<Message> expectedMessages = allMessagesOfAllCustomers.stream()
                                             .filter(message -> message.getCustomerId().equals(customerId))
                                             .collect(Collectors.toList());

        assertThat(actualMessages, is(equalTo(expectedMessages)));
    }

    @Test
    public void listMessagesForUserReturnsAllMessagesConnectedToUser() throws TransactionFailedException {
        Publication publication1 = createSamplePublication();
        Publication publication2 = createSamplePublication();

        List<Message> messagesForPublication1 = insertSampleMessages(publication1);
        List<Message> messagesForPublication2 = insertSampleMessages(publication2);

        List<ResourceMessages> actualMessages = messageService.listMessagesForUser(extractOwner(publication1));

        List<ResourceMessages> expectedMessages = List.of(
            ResourceMessages.fromMessageList(messagesForPublication1),
            ResourceMessages.fromMessageList(messagesForPublication2)
        );

        assertThat(actualMessages, is(equalTo(expectedMessages)));
    }

    private Environment setupEnvironment() {
        Environment env = mock(Environment.class);
        when(env.readEnv(StorageModelConstants.HOST_ENV_VARIABLE_NAME)).thenReturn(SOME_VALID_HOST);
        return env;
    }


    private MessageDto[] constructExpectedMessagesDtos(List<Message> insertedMessages) {
        List<MessageDto> messages = insertedMessages.stream()
                                        .map(MessageDto::fromMessage)
                                        .collect(Collectors.toList());
        MessageDto[] messagesArray = new MessageDto[messages.size()];
        messages.toArray(messagesArray);
        return messagesArray;
    }

    private Publication constructExpectedPublication(Publication insertedPublication) {
        EntityDescription entityDescription =
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
            var messageIdentifier = createSampleMessage(createdPublication, randomString());
            var owner = extractOwner(createdPublication);

            var savedMessage = fetchMessage(owner, messageIdentifier);
            savedMessages.add(savedMessage);
        }
        return savedMessages;
    }

    private Message fetchMessage(UserInstance publicationOwner, SortableIdentifier messageIdentifier) {
        return messageService.getMessage(publicationOwner, messageIdentifier);
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

    private Publication createSamplePublication() throws TransactionFailedException {
        return resourceService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }

    private List<Message> insertSampleMessages(Publication publication) {
        UserInstance publicationOwner = extractOwner(publication);
        return IntStream.range(0, NUMBER_OF_SAMPLE_MESSAGES).boxed()
                   .map(i -> FAKER.lorem().sentence())
                   .map(attempt(message -> createMessage(publication, message, extractOwner(publication))))
                   .map(Try::orElseThrow)
                   .map(identifier -> messageService.getMessage(publicationOwner, identifier))
                   .collect(Collectors.toList());
    }

    private SortableIdentifier createSampleMessage(Publication publication, String message) {
        UserInstance publicationOwner = extractOwner(publication);
        UserInstance sender = new UserInstance(SOME_SENDER, publicationOwner.getOrganizationUri());
        return createMessage(publication, message, sender);
    }

    private SortableIdentifier createMessage(Publication publication, String message, UserInstance sender) {
        return attempt(() -> messageService.createMessage(sender, publication, message)).orElseThrow();
    }

    private Clock mockClock() {
        Clock clock = mock(Clock.class);
        when(clock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(MESSAGE_CREATION_TIME)
            .thenReturn(SECOND_MESSAGE_CREATION_TIME)
            .thenReturn(THIRD_MESSAGE_CREATION_TIME);
        return clock;
    }

    private Message constructExpectedMessage(SortableIdentifier messageIdentifier, Publication publication,
                                             String messageText) {
        UserInstance sender = new UserInstance(SOME_SENDER, publication.getPublisher().getId());

        Clock clock = Clock.fixed(MESSAGE_CREATION_TIME, Clock.systemDefaultZone().getZone());
        return Message.simpleMessage(sender, publication, messageText, messageIdentifier, clock);

    }
}
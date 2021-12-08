package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.github.javafaker.Faker;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.UserInstance;
import org.junit.jupiter.api.Test;

class ResourceConversationTest {

    private static final int NUMBER_OF_PUBLICATIONS = 3;
    public static final boolean WITH_IDENTIFIER = true;
    public static final Faker FAKER = Faker.instance();
    public static final int SMALL_WAITING_TIME = 2;

    public static final String USER_IDENTIFIER = "userIdentifier";
    public static final URI SOME_PUBLISHER = URI.create("https://www.example.org");
    public static final UserInstance SOME_USER = new UserInstance(USER_IDENTIFIER, SOME_PUBLISHER);
    public static final int SINGLE_OBJECT = 0;

    @Test
    public void returnsListOfResourceConversationsForEachMentionedResource() {
        var publications =
            PublicationGenerator.samplePublicationsOfDifferentOwners(NUMBER_OF_PUBLICATIONS, WITH_IDENTIFIER);
        var allMessages = twoMessagesPerPublication(publications);

        List<ResourceConversation> conversations = ResourceConversation.fromMessageList(allMessages);
        MessageDto oldestMessage = conversations.get(0).getOldestMessage();
        var expectedOldestMessage = allMessages
                                        .stream()
                                        .min(Comparator.comparing(Message::getCreatedTime))
                                        .orElseThrow();

        assertThat(oldestMessage.getMessageIdentifier(), is(equalTo(expectedOldestMessage.getIdentifier())));
        assertThat(oldestMessage.getMessageIdentifier(), is(not(nullValue())));
    }

    @Test
    public void getRequestMessagesReturnsMessagesOfSpecifiedType() {
        var publication = PublicationGenerator.publicationWithIdentifier();
        var supportMessage = supportMessage(publication);
        var doiRequestMessage = doiRequestMessage(publication);
        var messageList = List.of(supportMessage, doiRequestMessage);
        var resourceConversation = ResourceConversation.fromMessageList(messageList);

        var actualMessages = resourceConversation
                                 .get(SINGLE_OBJECT)
                                 .getMessageCollectionOfType(MessageType.DOI_REQUEST)
                                 .getMessages();
        assertThat(actualMessages, contains(MessageDto.fromMessage(doiRequestMessage)));
        assertThat(actualMessages, contains(MessageDto.fromMessage(doiRequestMessage)));
    }

    private Message doiRequestMessage(Publication publication) {
        return Message.doiRequestMessage(SOME_USER,
                                         publication,
                                         randomString(),
                                         SortableIdentifier.next(),
                                         Clock.systemDefaultZone());
    }

    private Message supportMessage(Publication publication) {
        return Message.supportMessage(SOME_USER,
                                      publication,
                                      randomString(),
                                      SortableIdentifier.next(),
                                      Clock.systemDefaultZone());
    }

    private ArrayList<Message> twoMessagesPerPublication(List<Publication> publications) {
        var messages = createOneMessagePerPublication(publications);
        var moreMessages = createOneMessagePerPublication(publications);
        var allMessages = new ArrayList<Message>();
        allMessages.addAll(messages);
        allMessages.addAll(moreMessages);
        return allMessages;
    }

    private List<Message> createOneMessagePerPublication(List<Publication> publications) {
        return publications.stream().map(this::createMessage).collect(Collectors.toList());
    }

    private Message createMessage(Publication publication) {
        waitForAvoidingSameTimeStampInMessages();
        return Message.supportMessage(extractOwner(publication),
                                      publication,
                                      randomString(),
                                      SortableIdentifier.next(),
                                      Clock.systemDefaultZone());
    }

    private void waitForAvoidingSameTimeStampInMessages() {
        try {
            Thread.sleep(SMALL_WAITING_TIME);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String randomString() {
        return FAKER.lorem().sentence();
    }
}
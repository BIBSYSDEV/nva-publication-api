package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.github.javafaker.Faker;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.storage.model.Message;
import org.junit.jupiter.api.Test;

class ResourceConversationTest {

    private static final int NUMBER_OF_PUBLICATIONS = 3;
    public static final boolean WITH_IDENTIFIER = true;
    public static final Faker FAKER = Faker.instance();
    public static final int SMALL_WAITING_TIME = 2;

    @Test
    public void returnsListOfResourceConversationsForEachMentionedResource() {
        var publications =
            PublicationGenerator.samplePublicationsOfDifferentOwners(NUMBER_OF_PUBLICATIONS, WITH_IDENTIFIER);
        var allMessages = twoMessagesPerPublication(publications);

        List<ResourceConversation> conversations = ResourceConversation.fromMessageList(allMessages);
        MessageDto oldestMessage = conversations.get(0).getMessages().get(0);
        var expectedOldestMessage = allMessages
                                        .stream()
                                        .min(Comparator.comparing(Message::getCreatedTime))
                                        .orElseThrow();

        assertThat(oldestMessage.getMessageIdentifier(), is(equalTo(expectedOldestMessage.getIdentifier())));
        assertThat(oldestMessage.getMessageIdentifier(),is(not(nullValue())));
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
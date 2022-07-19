package no.unit.nva.publication.service.impl;

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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.UserInstance;
import org.junit.jupiter.api.Test;

class ResourceConversationTest {
    
    public static final Faker FAKER = Faker.instance();
    public static final int SMALL_WAITING_TIME = 2;
    public static final String USER_IDENTIFIER = "userIdentifier";
    public static final URI SOME_PUBLISHER = URI.create("https://www.example.org");
    public static final UserInstance SOME_USER = UserInstance.create(USER_IDENTIFIER, SOME_PUBLISHER);
    public static final int SINGLE_OBJECT = 0;
    public static final int RESOURCE_CONVERSATION_OF_SINGLE_RESOURCE = 0;
    private static final int NUMBER_OF_PUBLICATIONS = 3;
    
    @Test
    public void returnsListOfResourceConversationsForEachMentionedResource() {
        var publications =
            samplePublicationsOfDifferentOwners();
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
    
    @Test
    void shouldReturnNewResourceConversationContainingMessagesOfOnlySpecifiedTypes() {
        var publication = PublicationGenerator.publicationWithIdentifier();
        var inputSupportMessages = List.of(supportMessage(publication), supportMessage(publication));
        var inputDoiRequestMessages = List.of(doiRequestMessage(publication), doiRequestMessage(publication));
        
        var allMessages = Stream.of(inputSupportMessages, inputDoiRequestMessages)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        
        var resourceConversation = ResourceConversation.fromMessageList(allMessages)
            .get(RESOURCE_CONVERSATION_OF_SINGLE_RESOURCE);
        
        var doiRequestConversationMessages = resourceConversation.ofMessageTypes(MessageType.DOI_REQUEST)
            .allMessages()
            .stream()
            .map(MessageDto::getText)
            .collect(Collectors.toList());
        var expectedDoiRequestConversationMessages = inputDoiRequestMessages
            .stream()
            .map(Message::getText)
            .collect(Collectors.toList())
            .toArray(String[]::new);
        assertThat(doiRequestConversationMessages, contains(expectedDoiRequestConversationMessages));
    }
    
    private List<Publication> samplePublicationsOfDifferentOwners() {
        return IntStream.range(0, ResourceConversationTest.NUMBER_OF_PUBLICATIONS).boxed()
            .map(ignored -> PublicationGenerator.randomPublication())
            .collect(Collectors.toList());
    }
    
    private Message doiRequestMessage(Publication publication) {
        return Message.create(SOME_USER, publication, randomString(), SortableIdentifier.next(),
            Clock.systemDefaultZone(), MessageType.DOI_REQUEST);
    }
    
    private Message supportMessage(Publication publication) {
        return Message.create(SOME_USER, publication, randomString(), SortableIdentifier.next(),
            Clock.systemDefaultZone(), MessageType.SUPPORT);
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
        return Message.create(UserInstance.fromPublication(publication), publication, randomString(),
            SortableIdentifier.next(), Clock.systemDefaultZone(), MessageType.SUPPORT);
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
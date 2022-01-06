package no.unit.nva.publication.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageType;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.SingletonCollector;

/**
 * Contains messages related to a single resource. The messages are sorted by date, oldest first.
 */
public class ResourceConversation implements JsonSerializable {

    private static final int OLDEST_MESSAGE = 0;
    @JsonProperty("publication")
    private PublicationSummary publicationSummary;
    private List<MessageCollection> messageCollections;
    @JsonIgnore
    private MessageDto oldestMessage;

    public ResourceConversation() {
    }

    /**
     * Returns a list of {@link ResourceConversation} objects by grouping the messages by resource. The {@link
     * ResourceConversation} with the oldest message is at the top of the list.
     *
     * @param messages a collection of messages.
     * @return a list of {@link ResourceConversation} instnaces with the oldest conversation on top.
     */
    public static List<ResourceConversation> fromMessageList(Collection<Message> messages) {
        return messages.stream()
                   .collect(groupByResource())
                   .values()
                   .stream()
                   .map(ResourceConversation::newConversationForResource)
                   .sorted(ResourceConversation::conversationWithOldestMessageFirst)
                   .collect(Collectors.toList());
    }

    public static PublicationSummary createPublicationSummary(Message mostRecentMessage) {
        String resourceTitleInMostRecentMessage = mostRecentMessage.getResourceTitle();
        Organization publisher = constructPublisher(mostRecentMessage);
        EntityDescription entityDescription = constructEntityDescription(resourceTitleInMostRecentMessage);

        Publication publication = new Publication.Builder()
                   .withOwner(mostRecentMessage.getOwner())
                   .withPublisher(publisher)
                   .withEntityDescription(entityDescription)
                   .withIdentifier(mostRecentMessage.getResourceIdentifier())
                   .build();

        //TODO: remove use of Publication.Builder above
        return PublicationSummary.fromPublication(publication);
    }

    public List<MessageCollection> getMessageCollections() {
        return messageCollections;
    }

    public void setMessageCollections(List<MessageCollection> messageCollections) {
        this.messageCollections = messageCollections;
    }

    public int conversationWithOldestMessageFirst(ResourceConversation that) {
        return this.getOldestMessage().getDate().compareTo(that.getOldestMessage().getDate());
    }

    public PublicationSummary getPublicationSummary() {
        return publicationSummary;
    }

    public void setPublicationSummary(PublicationSummary publicationSummary) {
        this.publicationSummary = publicationSummary;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPublicationSummary(), getMessageCollections());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceConversation)) {
            return false;
        }
        ResourceConversation that = (ResourceConversation) o;
        return Objects.equals(getPublicationSummary(), that.getPublicationSummary())
               && Objects.equals(getMessageCollections(), that.getMessageCollections());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }

    public MessageDto getOldestMessage() {
        return oldestMessage;
    }

    public void setOldestMessage(MessageDto message) {
        this.oldestMessage = message;
    }

    public MessageCollection getMessageCollectionOfType(MessageType messageType) {
        return
            this.getMessageCollections()
                .stream()
                .filter(messageCollection -> Objects.equals(messageType, messageCollection.getMessageType()))
                .collect(SingletonCollector.collectOrElse(MessageCollection.empty(messageType)));
    }

    public List<MessageDto> allMessages() {
        return this.getMessageCollections()
                   .stream()
                   .flatMap(messageCollection -> messageCollection.getMessages().stream())
                   .collect(Collectors.toList());
    }

    private static Message newestMessage(List<Message> messages) {
        return messages.get(messages.size() - 1);
    }

    private static Collector<Message, ?, Map<SortableIdentifier, List<Message>>> groupByResource() {
        return Collectors.groupingBy(Message::getResourceIdentifier);
    }

    private static ResourceConversation newConversationForResource(List<Message> messages) {
        messages.sort(ResourceConversation::oldestMessageOnTop);
        Message mostRecentMessage = newestMessage(messages);
        Message oldestMessage = messages.get(OLDEST_MESSAGE);
        PublicationSummary publicationSummary = createPublicationSummary(mostRecentMessage);
        return createResourceConversation(messages, publicationSummary, oldestMessage);
    }

    private static int oldestMessageOnTop(Message left, Message right) {
        return left.getCreatedTime().compareTo(right.getCreatedTime());
    }

    private static EntityDescription constructEntityDescription(String title) {
        return new EntityDescription.Builder()
                   .withMainTitle(title)
                   .build();
    }

    private static ResourceConversation createResourceConversation(List<Message> messages,
                                                                   PublicationSummary publicationSummary,
                                                                   Message oldestMessage) {
        final List<MessageCollection> conversationMessages = createMessageCollections(messages);
        ResourceConversation result = new ResourceConversation();
        result.setPublicationSummary(publicationSummary);
        result.setMessageCollections(conversationMessages);
        result.setOldestMessage(MessageDto.fromMessage(oldestMessage));

        return result;
    }

    private static List<MessageCollection> createMessageCollections(List<Message> messages) {
        return MessageCollection.groupMessagesByType(messages);
    }

    private static Organization constructPublisher(Message message) {
        return new Builder().withId(message.getCustomerId()).build();
    }
}

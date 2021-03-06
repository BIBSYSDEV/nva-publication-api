package no.unit.nva.publication.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.storage.model.Message;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class ResourceConversation implements JsonSerializable {

    private static final int OLDEST_MESSAGE_INDEX = 0;
    private Publication publication;
    private List<MessageDto> messages;
    private MessageDto oldestMessage;

    public ResourceConversation() {
    }

    public static List<ResourceConversation> fromMessageList(List<Message> messages) {
        return messages.stream()
                   .collect(grouByResource())
                   .values()
                   .stream()
                   .map(ResourceConversation::newConversationForResource)
                   .sorted(ResourceConversation::conversationWithOldestMessageFirst)
                   .collect(Collectors.toList());
    }

    private static Collector<Message, ?, Map<SortableIdentifier, List<Message>>> grouByResource() {
        return Collectors.groupingBy(Message::getResourceIdentifier);
    }

    public int conversationWithOldestMessageFirst(ResourceConversation that) {
        return this.getOldestMessage().getDate().compareTo(that.getOldestMessage().getDate());
    }

    private MessageDto getOldestMessage() {
        return oldestMessage;
    }

    public static Publication createPublicationDescription(Message mostRecentMessage) {
        String resourceTitleInMostRecentMessage = mostRecentMessage.getResourceTitle();
        Organization publisher = constructPublisher(mostRecentMessage);
        EntityDescription entityDescription = constructEntityDescription(resourceTitleInMostRecentMessage);

        return new Publication.Builder()
                   .withOwner(mostRecentMessage.getOwner())
                   .withPublisher(publisher)
                   .withEntityDescription(entityDescription)
                   .withIdentifier(mostRecentMessage.getResourceIdentifier())
                   .build();
    }

    public List<MessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageDto> messages) {
        this.messages = messages;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPublication(), getMessages());
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
        return Objects.equals(getPublication(), that.getPublication()) && Objects.equals(getMessages(),
                                                                                         that.getMessages());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }

    private static ResourceConversation newConversationForResource(List<Message> messages) {
        messages.sort(ResourceConversation::oldestMessageForResource);
        Message mostRecentMessage = newestMessage(messages);
        Publication publication = createPublicationDescription(mostRecentMessage);
        return createResourceMessage(messages, publication);
    }

    private static Message newestMessage(List<Message> messages) {
        return messages.get(messages.size() - 1);
    }

    private static int oldestMessageForResource(Message left, Message right) {
        return left.getCreatedTime().compareTo(right.getCreatedTime());
    }

    private static ResourceConversation createResourceMessage(List<Message> messages, Publication publication) {
        ResourceConversation result = new ResourceConversation();
        result.setPublication(publication);
        List<MessageDto> conversationMessages = transformMessages(messages);
        result.setMessages(conversationMessages);
        result.setOldestMessage(conversationMessages.get(OLDEST_MESSAGE_INDEX));
        return result;
    }

    private void setOldestMessage(MessageDto message) {
        this.oldestMessage = message;
    }



    private static List<MessageDto> transformMessages(List<Message> messages) {
        return messages.stream().map(MessageDto::fromMessage).collect(Collectors.toList());
    }

    private static EntityDescription constructEntityDescription(String title) {
        return new EntityDescription.Builder()
                   .withMainTitle(title)
                   .build();
    }

    private static Organization constructPublisher(Message message) {
        return new Builder().withId(message.getCustomerId()).build();
    }
}

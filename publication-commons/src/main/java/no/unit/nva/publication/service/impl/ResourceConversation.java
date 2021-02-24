package no.unit.nva.publication.service.impl;

import static java.util.Objects.isNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.storage.model.Message;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class ResourceConversation implements JsonSerializable {

    public static final int NEWEST_MESSAGE = 0;
    private Publication publication;
    private List<MessageDto> messages;

    public ResourceConversation() {
    }

    public static Optional<ResourceConversation> fromMessageList(List<Message> messages) {
        if (isEmpty(messages)) {
            return Optional.empty();
        }
        return Optional.of(createNewResourceMessageInstance(messages));
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

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public List<MessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageDto> messages) {
        this.messages = messages;
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

    private static ResourceConversation createNewResourceMessageInstance(List<Message> messages) {
        messages.sort(ResourceConversation::newestFirst);
        Message mostRecentMessage = messages.get(NEWEST_MESSAGE);
        Publication publication = createPublicationDescription(mostRecentMessage);
        return createResourceMessage(messages, publication);
    }

    private static int newestFirst(Message o1, Message o2) {
        return o2.getCreatedTime().compareTo(o1.getCreatedTime());
    }

    private static ResourceConversation createResourceMessage(List<Message> messages, Publication publication) {
        ResourceConversation result = new ResourceConversation();
        result.setPublication(publication);
        result.setMessages(transformMessages(messages));
        return result;
    }

    private static List<MessageDto> transformMessages(List<Message> messages) {
        return messages.stream().map(MessageDto::fromMessage).collect(Collectors.toList());
    }

    private static EntityDescription constructEntityDescription(String resourceTitleInMostRecentMessage) {
        return new EntityDescription.Builder()
                   .withMainTitle(resourceTitleInMostRecentMessage)
                   .build();
    }

    private static Organization constructPublisher(Message mostRecentMessage) {
        return new Builder().withId(mostRecentMessage.getCustomerId()).build();
    }

    private static boolean isEmpty(List<Message> messages) {
        return isNull(messages) || messages.isEmpty();
    }
}

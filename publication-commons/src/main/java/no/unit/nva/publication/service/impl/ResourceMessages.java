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

public class ResourceMessages implements JsonSerializable {

    public static final String EMPTY_MESSAGE_LIST_ERROR = "Message list cannot be empty";
    private Publication publication;
    private List<MessageDto> messages;

    public ResourceMessages() {
    }

    public static Optional<ResourceMessages> fromMessageList(List<Message> messages) {
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
        if (!(o instanceof ResourceMessages)) {
            return false;
        }
        ResourceMessages that = (ResourceMessages) o;
        return Objects.equals(getPublication(), that.getPublication()) && Objects.equals(getMessages(),
            that.getMessages());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }

    private static ResourceMessages createNewResourceMessageInstance(List<Message> messages) {
        messages.sort(ResourceMessages::oldestFirst);
        Message mostRecentMessage = newestMessage(messages);
        Publication publication = createPublicationDescription(mostRecentMessage);
        return createResourceMessage(messages, publication);
    }

    private static Message newestMessage(List<Message> messages) {
        return messages.get(messages.size() - 1);
    }

    private static int oldestFirst(Message left, Message right) {
        return left.getCreatedTime().compareTo(right.getCreatedTime());
    }

    private static ResourceMessages createResourceMessage(List<Message> messages, Publication publication) {
        ResourceMessages result = new ResourceMessages();
        result.setPublication(publication);
        result.setMessages(transformMessages(messages));
        return result;
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

    private static boolean isEmpty(List<Message> messages) {
        return isNull(messages) || messages.isEmpty();
    }
}

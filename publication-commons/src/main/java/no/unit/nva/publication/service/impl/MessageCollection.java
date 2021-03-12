package no.unit.nva.publication.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageType;
import nva.commons.core.JacocoGenerated;

public class MessageCollection {

    private List<MessageDto> messages;
    private MessageType messageType;

    @JacocoGenerated
    public MessageCollection() {

    }

    private MessageCollection(MessageType messageType, List<Message> messages) {
        this.messageType = messageType;
        this.messages = messages.stream().map(MessageDto::fromMessage).collect(Collectors.toList());
    }

    public static List<MessageCollection> groupMessagesByType(Collection<Message> messages) {
        return messages.stream()
                   .collect(Collectors.groupingBy(Message::getMessageType))
                   .entrySet()
                   .stream()
                   .map(group -> new MessageCollection(group.getKey(), group.getValue()))
                   .collect(Collectors.toList());
    }

    public static MessageCollection empty(MessageType messageType) {
        return new MessageCollection(messageType, Collections.emptyList());
    }

    @JacocoGenerated
    public List<MessageDto> getMessages() {
        return messages;
    }

    @JacocoGenerated
    public void setMessages(List<MessageDto> messages) {
        this.messages = messages;
    }

    @JacocoGenerated
    public MessageType getMessageType() {
        return messageType;
    }

    @JacocoGenerated
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getMessages(), getMessageType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageCollection)) {
            return false;
        }
        MessageCollection that = (MessageCollection) o;
        return Objects.equals(getMessages(), that.getMessages()) && getMessageType() == that.getMessageType();
    }
}

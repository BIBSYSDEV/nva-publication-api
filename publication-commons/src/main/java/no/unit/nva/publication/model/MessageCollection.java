package no.unit.nva.publication.model;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageType;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(MessageCollection.TYPE_VALUE)
public class MessageCollection {
    
    public static final String TYPE_VALUE = "MessageCollection";
    public static final String MESSAGES_FIELD = "messages";
    public static final String MESSAGE_TYPE_FIELD = "messageType";
    
    @JsonProperty(MESSAGES_FIELD)
    private List<MessageDto> messages;
    @JsonProperty(MESSAGE_TYPE_FIELD)
    private MessageType messageType;
    @JsonIgnore
    private List<Message> messagesInternalStructure;
    
    @JacocoGenerated
    public MessageCollection() {
    
    }
    
    private MessageCollection(MessageType messageType, List<Message> messages) {
        this.messageType = messageType;
        this.messagesInternalStructure = messages;
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
        return isNull(messages) ? Collections.emptyList() : messages;
    }
    
    @JacocoGenerated
    public void setMessages(List<MessageDto> messages) {
        this.messages = messages;
    }
    
    @JsonIgnore
    public List<Message> getMessagesInternalStructure() {
        return messagesInternalStructure;
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
    
    @JacocoGenerated
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

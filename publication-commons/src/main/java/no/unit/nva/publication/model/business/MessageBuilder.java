package no.unit.nva.publication.model.business;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;

public class MessageBuilder {
    
    private final Message message;
    
    public MessageBuilder() {
        this.message = new Message();
    }
    
    public MessageBuilder withIdentifier(SortableIdentifier identifier) {
        message.setIdentifier(identifier);
        return this;
    }
    
    public MessageBuilder withOwner(String owner) {
        message.setOwner(owner);
        return this;
    }
    
    public MessageBuilder withCustomerId(URI customerId) {
        message.setCustomerId(customerId);
        return this;
    }
    
    public MessageBuilder withStatus(MessageStatus status) {
        message.setStatus(status);
        return this;
    }
    
    public MessageBuilder withSender(String sender) {
        message.setSender(sender);
        return this;
    }
    
    public MessageBuilder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
        message.setResourceIdentifier(resourceIdentifier);
        return this;
    }
    
    public MessageBuilder withText(String text) {
        message.setText(text);
        return this;
    }
    
    public MessageBuilder withCreatedTime(Instant createdTime) {
        message.setCreatedTime(createdTime);
        return this;
    }
    
    public MessageBuilder withResourceTitle(String resourceTitle) {
        message.setResourceTitle(resourceTitle);
        return this;
    }
    
    public MessageBuilder withMessageType(MessageType messageType) {
        message.setMessageType(messageType);
        return this;
    }
    
    public Message build() {
        return message;
    }
    
    public MessageBuilder withRowVersion(String rowVersion) {
        message.setRowVersion(rowVersion);
        return this;
    }
}

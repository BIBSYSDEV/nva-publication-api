package no.unit.nva.publication.messages;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.MessageType;


public class CreateMessageRequest {
    
    private String message;
    private SortableIdentifier publicationIdentifier;
    private MessageType messageType;
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public SortableIdentifier getPublicationIdentifier() {
        return publicationIdentifier;
    }
    
    public void setPublicationIdentifier(SortableIdentifier publicationIdentifier) {
        this.publicationIdentifier = publicationIdentifier;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

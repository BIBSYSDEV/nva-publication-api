package no.unit.nva.publication.messages;

import no.unit.nva.identifiers.SortableIdentifier;

public class CreateMessageRequest {

    private String message;
    private SortableIdentifier publicationIdentifier;
    private String messageType;

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
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

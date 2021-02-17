package no.unit.nva.publication.messages.create;

import no.unit.nva.identifiers.SortableIdentifier;

public class CreateMessageRequest {

    private String message;
    private SortableIdentifier publicationIdentifier;

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

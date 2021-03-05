package no.unit.nva.pubication.messages.list;

import no.unit.nva.identifiers.SortableIdentifier;

public class CreateMessageRequest {

    private boolean doiRequestRelated;
    private String message;
    private SortableIdentifier publicationIdentifier;

    public boolean isDoiRequestRelated() {
        return doiRequestRelated;
    }

    public void setDoiRequestRelated(boolean doiRequestRelated) {
        this.doiRequestRelated = doiRequestRelated;
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

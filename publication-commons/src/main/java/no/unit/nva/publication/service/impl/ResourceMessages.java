package no.unit.nva.publication.service.impl;

import java.util.List;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import nva.commons.core.JacocoGenerated;

public class ResourceMessages {

    private final Resource resource;
    private final List<Message> messages;

    public ResourceMessages(Resource resource, List<Message> messages) {
        this.resource = resource;
        this.messages = messages;
    }

    @JacocoGenerated
    public Resource getResource() {
        return resource;
    }

    @JacocoGenerated
    public List<Message> getMessages() {
        return messages;
    }
}

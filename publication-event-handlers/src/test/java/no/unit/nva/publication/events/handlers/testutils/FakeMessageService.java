package no.unit.nva.publication.events.handlers.testutils;

import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.UserInstance;

public class FakeMessageService extends MessageService {

    public FakeMessageService() {
        super(null, null);
    }

    @Override
    public Optional<ResourceConversation> getMessagesForResource(UserInstance user, SortableIdentifier identifier) {
        return Optional.empty();
    }
}

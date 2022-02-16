package no.unit.nva.expansion.model;

import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.UserInstance;


//TODO: this is a quick fix. The best solution is to send messages to a real service connected to local database.
public class FakeMessageService extends MessageService {

    public FakeMessageService() {
        super(null,null);
    }

    @Override
    public Optional<ResourceConversation> getMessagesForResource(UserInstance user, SortableIdentifier identifier) {
        return Optional.empty();
    }
}

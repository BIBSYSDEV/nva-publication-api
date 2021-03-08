package no.unit.nva.publication.migration;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestMessage;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.UserInstance;

public class PublicationMessagePair {

    private final Publication publication;
    private final DoiRequestMessage message;

    public PublicationMessagePair(Publication publication, DoiRequestMessage message) {
        this.publication = publication;
        this.message = message;
    }

    public Publication getPublication() {
        return publication;
    }

    public DoiRequestMessage getMessage() {
        return message;
    }

    public Message toMessage() {
        var owner = getOwner();
        return Message.doiRequestMessage(owner,
            getPublication(),
            getMessage().getText(),
            SortableIdentifier.next(),
            Clock.fixed(getMessage().getTimestamp(), Clock.systemDefaultZone().getZone())
        );
    }

    public UserInstance getOwner() {
        return extractOwner(publication);
    }
}

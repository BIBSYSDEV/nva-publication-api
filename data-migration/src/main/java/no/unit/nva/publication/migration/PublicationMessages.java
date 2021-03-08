package no.unit.nva.publication.migration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.DoiRequestMessage;
import no.unit.nva.model.Publication;

public class PublicationMessages {

    private final Publication publication;
    private final List<DoiRequestMessage> messages;

    public PublicationMessages(Publication publication) {
        this.publication = publication;
        this.messages = extractMessages(publication);
    }

    public Stream<PublicationMessagePair> createPublicationMessagePairs() {
        return messages.stream().map(this::createPublicationMessagePair);
    }

    private List<DoiRequestMessage> extractMessages(Publication publication) {
        return Optional.ofNullable(publication.getDoiRequest())
                   .map(DoiRequest::getMessages)
                   .orElse(Collections.emptyList());
    }

    private PublicationMessagePair createPublicationMessagePair(DoiRequestMessage message) {
        return new PublicationMessagePair(publication, message);
    }
}

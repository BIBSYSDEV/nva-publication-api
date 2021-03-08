package no.unit.nva.publication.migration;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.NotFoundException;

public class MessagesImporter extends DataImporter {

    private static final String RESOURCE_TYPE = "messages";
    private final MessageService messageService;

    public MessagesImporter(S3Driver s3Client, Path dataPath, MessageService messageService) {
        super(s3Client, dataPath);
        this.messageService = messageService;
    }

    public Stream<PublicationMessagePair> geDoiRequestMessages() {
        return getPublications().stream()
                   .filter(this::hasDoiRequest)
                   .map(PublicationMessages::new)
                   .flatMap(PublicationMessages::createPublicationMessagePairs);
    }

    public List<ResourceUpdate> insertMessages(Stream<PublicationMessagePair> messages) {
        var allMessages = messages.collect(Collectors.toList());
        return allMessages.stream().map(this::storeMessageInDb).collect(Collectors.toList());
    }

    private ResourceUpdate storeMessageInDb(PublicationMessagePair publicationMessagePair) {
        Publication publication = publicationMessagePair.getPublication();
        return attempt(() -> storeMessage(publicationMessagePair))
                   .map(message -> ResourceUpdate.createSuccessfulUpdate(RESOURCE_TYPE, publication, null))
                   .orElse(fail -> ResourceUpdate.createFailedUpdate(RESOURCE_TYPE, publication, fail.getException()));
    }

    private Message storeMessage(PublicationMessagePair publicationMessagePair)
        throws TransactionFailedException, NotFoundException {
        var messageId = messageService.writeMessageToDb(publicationMessagePair.toMessage());
        return messageService.getMessage(publicationMessagePair.getOwner(), messageId);
    }

    private boolean hasDoiRequest(Publication p) {
        return nonNull(p.getDoiRequest());
    }
}

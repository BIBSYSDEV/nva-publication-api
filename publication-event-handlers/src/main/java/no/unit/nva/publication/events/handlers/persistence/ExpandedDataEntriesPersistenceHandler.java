package no.unit.nva.publication.events.handlers.persistence;

import static no.unit.nva.publication.events.handlers.persistence.PersistedDocument.createIndexDocument;
import static no.unit.nva.publication.events.handlers.persistence.PersistenceConfig.PERSISTED_ENTRIES_BUCKET;
import static no.unit.nva.s3.S3Driver.GZIP_ENDING;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.IOException;
import java.net.URI;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.events.handlers.expandresources.RecoveryEntry;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.ResourceQueueClient;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandedDataEntriesPersistenceHandler
    extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC = "PublicationService.ExpandedEntry.Persisted";
    public static final String SENT_TO_RECOVERY_QUEUE_MESSAGE = "DateEntry has been sent to recovery queue: {}";
    public static final String RECOVERY_QUEUE = new Environment().readEnv("RECOVERY_QUEUE");
    private static final Logger logger = LoggerFactory.getLogger(ExpandedDataEntriesPersistenceHandler.class);
    private final QueueClient queueClient;
    private final S3Driver s3Reader;
    private final S3Driver s3Writer;

    @JacocoGenerated
    public ExpandedDataEntriesPersistenceHandler() {
        this(new S3Driver(PublicationEventsConfig.EVENTS_BUCKET), new S3Driver(PERSISTED_ENTRIES_BUCKET),
             ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE));
    }

    public ExpandedDataEntriesPersistenceHandler(S3Driver s3Reader, S3Driver s3Writer, QueueClient queueClient) {
        super(EventReference.class);
        this.s3Reader = s3Reader;
        this.s3Writer = s3Writer;
        this.queueClient = queueClient;
    }

    @Override
    protected EventReference processInputPayload(
        EventReference input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
        Context context) {
        var expandedResourceUpdate = readEvent(input);
        var indexDocument = createIndexDocument(expandedResourceUpdate);
        return attempt(() -> writeEntryToS3(indexDocument))
                   .map(item -> new EventReference(EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC, item))
                   .orElse(failure -> persistRecoveryMessage(failure, indexDocument));
    }

    private EventReference persistRecoveryMessage(Failure<EventReference> failure, PersistedDocument indexDocument) {
        RecoveryEntry.fromIdentifier(indexDocument.getBody().identifyExpandedEntry())
            .resourceType(indexDocument.getConsumptionAttributes().getIndex())
            .withException(failure.getException())
            .persist(queueClient);
        logger.error(SENT_TO_RECOVERY_QUEUE_MESSAGE, indexDocument.getConsumptionAttributes().getIndex());
        return null;
    }

    private URI writeEntryToS3(PersistedDocument indexDocument) throws IOException {
        var filePath = createFilePath(indexDocument);
        return s3Writer.insertFile(filePath, indexDocument.toJsonString());
    }

    private ExpandedDataEntry readEvent(EventReference input) {
        String data = s3Reader.readEvent(input.getUri());
        return attempt(() -> PublicationEventsConfig.objectMapper.readValue(data, ExpandedDataEntry.class))
                   .orElseThrow();
    }

    private UnixPath createFilePath(PersistedDocument indexDocument) {
        return UnixPath.of(createPathBasedOnIndexName(indexDocument))
                   .addChild(indexDocument.getConsumptionAttributes().getDocumentIdentifier().toString() + GZIP_ENDING);
    }

    private String createPathBasedOnIndexName(PersistedDocument indexDocument) {
        return indexDocument.getConsumptionAttributes().getIndex();
    }
}

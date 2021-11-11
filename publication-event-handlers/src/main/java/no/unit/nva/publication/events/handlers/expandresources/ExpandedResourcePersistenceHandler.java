package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.ENVIRONMENT;
import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocument.createIndexDocument;
import static no.unit.nva.s3.S3Driver.GZIP_ENDING;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.expansion.model.ExpandedDatabaseEntry;
import no.unit.nva.publication.events.bodies.EventPayload;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandedResourcePersistenceHandler
    extends DestinationsEventBridgeEventHandler<EventPayload, EventPayload> {

    public static final String PERSISTED_ENTRIES_BUCKET = ENVIRONMENT.readEnv("PERSISTED_ENTRIES_BUCKET");
    private final S3Driver s3Reader;
    private final S3Driver s3Writer;
    private static final Logger logger = LoggerFactory.getLogger(ExpandedResourcePersistenceHandler.class);

    @JacocoGenerated
    public ExpandedResourcePersistenceHandler() {
        this(new S3Driver(PublicationEventsConfig.EVENTS_BUCKET), new S3Driver(PERSISTED_ENTRIES_BUCKET));
    }

    public ExpandedResourcePersistenceHandler(S3Driver s3Reader, S3Driver s3Writer) {
        super(EventPayload.class);
        this.s3Reader = s3Reader;
        this.s3Writer = s3Writer;
    }

    @Override
    protected EventPayload processInputPayload(EventPayload input,
                                               AwsEventBridgeEvent<AwsEventBridgeDetail<EventPayload>> event,
                                               Context context) {
        ExpandedDatabaseEntry expandedResourceUpdate = readEvent(input);
        var indexDocument = createIndexDocument(expandedResourceUpdate);
        var uri = writeEntryToS3(indexDocument);
        var outputEvent = EventPayload.indexEntryEvent(uri);
        logger.info(attempt(() -> objectMapper.writeValueAsString(outputEvent)).orElseThrow());
        return outputEvent;
    }

    private URI writeEntryToS3(PersistedDocument indexDocument) {
        var filePath = createFilePath(indexDocument);
        return attempt(() -> s3Writer.insertFile(filePath, indexDocument.toJsonString())).orElseThrow();
    }

    private ExpandedDatabaseEntry readEvent(EventPayload input) {
        String data = s3Reader.readEvent(input.getPayloadUri());
        return attempt(() -> PublicationEventsConfig.objectMapper.readValue(data, ExpandedDatabaseEntry.class))
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

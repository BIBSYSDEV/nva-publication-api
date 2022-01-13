package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocument.createIndexDocument;
import static no.unit.nva.publication.events.handlers.persistence.PersistenceConfig.PERSISTED_ENTRIES_BUCKET;
import static no.unit.nva.s3.S3Driver.GZIP_ENDING;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandedDataEntriesPersistenceHandler
    extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC = "PublicationService.ExpandedEntry.Persisted";

    private final S3Driver s3Reader;
    private final S3Driver s3Writer;
    private static final Logger logger = LoggerFactory.getLogger(ExpandedDataEntriesPersistenceHandler.class);

    @JacocoGenerated
    public ExpandedDataEntriesPersistenceHandler() {
        this(new S3Driver(PublicationEventsConfig.EVENTS_BUCKET), new S3Driver(PERSISTED_ENTRIES_BUCKET));
    }

    public ExpandedDataEntriesPersistenceHandler(S3Driver s3Reader, S3Driver s3Writer) {
        super(EventReference.class);
        this.s3Reader = s3Reader;
        this.s3Writer = s3Writer;
    }

    @Override
    protected EventReference processInputPayload(
        EventReference input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
        Context context) {
        ExpandedDataEntry expandedResourceUpdate = readEvent(input);
        var indexDocument = createIndexDocument(expandedResourceUpdate);
        var uri = writeEntryToS3(indexDocument);
        var outputEvent = new EventReference(EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC, uri);
        logger.info(outputEvent.toJsonString());
        return outputEvent;
    }

    private URI writeEntryToS3(PersistedDocument indexDocument) {
        var filePath = createFilePath(indexDocument);
        return attempt(() -> s3Writer.insertFile(filePath, indexDocument.toJsonString())).orElseThrow();
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

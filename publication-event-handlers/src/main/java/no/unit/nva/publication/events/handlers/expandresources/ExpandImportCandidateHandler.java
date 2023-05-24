package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.persistence.PersistedDocument.createIndexDocument;
import static no.unit.nva.publication.events.handlers.persistence.PersistenceConfig.PERSISTED_ENTRIES_BUCKET;
import static no.unit.nva.s3.S3Driver.GZIP_ENDING;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.events.handlers.persistence.PersistedDocument;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandImportCandidateHandler extends
                                          DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String IMPORT_CANDIDATE_PERSISTENCE = "ImportCandidates.ExpandedDataEntry.Persisted";
    public static final String EVENTS_BUCKET = "EVENTS_BUCKET";
    public static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    private final Logger logger = LoggerFactory.getLogger(ExpandImportCandidateHandler.class);
    private final S3Driver s3Reader;
    private final S3Driver s3Writer;

    @JacocoGenerated
    public ExpandImportCandidateHandler() {
        this(new S3Driver(EVENTS_BUCKET), new S3Driver(PERSISTED_ENTRIES_BUCKET));
    }

    public ExpandImportCandidateHandler(S3Driver s3Driver, S3Driver s3Writer) {
        super(EventReference.class);
        this.s3Reader = s3Driver;
        this.s3Writer = s3Writer;
    }

    @Override
    protected EventReference processInputPayload(EventReference input,
                                                 AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                 Context context) {
        var blob = readBlobFromS3(input);
        return attempt(() -> ExpandedImportCandidate.fromImportCandidate(blob.getNewData()))
                   .map(this::createOutPutEventAndPersistDocument)
                   .orElse(failure -> emptyEvent());
    }

    private EventReference emptyEvent() {
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }

    private ImportCandidateDataEntryUpdate readBlobFromS3(EventReference input) {
        logger.info("Event to proceed: {}", input.getUri());
        var blobString = s3Reader.readEvent(input.getUri());
        return ImportCandidateDataEntryUpdate.fromJson(blobString);
    }

    private EventReference createOutPutEventAndPersistDocument(ExpandedImportCandidate expandedImportCandidate) {
        var indexDocument = createIndexDocument(expandedImportCandidate);
        var uri = writeEntryToS3(indexDocument);
        var outputEvent = new EventReference(IMPORT_CANDIDATE_PERSISTENCE, uri);
        logger.info(outputEvent.toJsonString());
        return outputEvent;
    }

    private URI writeEntryToS3(PersistedDocument indexDocument) {
        var filePath = createFilePath(indexDocument);
        return attempt(() -> s3Writer.insertFile(filePath, indexDocument.toJsonString())).orElseThrow();
    }

    private UnixPath createFilePath(PersistedDocument indexDocument) {
        return UnixPath.of(createPathBasedOnIndexName(indexDocument))
                   .addChild(indexDocument.getConsumptionAttributes().getDocumentIdentifier().toString() + GZIP_ENDING);
    }

    private String createPathBasedOnIndexName(PersistedDocument indexDocument) {
        return indexDocument.getConsumptionAttributes().getIndex();
    }
}

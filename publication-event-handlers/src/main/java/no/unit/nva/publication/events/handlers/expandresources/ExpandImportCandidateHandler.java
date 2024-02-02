package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler.EMPTY_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.persistence.PersistedDocument.createIndexDocument;
import static no.unit.nva.s3.S3Driver.GZIP_ENDING;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.events.handlers.persistence.PersistedDocument;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandImportCandidateHandler extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String IMPORT_CANDIDATE_PERSISTENCE = "ImportCandidates.ExpandedDataEntry.Persisted";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String EVENTS_BUCKET = ENVIRONMENT.readEnv("EVENTS_BUCKET");
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String PERSISTED_ENTRIES_BUCKET = ENVIRONMENT.readEnv("PERSISTED_ENTRIES_BUCKET");
    public static final int PUBLICATION_YEAR_2018 = 2018;
    public static final String EMPTY_EVENT_MESSAGE = "Candidate should not be expanded because of publication year: {}";
    public static final String EXPANSION_ERROR_MESSAGE = "Something went wrong expanding import candidate: {}";
    public static final String EXPANSION_MESSAGE = "Import candidate with identifier has been expanded: {}";
    private final Logger logger = LoggerFactory.getLogger(ExpandImportCandidateHandler.class);
    private final S3Driver s3Reader;
    private final S3Driver s3Writer;
    private final RawContentRetriever uriRetriever;

    @JacocoGenerated
    public ExpandImportCandidateHandler() {
        this(new S3Driver(EVENTS_BUCKET), new S3Driver(PERSISTED_ENTRIES_BUCKET),
             new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME));
    }

    public ExpandImportCandidateHandler(S3Driver s3Reader, S3Driver s3Writer, RawContentRetriever uriRetriever) {
        super(EventReference.class);
        this.s3Reader = s3Reader;
        this.s3Writer = s3Writer;
        this.uriRetriever = uriRetriever;
    }

    @Override
    protected EventReference processInputPayload(EventReference input,
                                                 AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                 Context context) {
        var blob = readBlobFromS3(input);
        return attempt(() -> ExpandedImportCandidate.fromImportCandidate(blob.getNewData(), uriRetriever))
                   .map(expandedImportCandidate -> shouldBeExpanded(expandedImportCandidate)
                                                       ? createOutPutEventAndPersistDocument(expandedImportCandidate)
                                                       : emptyEvent(expandedImportCandidate))
                   .orElse(this::emptyEvent);
    }

    private boolean shouldBeExpanded(ExpandedImportCandidate expandedImportCandidate) {
        return Integer.parseInt(expandedImportCandidate.getPublicationYear()) >= PUBLICATION_YEAR_2018;
    }

    private EventReference emptyEvent(ExpandedImportCandidate expandedImportCandidate) {
        logger.info(EMPTY_EVENT_MESSAGE, expandedImportCandidate.getPublicationYear()
                                         + StringUtils.WHITESPACES
                                         + expandedImportCandidate.getIdentifier());
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }

    private EventReference emptyEvent(Failure<EventReference> failure) {
        logger.error(EXPANSION_ERROR_MESSAGE, failure.getException().getMessage());
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }

    private ImportCandidateDataEntryUpdate readBlobFromS3(EventReference input) {
        var blobString = s3Reader.readEvent(input.getUri());
        return ImportCandidateDataEntryUpdate.fromJson(blobString);
    }

    private EventReference createOutPutEventAndPersistDocument(ExpandedImportCandidate expandedImportCandidate) {
        var indexDocument = createIndexDocument(expandedImportCandidate);
        var uri = writeEntryToS3(indexDocument);
        var outputEvent = new EventReference(IMPORT_CANDIDATE_PERSISTENCE, uri);
        logger.info(EXPANSION_MESSAGE, expandedImportCandidate.getIdentifier());
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

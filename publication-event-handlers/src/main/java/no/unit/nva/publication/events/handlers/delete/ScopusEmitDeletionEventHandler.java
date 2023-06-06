package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultEventBridgeClient;
import static no.unit.nva.publication.s3imports.DeleteEntriesEventEmitter.NON_EMITTED_FILENAMES_WARNING_PREFIX;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.LINE_SEPARATOR;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.publication.events.bodies.ImportCandidateDeletion;
import no.unit.nva.publication.s3imports.BatchEventEmitter;
import no.unit.nva.publication.s3imports.PutEventsResult;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusEmitDeletionEventHandler implements RequestHandler<S3Event, Void> {

    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    public static final String SCOPUS_IDENTIFIER_DELIMITER = "DELETE-";
    public static final int NUMBER_OF_EMITTED_ENTRIES_PER_BATCH = 10;
    public static final String EVENTS_BUCKET = new Environment().readEnv("EVENTS_BUCKET");
    private static final int SINGLE_EXPECTED_FILE = 0;
    private static final Logger logger = LoggerFactory.getLogger(ScopusEmitDeletionEventHandler.class);
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public ScopusEmitDeletionEventHandler() {
        this(S3Driver.defaultS3Client().build(), defaultEventBridgeClient());
    }

    public ScopusEmitDeletionEventHandler(S3Client s3Client, EventBridgeClient eventBridgeClient) {
        super();
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    public Void handleRequest(S3Event input, Context context) {
        Stream.of(readFile(input))
            .map(this::extractIdentifiers)
            .map(this::createEvents)
            .map(scopusDeletionEventList -> emitEvents(scopusDeletionEventList, context))
            .forEach(this::logWarningForNotEmittedFilenames);

        return null;
    }

    private static List<String> splitOnNewLine(String string) {
        return Arrays.asList(string.split("\n"));
    }

    private static boolean isEmptyLine(String string) {
        return !string.isBlank() || !string.isEmpty();
    }

    private static ImportCandidateDeletion createEvent(String id) {
        return new ImportCandidateDeletion(ImportCandidateDeletion.EVENT_TOPIC, id);
    }

    private List<ImportCandidateDeletion> createEvents(Stream<String> scopusIdentifiers) {
        return scopusIdentifiers
                   .map(ScopusEmitDeletionEventHandler::createEvent)
                   .collect(Collectors.toList());
    }

    private List<PutEventsResult> emitEvents(List<ImportCandidateDeletion> events, Context context) {
        var batchEventEmitter = new BatchEventEmitter<ImportCandidateDeletion>
                                    (ImportCandidateDeletion.class.getCanonicalName(),
                                     context.getInvokedFunctionArn(),
                                     eventBridgeClient);
        batchEventEmitter.addEvents(events);
        return batchEventEmitter.emitEvents(NUMBER_OF_EMITTED_ENTRIES_PER_BATCH);
    }

    private Stream<String> extractIdentifiers(String string) {
        return splitOnNewLine(string).stream()
                   .filter(ScopusEmitDeletionEventHandler::isEmptyLine)
                   .map(this::extractScopusIdentifier);
    }

    private String extractScopusIdentifier(String item) {

        return item.split(SCOPUS_IDENTIFIER_DELIMITER)[1];
    }

    private String readFile(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath());
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_FILE).getS3().getBucket().getName();
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractFilename(s3Event)));
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_FILE).getS3().getObject().getKey();
    }

    private void logWarningForNotEmittedFilenames(List<PutEventsResult> failedRequests) {
        if (!failedRequests.isEmpty()) {
            String failedRequestsString = failedRequests
                                              .stream()
                                              .map(PutEventsResult::toString)
                                              .collect(Collectors.joining(LINE_SEPARATOR));
            logger.warn(NON_EMITTED_FILENAMES_WARNING_PREFIX, failedRequestsString);
        }
    }
}

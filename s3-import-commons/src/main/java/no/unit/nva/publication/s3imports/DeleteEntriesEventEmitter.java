package no.unit.nva.publication.s3imports;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultEventBridgeClient;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.events.DeleteEntryEvent;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteEntriesEventEmitter implements RequestStreamHandler {

    public static final String EXPECTED_BODY_MESSAGE =
        "The expected json body contains only an s3Location.\nThe received body was: ";

    public static final int NUMBER_OF_EMITTED_ENTRIES_PER_BATCH = 10;
    public static final String NON_EMITTED_FILENAMES_WARNING_PREFIX = "Some files failed to be emitted:{}";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String CANONICAL_NAME = DeleteEntriesEventEmitter.class.getCanonicalName();
    private static final Logger logger = LoggerFactory.getLogger(DeleteEntriesEventEmitter.class);
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public DeleteEntriesEventEmitter() {
        this(defaultS3Client(), defaultEventBridgeClient());
    }

    public DeleteEntriesEventEmitter(S3Client s3Client, EventBridgeClient eventBridgeClient) {
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    /**
     * Method for emitting DeleteEntryEvents to DeletePublicationHandler
     *
     * @param input   EventReference containing S3 uri containing files with object keys corresponding to
     *                publication-identifier. Ex. Brage-migration report bucket and cristin-import bucket contains such
     *                objects.
     * @param output  not used
     * @param context used for obtaining invokingFunctionArn
     */

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        var importRequest = parseInput(input);
        var files = listFilesInBucket(importRequest);
        var deleteEntryEvents = createDeleteEvents(files);
        var failedEntries =
            attempt(() -> emitEventReferences(context, deleteEntryEvents)).orElseThrow();
        logWarningForNotEmittedFilenames(failedEntries);
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

    private List<DeleteEntryEvent> createDeleteEvents(List<UnixPath> files) {
        return files.stream()
                   .map(this::convertToIdentifier)
                   .map(sortableIdentifier -> new DeleteEntryEvent(DeleteEntryEvent.EVENT_TOPIC,
                                                                   sortableIdentifier))
                   .collect(Collectors.toList());
    }

    private List<PutEventsResult> emitEventReferences(Context context, List<DeleteEntryEvent> deleteEntryEvents) {
        var batchEventEmitter =
            new BatchEventEmitter<DeleteEntryEvent>(CANONICAL_NAME,
                                                    context.getInvokedFunctionArn(),
                                                    eventBridgeClient);
        batchEventEmitter.addEvents(deleteEntryEvents);
        return batchEventEmitter.emitEvents(NUMBER_OF_EMITTED_ENTRIES_PER_BATCH);
    }

    private SortableIdentifier convertToIdentifier(UnixPath unixpath) {
        return new SortableIdentifier(unixpath.getLastPathElement());
    }

    private EventReference parseInput(InputStream input) {
        String inputString = IoUtils.streamToString(input);
        return attempt(() -> EventReference.fromJson(inputString))
                   .toOptional()
                   .filter(event -> nonNull(event.getUri()))
                   .orElseThrow(() -> new IllegalArgumentException(EXPECTED_BODY_MESSAGE + inputString));
    }

    private List<UnixPath> listFilesInBucket(EventReference importRequest) {
        S3Driver s3Driver = new S3Driver(s3Client, importRequest.extractBucketName());
        return s3Driver.listAllFiles(importRequest.getUri());
    }
}

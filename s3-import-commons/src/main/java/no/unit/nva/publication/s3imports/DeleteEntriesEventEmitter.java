package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultEventBridgeClient;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.events.DeleteEntryEvent;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteEntriesEventEmitter extends EventHandler<EventReference, String> {

    public static final int NUMBER_OF_EMITTED_ENTRIES_PER_BATCH = 100;

    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static final String CANONICAL_NAME = FileEntriesEventEmitter.class.getCanonicalName();

    private static final String NON_EMITTED_ENTRIES_WARNING_PREFIX = "Some entries failed to be emitted: ";

    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;

    private static final Logger logger = LoggerFactory.getLogger(DeleteEntriesEventEmitter.class);

    @JacocoGenerated
    public DeleteEntriesEventEmitter() {
        this(defaultS3Client(), defaultEventBridgeClient());
    }

    public DeleteEntriesEventEmitter(S3Client s3Client, EventBridgeClient eventBridgeClient) {
        super(EventReference.class);
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    /**
     * Method for emitting DeleteEntryEvents to DeletePublicationHandler
     * @param input EventReference containing S3 uri containing files with object keys corresponding to
     *              publication-identifier. Ex. Brage-migration report bucket and cristin-import bucket contains such
     *              objects.
     * @param _event not used
     * @param context used for obtaining invokingFunctionArn
     * @return null
     */

    protected String processInput(EventReference input, AwsEventBridgeEvent<EventReference> _event, Context context) {
        var files = listFilesInBucket(input);
        var deleteEntryEvents = createDeleteEvents(files);
        var failedEntries =
            attempt(() -> emitEventReferences(context, deleteEntryEvents));
        if (thereAreFailures(failedEntries)) {
            logWarningForNotEmittedEntries(failedEntries);
        }
        return null;
    }

    private boolean thereAreFailures(Try<List<PutEventsResult>> failedEntries) {
        return completeEmissionFailure(failedEntries) || partialEmissionFailure(failedEntries);
    }

    private boolean completeEmissionFailure(Try<List<PutEventsResult>> failedEntries) {
        return failedEntries.isFailure();
    }

    private boolean partialEmissionFailure(Try<List<PutEventsResult>> failedEntries) {
        return failedEntries.isSuccess() && !failedEntries.orElseThrow().isEmpty();
    }


    private void logWarningForNotEmittedEntries(Try<List<PutEventsResult>> failedRequests) {
        String failedRequestsString = failedRequests
                                          .stream()
                                          .map(PutEventsResult::toString)
                                          .collect(Collectors.joining(LINE_SEPARATOR));
        if (StringUtils.isNotBlank(failedRequestsString)) {
            logger.warn(NON_EMITTED_ENTRIES_WARNING_PREFIX + failedRequestsString);
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

    private List<UnixPath> listFilesInBucket(EventReference importRequest) {
        S3Driver s3Driver = new S3Driver(s3Client, importRequest.extractBucketName());
        return s3Driver.listAllFiles(importRequest.getUri());
    }
}

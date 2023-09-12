package no.unit.nva.publication.s3imports;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteImportCandidatesEventEmitter implements RequestHandler<S3Event, Void> {

    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    public static final String SCOPUS_IDENTIFIER_DELIMITER = "DELETE-";
    public static final int NUMBER_OF_EMITTED_ENTRIES_PER_BATCH = 10;
    public static final String NOT_EMITTED_EVENTS = "Not emitted events {}:";
    private static final int SINGLE_EXPECTED_FILE = 0;
    private static final Logger logger = LoggerFactory.getLogger(DeleteImportCandidatesEventEmitter.class);
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public DeleteImportCandidatesEventEmitter() {
        this(S3Driver.defaultS3Client().build(), defaultEventBridgeClient());
    }

    public DeleteImportCandidatesEventEmitter(S3Client s3Client, EventBridgeClient eventBridgeClient) {
        super();
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    @JacocoGenerated
    public static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
                   .region(ApplicationConstants.AWS_REGION)
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    @Override
    public Void handleRequest(S3Event input, Context context) {
        var events = extractIdentifiersFromEventBlob(input)
                    .map(this::createEvent)
                    .toList();
        var putEventsResults = emitEvents(events, context);
        logWarningForNotEmittedEvents(putEventsResults);
        return null;
    }

    private static boolean isNotEmptyLine(String string) {
        return !string.isBlank() || !string.isEmpty();
    }

    private ImportCandidateDeleteEvent createEvent(String id) {
        return new ImportCandidateDeleteEvent(ImportCandidateDeleteEvent.EVENT_TOPIC, id);
    }

    private List<PutEventsResult> emitEvents(List<ImportCandidateDeleteEvent> events, Context context) {
        var batchEventEmitter = getBatchEventEmitter(context);
        logger.info("Events to emit: {}", events.stream().map(ImportCandidateDeleteEvent::toJsonString).toList());
        batchEventEmitter.addEvents(events);
        return batchEventEmitter.emitEvents(NUMBER_OF_EMITTED_ENTRIES_PER_BATCH);
    }

    private BatchEventEmitter<ImportCandidateDeleteEvent> getBatchEventEmitter(Context context) {
        return new BatchEventEmitter<>(ImportCandidateDeleteEvent.class.getCanonicalName(),
                                       context.getInvokedFunctionArn(),
                                       eventBridgeClient);
    }

    private String extractScopusIdentifier(String item) {
        return item.split(SCOPUS_IDENTIFIER_DELIMITER)[1];
    }

    private Stream<String> extractIdentifiersFromEventBlob(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath())
                   .lines()
                   .filter(DeleteImportCandidatesEventEmitter::isNotEmptyLine)
                   .map(this::extractScopusIdentifier);
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

    private void logWarningForNotEmittedEvents(List<PutEventsResult> failedRequests) {
        if (!failedRequests.isEmpty()) {
            String failedRequestsString = failedRequests
                                              .stream()
                                              .map(PutEventsResult::toString)
                                              .collect(Collectors.joining(System.lineSeparator()));
            logger.warn(NOT_EMITTED_EVENTS, failedRequestsString);
        }
    }
}

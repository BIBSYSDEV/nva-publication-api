package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.publication.events.bodies.ImportCandidateDeleteEvent.EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.ConfigurationForPushingDirectlyToEventBridge.EVENT_BUS_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.publication.events.bodies.ImportCandidateDeleteEvent;
import no.unit.nva.publication.s3imports.ApplicationConstants;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteImportCandidatesEventEmitter implements RequestHandler<S3Event, Void> {

    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    public static final String SCOPUS_IDENTIFIER_DELIMITER = "DELETE-";
    //    public static final int NUMBER_OF_EMITTED_ENTRIES_PER_BATCH = 10;
    //    public static final String NOT_EMITTED_EVENTS = "Not emitted events {}:";
    private static final int SINGLE_EXPECTED_FILE = 0;
    //    private static final Logger logger = LoggerFactory.getLogger(DeleteImportCandidatesEventEmitter.class);
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public DeleteImportCandidatesEventEmitter() {
        this(S3Driver.defaultS3Client().build(), ApplicationConstants.defaultEventBridgeClient());
    }

    public DeleteImportCandidatesEventEmitter(S3Client s3Client, EventBridgeClient eventBridgeClient) {
        super();
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    public Void handleRequest(S3Event input, Context context) {
        Stream.of(readFile(input))
            .map(this::extractIdentifiers)
            .map(this::createEvents)
            .map(list -> toPutRequestEventEntries(list, context))
            .forEach(this::emitEvents);
        return null;
    }

    private static List<String> splitOnNewLine(String string) {
        return Arrays.asList(string.split("\n"));
    }

    private static boolean isEmptyLine(String string) {
        return !string.isBlank() || !string.isEmpty();
    }

    private void emitEvents(List<PutEventsRequestEntry> requests) {
        attempt(() -> eventBridgeClient.putEvents(PutEventsRequest.builder().entries(requests).build()))
            .orElseThrow();
    }

    private List<PutEventsRequestEntry> toPutRequestEventEntries(
        List<ImportCandidateDeleteEvent> importCandidateDeleteEvent,
        Context context) {
        return importCandidateDeleteEvent.stream().map(detail -> PutEventsRequestEntry.builder()
                                                                     .detailType(EVENT_TOPIC)
                                                                     .time(Instant.now())
                                                                     .eventBusName(EVENT_BUS_NAME)
                                                                     .detail(detail.toJsonString())
                                                                     .source(context.getFunctionName())
                                                                     .resources(context.getInvokedFunctionArn())
                                                                     .build()).collect(Collectors.toList());
    }

    private ImportCandidateDeleteEvent toEventDetail(String id) {
        return new ImportCandidateDeleteEvent(EVENT_TOPIC, id);
    }

    private List<ImportCandidateDeleteEvent> createEvents(Stream<String> scopusIdentifiers) {
        return scopusIdentifiers.map(this::toEventDetail)
                   .collect(Collectors.toList());
    }

    //    private List<PutEventsResult> emitEvents(List<ImportCandidateDeleteEvent> events, Context context) {
    //        var batchEventEmitter = new BatchEventEmitter<ImportCandidateDeleteEvent>(
    //            ImportCandidateDeleteEvent.class.getCanonicalName(),
    //            context.getInvokedFunctionArn(),
    //            eventBridgeClient);
    //        logger.info("Events to emit: {}",
    //                    events.stream().map(ImportCandidateDeleteEvent::toJsonString).collect(Collectors.toList()));
    //        batchEventEmitter.addEvents(events);
    //        return batchEventEmitter.emitEvents(NUMBER_OF_EMITTED_ENTRIES_PER_BATCH);
    //    }

    private Stream<String> extractIdentifiers(String string) {
        return splitOnNewLine(string).stream()
                   .filter(DeleteImportCandidatesEventEmitter::isEmptyLine)
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

    //    private void logWarningForNotEmittedEvents(List<PutEventsResult> failedRequests) {
    //        if (!failedRequests.isEmpty()) {
    //            String failedRequestsString = failedRequests
    //                                              .stream()
    //                                              .map(PutEventsResult::toString)
    //                                              .collect(Collectors.joining("\n"));
    //            logger.warn(NOT_EMITTED_EVENTS, failedRequestsString);
    //        }
    //    }
}

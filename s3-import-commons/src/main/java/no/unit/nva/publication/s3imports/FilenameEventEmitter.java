package no.unit.nva.publication.s3imports;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.s3imports.ApplicationConstants.ERRORS_FOLDER;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultClock;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultEventBridgeClient;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * {@link FilenameEventEmitter} accepts an {@link EventReference}, it lists all the files in the S3 location defined in
 * the {@link EventReference} and it emits on event per filename.
 *
 * <p>Each event has as event detail-type the value {@link FilenameEventEmitter#FILENAME_EMISSION_EVENT_TOPIC} and
 * detail (event-body) an {@link EventReference} where s3Location is the URI of the respective file and the rest of the
 * fields are copied from the input.
 */
public class FilenameEventEmitter implements RequestStreamHandler {
    
    public static final String FILENAME_EMISSION_EVENT_TOPIC = "PublicationService.DataImport.Filename";
    public static final String SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER =
        "PublicationService.CristinData.DataEntry";
    public static final String SUBTOPIC_SEND_EVENT_TO_CRISTIN_ENTRIES_PATCH_EVENT_CONSUMER =
        "PublicationService.CristinData.PatchEntry";
    public static final Set<String> SUPPORTED_SUBTOPICS =
        Set.of(SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER,
               SUBTOPIC_SEND_EVENT_TO_CRISTIN_ENTRIES_PATCH_EVENT_CONSUMER);
    
    public static final String EXPECTED_BODY_MESSAGE =
        "The expected json body contains only an s3Location.\nThe received body was: ";
    public static final String WRONG_OR_EMPTY_S3_LOCATION_ERROR = "S3 location does not exist or is empty:";

    public static final String INFORM_USER_THAT_SUBTYPE_IS_INVALID_TEMPLATE =
        "Invalid subtopic supplied: %s, , only these are valid: %s";

    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String NON_EMITTED_FILENAMES_WARNING_PREFIX = "Some files failed to be emitted:{}";
    public static final String RUNNING_CLASS_NAME = FilenameEventEmitter.class.getCanonicalName();
    public static final String ERROR_REPORT_FILENAME = Instant.now().toString() + "emitFilenamesReport.error.";
    public static final int NUMBER_OF_EMITTED_FILENAMES_PER_BATCH = 10;
    private static final Logger logger = LoggerFactory.getLogger(FilenameEventEmitter.class);
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;
    private final Clock clock;
    private Instant commonTimestampForAllEmittedEventsIndicatingTheBeginningOfTheImport;
    
    @JacocoGenerated
    public FilenameEventEmitter() {
        this(defaultS3Client(), defaultEventBridgeClient(), defaultClock());
    }
    
    public FilenameEventEmitter(S3Client s3Client, EventBridgeClient eventBridgeClient, Clock clock) {
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
        this.clock = clock;
    }
    
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        commonTimestampForAllEmittedEventsIndicatingTheBeginningOfTheImport = clock.instant();
        var importRequest = parseInput(input);
        var files = listFiles(importRequest);
        validateImportRequest(importRequest, files);
        var failedRequests = emitEvents(context, files, importRequest);
        logWarningForNotEmittedFilenames(failedRequests);
        List<URI> notEmittedFilenames = collectNotEmittedFilenames(failedRequests);
        writeFailedEmitActionsInS3(failedRequests, importRequest);
        writeOutput(output, notEmittedFilenames);
    }
    
    private void writeFailedEmitActionsInS3(List<PutEventsResult> failedRequests, EventReference request)
        throws IOException {
        UriWrapper errorReportUri = createErrorReportUri(request);
        S3Driver s3Driver = new S3Driver(s3Client, request.extractBucketName());
        String errorReportContent = PutEventsResult.toString(failedRequests);
        if (!failedRequests.isEmpty()) {
            s3Driver.insertFile(errorReportUri.toS3bucketPath(), errorReportContent);
        }
    }
    
    private UriWrapper createErrorReportUri(EventReference request) {
        UriWrapper inputFolderUri = UriWrapper.fromUri(request.getUri());
        UriWrapper bucketUri = inputFolderUri.getHost();
        return bucketUri
                   .addChild(ERRORS_FOLDER)
                   .addChild(timestampToString(commonTimestampForAllEmittedEventsIndicatingTheBeginningOfTheImport))
                   .addChild(inputFolderUri.getPath())
                   .addChild(ERROR_REPORT_FILENAME);
    }
    
    private URI createUri(URI s3Location, UnixPath filename) {
        return Try.of(s3Location)
                   .map(UriWrapper::fromUri)
                   .map(UriWrapper::getHost)
                   .map(uri -> uri.addChild(filename))
                   .map(UriWrapper::getUri)
                   .orElseThrow();
    }
    
    private List<URI> listFiles(EventReference importRequest) {
        
        S3Driver s3Driver = new S3Driver(s3Client, importRequest.extractBucketName());
        
        List<UnixPath> filenames = s3Driver.listAllFiles(importRequest.getUri());
        logger.info(attempt(() -> s3ImportsMapper.writeValueAsString(filenames)).orElseThrow());
        return filenames.stream()
                   .map(filename -> createUri(importRequest.getUri(), filename))
                   .collect(Collectors.toList());
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
    
    private List<URI> collectNotEmittedFilenames(List<PutEventsResult> failedRequests) {
        return failedRequests.stream()
                   .map(PutEventsResult::getRequest)
                   .map(PutEventsRequest::entries)
                   .flatMap(Collection::stream)
                   .map(PutEventsRequestEntry::detail)
                   .map(EventReference::fromJson)
                   .map(EventReference::getUri)
                   .collect(Collectors.toList());
    }

    private List<PutEventsResult> emitEvents(Context context, List<URI> files, EventReference importRequest) {

        var filenameEvents = files.stream()
                                 .map(file -> newImportRequestForSingleFile(file, importRequest))
                                 .collect(Collectors.toList());

        return returnEmitAllEvents(context, filenameEvents);
    }
    
    private List<PutEventsResult> returnEmitAllEvents(Context context, List<EventReference> filenameEvents) {
        var batchEventEmitter = new BatchEventEmitter(RUNNING_CLASS_NAME,
                                                      context.getInvokedFunctionArn(), eventBridgeClient);

        batchEventEmitter.addEvents(filenameEvents);
        return batchEventEmitter.emitEvents(NUMBER_OF_EMITTED_FILENAMES_PER_BATCH);
    }

    private EventReference newImportRequestForSingleFile(URI uri, EventReference importRequest) {
        return new EventReference(FILENAME_EMISSION_EVENT_TOPIC, importRequest.getSubtopic(), uri,
                                  commonTimestampForAllEmittedEventsIndicatingTheBeginningOfTheImport);
    }
    
    private void validateImportRequest(EventReference importRequest, List<URI> files) {
        if (isNull(files) || files.isEmpty()) {
            throw new IllegalArgumentException(WRONG_OR_EMPTY_S3_LOCATION_ERROR + importRequest.getUri());
        }
        if (isNull(importRequest.getSubtopic()) || !isSupportedSubtopic(importRequest)) {
            throw new IllegalArgumentException(String.format(INFORM_USER_THAT_SUBTYPE_IS_INVALID_TEMPLATE,
                                                             importRequest.getSubtopic(), SUPPORTED_SUBTOPICS));
        }
    }

    private boolean isSupportedSubtopic(EventReference importRequest) {
        return SUPPORTED_SUBTOPICS.contains(importRequest.getSubtopic());
    }

    private EventReference parseInput(InputStream input) {
        String inputString = IoUtils.streamToString(input);
        return attempt(() -> EventReference.fromJson(inputString))
                   .toOptional()
                   .filter(event -> nonNull(event.getUri()))
                   .orElseThrow(() -> new IllegalArgumentException(EXPECTED_BODY_MESSAGE + inputString));
    }
    
    private <T> void writeOutput(OutputStream output, List<T> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            writer.write(toJson(results));
        }
    }
    
    private <T> String toJson(T results) throws JsonProcessingException {
        return s3ImportsMapper.writeValueAsString(results);
    }
}

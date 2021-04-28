package no.unit.nva.cristin.lambda;

import static java.util.Objects.isNull;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

public class FilenameEventEmitter implements RequestStreamHandler {

    public static final String WRONG_OR_EMPTY_S3_LOCATION_ERROR = "S3 location does not exist or is empty:";
    public static final String IMPORT_CRISTIN_FILENAME_EVENT = "import.cristin.filename-event";
    public static final String LINE_SEPARATOR = System.lineSeparator();
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;
    private static final Logger logger = LoggerFactory.getLogger(FilenameEventEmitter.class);

    public FilenameEventEmitter(S3Client s3Client, EventBridgeClient eventBridgeClient) {
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        ImportRequest importRequest = parseInput(input);
        S3Driver s3Driver = new S3Driver(s3Client, importRequest.extractBucketFromS3Location());
        List<String> files = s3Driver.listFiles(Path.of(importRequest.extractPathFromS3Location()));
        validateLocation(importRequest, files);

        List<FilenameEvent> filenameEvents = files.stream().map(FilenameEvent::new).collect(Collectors.toList());

        List<PutEventsResult> failedRequests = emitEvents(context, filenameEvents);
        String failedRequestsString = failedRequests.stream()
                                          .map(PutEventsResult::toString)
                                          .collect(Collectors.joining(LINE_SEPARATOR));
        List<String> notEmittedFilenames = collecteNotEmittedFilenames(failedRequests);
        logger.warn("Some files failed to be emitted:" + failedRequestsString);
        writeOutput(output, notEmittedFilenames);
    }

    private List<String> collecteNotEmittedFilenames(List<PutEventsResult> failedRequests) {
        return failedRequests.stream()
                   .map(PutEventsResult::getRequest)
                   .map(PutEventsRequest::entries)
                   .flatMap(Collection::stream)
                   .map(PutEventsRequestEntry::detail)
                   .map(FilenameEvent::fromJson)
                   .map(FilenameEvent::getFileUri)
                   .map(URI::toString)
                   .collect(Collectors.toList());
    }

    private List<PutEventsResult> emitEvents(Context context, List<FilenameEvent> filenameEvents) {
        EventEmitter<FilenameEvent> eventEmitter =
            new EventEmitter<>(IMPORT_CRISTIN_FILENAME_EVENT, context.getInvokedFunctionArn(), eventBridgeClient);

        eventEmitter.addEvents(filenameEvents);
        return eventEmitter.emitEvents();
    }

    private void validateLocation(ImportRequest importRequest, List<String> files) {
        if (isNull(files) || files.isEmpty()) {
            throw new IllegalArgumentException(WRONG_OR_EMPTY_S3_LOCATION_ERROR + importRequest.getS3Location());
        }
    }

    private ImportRequest parseInput(InputStream input) {
        return ImportRequest.fromJson(IoUtils.streamToString(input));
    }

    private <T> void writeOutput(OutputStream output, List<T> results) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            writer.write(toJson(results));
        }
    }

    private <T> String toJson(T results) throws com.fasterxml.jackson.core.JsonProcessingException {
        return JsonUtils.objectMapper.writeValueAsString(results);
    }
}

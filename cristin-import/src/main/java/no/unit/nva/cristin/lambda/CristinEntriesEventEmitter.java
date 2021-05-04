package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.lambda.ApplicationConstants.EMPTY_STRING;
import static no.unit.nva.cristin.lambda.ApplicationConstants.defaultEventBridgeClient;
import static no.unit.nva.cristin.lambda.ApplicationConstants.defaultS3Client;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class CristinEntriesEventEmitter extends EventHandler<ImportRequest, String> {

    public static final String EVENT_DETAIL_TYPE = "import.cristin.entry-event";
    public static final String WRONG_DETAIL_TYPE_ERROR = "event does not contain the correct detail-type:";
    public static final String FILE_NOT_FOUND_ERROR = "File not found: ";
    private static final String CANONICAL_NAME = CristinEntriesEventEmitter.class.getCanonicalName();
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final boolean SEQUENTIAL = false;
    private static final Logger logger = LoggerFactory.getLogger(CristinEntriesEventEmitter.class);
    private static final String NON_EMITTED_ENTRIES_WARNING_PREFIX = "Some entries failed to be emitted: ";
    private static final String CONSECUTIVE_JSON_OBJECTS = "}\\s*\n\\s*\\{";
    private static final String NODES_IN_ARRAY = "},{";
    private static final Object END_OF_ARRAY = "]";
    private static final String BEGINNING_OF_ARRAY = "[";
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public CristinEntriesEventEmitter() {
        this(defaultS3Client(), defaultEventBridgeClient());
    }

    public CristinEntriesEventEmitter(S3Client s3Client,
                                      EventBridgeClient eventBridgeClient) {
        super(ImportRequest.class);
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    protected String processInput(ImportRequest input, AwsEventBridgeEvent<ImportRequest> event, Context context) {
        validateEvent(event);
        S3Driver s3Driver = new S3Driver(s3Client, input.extractBucketFromS3Location());
        String content = fetchFileFromS3(input, s3Driver);
        List<JsonNode> contents = parseContents(content);
        Stream<FileContentsEvent<JsonNode>> eventBodies = generateEventBodies(input, contents);
        List<PutEventsResult> failedEntries = emitEvents(context, eventBodies);
        logWarningForNotEmittedEntries(failedEntries);
        return EMPTY_STRING;
    }

    private Stream<FileContentsEvent<JsonNode>> generateEventBodies(ImportRequest input, List<JsonNode> contents) {
        return contents.stream().map(json -> new FileContentsEvent<>(json, input));
    }

    private List<PutEventsResult> emitEvents(Context context, Stream<FileContentsEvent<JsonNode>> eventBodies) {
        EventEmitter<FileContentsEvent<JsonNode>> eventEmitter = new EventEmitter<>(EVENT_DETAIL_TYPE,
                                                                                    CANONICAL_NAME,
                                                                                    context.getInvokedFunctionArn(),
                                                                                    eventBridgeClient);
        eventEmitter.addEvents(eventBodies);
        return eventEmitter.emitEvents();
    }

    private String fetchFileFromS3(ImportRequest input, S3Driver s3Driver) {
        try {
            return s3Driver.getFile(input.extractPathFromS3Location());
        } catch (NoSuchKeyException exception) {
            throw new IllegalArgumentException(FILE_NOT_FOUND_ERROR + input.getS3Location(), exception);
        }
    }

    private void validateEvent(AwsEventBridgeEvent<ImportRequest> event) {
        if (!event.getDetailType().equalsIgnoreCase(CristinFilenameEventEmitter.EVENT_DETAIL_TYPE)) {
            throw new IllegalArgumentException(WRONG_DETAIL_TYPE_ERROR + event.getDetailType());
        }
    }

    private void logWarningForNotEmittedEntries(List<PutEventsResult> failedRequests) {
        String failedRequestsString = failedRequests
                                          .stream()
                                          .map(PutEventsResult::toString)
                                          .collect(Collectors.joining(LINE_SEPARATOR));
        if (StringUtils.isNotBlank(failedRequestsString)) {
            logger.warn(NON_EMITTED_ENTRIES_WARNING_PREFIX + failedRequestsString);
        }
    }

    private List<JsonNode> parseContents(String content) {
        Try<List<JsonNode>> result = attempt(() -> parseContentAsJsonArray(content));
        if (result.isFailure()) {
            result = attempt(() -> parseContentAsListOfJsonObjects(content));
        }
        return result.orElseThrow();
    }

    private List<JsonNode> parseContentAsListOfJsonObjects(String content) {

        return attempt(() -> content.replaceAll(CONSECUTIVE_JSON_OBJECTS, NODES_IN_ARRAY))
                   .map(jsonObjectStrings -> BEGINNING_OF_ARRAY + jsonObjectStrings + END_OF_ARRAY)
                   .map(jsonArrayString -> (ArrayNode) JsonUtils.objectMapper.readTree(jsonArrayString))
                   .map(array -> toStream(array).collect(Collectors.toList()))
                   .orElseThrow();
    }

    private Stream<JsonNode> toStream(ArrayNode root) {
        return StreamSupport
                   .stream(Spliterators.spliteratorUnknownSize(root.elements(), Spliterator.ORDERED), SEQUENTIAL);
    }

    private List<JsonNode> parseContentAsJsonArray(String content) throws JsonProcessingException {
        return toStream(parseAsArrayNode(content)).collect(Collectors.toList());
    }

    private ArrayNode parseAsArrayNode(String content) throws JsonProcessingException {
        JsonNode jsonNode = JsonUtils.objectMapperNoEmpty.readTree(content);
        if (jsonNode.isArray()) {
            return (ArrayNode) jsonNode;
        } else {
            throw new IllegalArgumentException("Content is not array node");
        }
    }
}

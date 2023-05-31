package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.ERRORS_FOLDER;
import static no.unit.nva.publication.s3imports.ApplicationConstants.EVENTS_BUCKET;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultS3Client;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.SUBTOPIC_SEND_EVENT_TO_CRISTIN_ENTRIES_PATCH_EVENT_CONSUMER;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * The body of the event (field "detail") is of type {@link FileContentsEvent} and it contains the data of the file
 * located in the s3Location defined in {@link EventReference#getUri()} ()}.
 *
 * <p>In its present form the {@link FileContentsEvent} contains also a field with the name "publicationsOwner" which
 * is specific to the task of importing Cristin records.  In the future, this should be replaced by a more generic
 * format such as a {@link Map} annotated with "@JsonAnySetter".
 */
@JacocoGenerated
public class FileEntriesEventEmitter extends EventHandler<EventReference, PutSqsMessageResult> {

    public static final String WRONG_TOPIC_ERROR = "event does not contain the correct topic:";
    public static final String FILE_NOT_FOUND_ERROR = "File not found: ";
    public static final String FILE_EXTENSION_ERROR = ".error";
    public static final String FILE_CONTENTS_EMISSION_EVENT_TOPIC = "PublicationService.DataImport.DataEntry";
    public static final String EXPECTED_INPUT_TOPIC = FilenameEventEmitter.FILENAME_EMISSION_EVENT_TOPIC;
    private static final boolean SEQUENTIAL = false;
    private static final Logger logger = LoggerFactory.getLogger(FileEntriesEventEmitter.class);
    private static final String CONSECUTIVE_JSON_OBJECTS = "}\\s*\\{";
    private static final String NODES_IN_ARRAY = "},{";
    private static final Object END_OF_ARRAY = "]";
    private static final String BEGINNING_OF_ARRAY = "[";
    public static final String WRONG_SUBTOPIC = "event does not contain the correct subtopic";

    private final AmazonSQS amazonSQS;
    private final S3Client s3Client;
    private final Map<String, String> subtopicToQueueUrl;

    @JacocoGenerated
    public FileEntriesEventEmitter() {
        this(defaultS3Client(), defaultSqsClient());
    }

    public FileEntriesEventEmitter(S3Client s3Client,
                                   AmazonSQS amazonSQS) {
        super(EventReference.class);
        this.subtopicToQueueUrl = Map.of(SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER,
                                         new Environment().readEnv("CRISTIN_IMPORT_DATA_ENTRY_QUEUE_URL"),
                                         SUBTOPIC_SEND_EVENT_TO_CRISTIN_ENTRIES_PATCH_EVENT_CONSUMER,
                                         new Environment().readEnv("CRISTIN_IMPORT_PATCH_QUEUE_URL"));
        this.s3Client = s3Client;
        this.amazonSQS = amazonSQS;
    }

    @Override
    protected PutSqsMessageResult processInput(EventReference input, AwsEventBridgeEvent<EventReference> event,
                                               Context context) {
        logger.info("Input event: " + input.toJsonString());
        validateEvent(event);
        return attemptToPlaceMessagesOnQueue(input);
    }

    private static AmazonSQS defaultSqsClient() {
        return AmazonSQSClientBuilder.defaultClient();
    }

    private PutSqsMessageResult attemptToPlaceMessagesOnQueue(EventReference input) {
        var s3Driver = new S3Driver(s3Client, input.extractBucketName());
        return attempt(() -> fetchFileFromS3(input, s3Driver))
                   .map(this::parseContents)
                   .map(this::convertFieldNamesToLowerCase)
                   .map(jsonNodes -> generateMessageBodies(input, jsonNodes))
                   .map(this::createEventReferences)
                   .map(entries -> placeOnQueue(entries, input))
                   .map(result -> storePartialFailuresToS3(result, input))
                   .orElseThrow(failure -> storeCompleteFailureReport(failure, input));
    }

    private RuntimeException storeCompleteFailureReport(Failure<PutSqsMessageResult> failure, EventReference input) {
        var reportContent = failure.getException().toString();
        storeReportContentToS3(reportContent, input);
        throw new RuntimeException(failure.getException());
    }

    private PutSqsMessageResult storePartialFailuresToS3(PutSqsMessageResult result, EventReference input) {
        if (!result.getFailures().isEmpty()) {
            String reportContent = result.toJsonString();
            storeReportContentToS3(reportContent, input);
        }
        return result;
    }

    private void storeReportContentToS3(String reportContent, EventReference input) {
        S3Driver s3Driver = new S3Driver(s3Client, input.extractBucketName());
        var reportFilename = generateErrorReportUri(input);
        attempt(() -> s3Driver.insertFile(reportFilename.toS3bucketPath(), reportContent)).orElseThrow();
    }

    // This is done because cristin-import JsonIgnoreProperties is case-sensitive, and it seems casing is arbitary
    // from cristin-export. This functionality may be removed if JsonIgnoreProperty is removed from cristinObject.
    private List<JsonNode> convertFieldNamesToLowerCase(List<JsonNode> jsonNodes) {
        return jsonNodes.stream().map(this::convertKeysToLowerCase).collect(Collectors.toList());
    }

    private JsonNode convertKeysToLowerCase(JsonNode jsonNode) {
        if (jsonNode instanceof ObjectNode) {
            var objectNode = (ObjectNode) jsonNode;
            var fieldNames = new ArrayList<String>();
            objectNode.fieldNames().forEachRemaining(name -> addIfHasUpperCase(name, fieldNames));
            fieldNames.forEach(name -> setObjectNodeToLowerCase(name, objectNode));
        }
        jsonNode.elements().forEachRemaining(this::convertKeysToLowerCase);
        return jsonNode;
    }

    private void setObjectNodeToLowerCase(String name, ObjectNode objectNode) {
        var lowerCaseName = name.toLowerCase(Locale.ROOT);
        var child = objectNode.get(name);
        objectNode.remove(name);
        objectNode.set(lowerCaseName, child);
    }

    @SuppressWarnings("PMD.UnnecessaryCaseChange")
    private void addIfHasUpperCase(String name, List<String> fieldNames) {
        if (!name.toLowerCase(Locale.ROOT).equals(name)) {
            fieldNames.add(name);
        }
    }

    private UriWrapper generateErrorReportUri(EventReference input) {
        var inputUri = UriWrapper.fromUri(input.getUri().toString());
        var bucket = inputUri.getHost();

        return bucket
                   .addChild(ERRORS_FOLDER)
                   .addChild(timestampToString(input.getTimestamp()))
                   .addChild(inputUri.getParent().map(UriWrapper::getPath).orElse(UnixPath.EMPTY_PATH))
                   .addChild(makeFileExtensionError(inputUri.getLastPathElement()));
    }

    private String makeFileExtensionError(String filename) {
        return filename + FILE_EXTENSION_ERROR;
    }

    private Stream<FileContentsEvent<JsonNode>> generateMessageBodies(EventReference input, List<JsonNode> contents) {
        var fileUri = input.getUri();
        var timestamp = input.getTimestamp();
        return contents.stream()
                   .map(json -> new FileContentsEvent<>(
                       FILE_CONTENTS_EMISSION_EVENT_TOPIC,
                       input.getSubtopic(),
                       fileUri,
                       timestamp,
                       json));
    }

    private PutSqsMessageResult placeOnQueue(
        List<EventReference> eventBodies, EventReference input) {
        var client = new SqsBatchMessenger(amazonSQS, inferQueueUrlFromEventReference(input));
        return client.sendMessages(eventBodies);
    }

    private String inferQueueUrlFromEventReference(EventReference input) {
        return subtopicToQueueUrl.get(input.getSubtopic());
    }

    private List<EventReference> createEventReferences(
        Stream<FileContentsEvent<JsonNode>> eventBodies) {

        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        return eventBodies
                   .map(attempt(fileContents -> fileContents.toEventReference(s3Driver)))
                   .map(Try::orElseThrow).collect(Collectors.toList());
    }

    private String fetchFileFromS3(EventReference input, S3Driver s3Driver) {
        try {
            return s3Driver.readEvent(input.getUri());
        } catch (NoSuchKeyException exception) {
            throw new IllegalArgumentException(FILE_NOT_FOUND_ERROR + input.getUri(), exception);
        }
    }

    private void validateEvent(AwsEventBridgeEvent<EventReference> event) {
        if (!EXPECTED_INPUT_TOPIC.equalsIgnoreCase(event.getDetail().getTopic())) {
            logger.info(event.toJsonString());
            throw new IllegalArgumentException(WRONG_TOPIC_ERROR + event.getDetail().getTopic());
        }
        if (!SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER.equalsIgnoreCase(event.getDetail().getSubtopic())) {
            logger.info(event.toJsonString());
            throw new IllegalArgumentException(WRONG_SUBTOPIC + event.getDetail().getSubtopic());
        }
    }

    private List<JsonNode> parseContents(String content) {
        Try<List<JsonNode>> result = attempt(() -> parseContentAsJsonArray(content));
        if (result.isFailure()) {
            result = attempt(() -> parseContentsAsIndependentConsecutiveJsonObjects(content));
        }
        if (result.isFailure()) {
            result = attempt(() -> parseContentsAsIonFormat(content));
        }
        return result.orElseThrow();
    }
    
    private List<JsonNode> parseContentsAsIonFormat(String content) {
        return S3IonReader.extractJsonNodesFromIonContent(content).collect(Collectors.toList());
    }
    
    private List<JsonNode> parseContentsAsIndependentConsecutiveJsonObjects(String content) {
        
        return attempt(() -> content.replaceAll(CONSECUTIVE_JSON_OBJECTS, NODES_IN_ARRAY))
                   .map(jsonObjectStrings -> BEGINNING_OF_ARRAY + jsonObjectStrings + END_OF_ARRAY)
                   .map(jsonArrayString -> (ArrayNode) s3ImportsMapper.readTree(jsonArrayString))
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
        JsonNode jsonNode = s3ImportsMapper.readTree(content);
        if (jsonNode.isArray()) {
            return (ArrayNode) jsonNode;
        } else {
            throw new IllegalArgumentException("Content is not array node");
        }
    }
}

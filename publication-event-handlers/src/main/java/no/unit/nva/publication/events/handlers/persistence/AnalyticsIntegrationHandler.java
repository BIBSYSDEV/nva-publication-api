package no.unit.nva.publication.events.handlers.persistence;

import static no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler.EXPANDED_ENTRY_UPDATED_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.persistence.PersistenceConfig.PERSISTED_ENTRIES_BUCKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class AnalyticsIntegrationHandler extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String EXPECTED_EVENT_TOPIC_ERROR_MESSAGE =
        "The event topic is not the expected. Expected topic is "
        + EXPANDED_ENTRY_UPDATED_EVENT_TOPIC;
    public static final String ANALYTICS_ENTRY_PERSISTED_EVENT_TOPIC = "PublicationService.ExpandedEntry.Analytics";
    public static final String CONTEXT = "@context";
    public static final EventReference EMPTY_EVENT = null;
    public static final String TYPE_FIELD = "type";
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsIntegrationHandler.class);
    private final S3Client s3Client;

    @JacocoGenerated
    public AnalyticsIntegrationHandler() {
        this(S3Driver.defaultS3Client().build());
    }

    public AnalyticsIntegrationHandler(S3Client s3Client) {
        super(EventReference.class);
        this.s3Client = s3Client;
    }

    @Override
    protected EventReference processInputPayload(EventReference input,
                                                 AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                 Context context) {
        if (topicIsInvalid(input)) {
            logErrorMessageAndThrowException(event);
        }
        //this line will be deleted after we have verified that things work as they should.
        logger.info("input:" + attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(input)).orElseThrow());
        return processInputEvent(input.getUri());
    }

    private EventReference processInputEvent(URI inputFileLocation) {
        return readPublicationAndRemoveJsonLdContext(inputFileLocation)
            .map(fileContents -> storePublicationInAnalyticsFolder(fileContents, inputFileLocation))
            .map(this::createEventWithOutputFileUri)
            .orElse(EMPTY_EVENT);
    }

    private Optional<String> readPublicationAndRemoveJsonLdContext(URI inputFileLocation) {
        return Optional.ofNullable(readFileContents(inputFileLocation))
            .map(this::parseAsJson)
            .filter(this::expandedResourceIsPublication)
            .map(this::removeJsonLdContext)
            .orElseGet(this::ignoreNotInterestingEntries);
    }

    private String readFileContents(URI inputFileLocation) {
        var s3Driver = new S3Driver(s3Client, inputFileLocation.getHost());
        var inputFilePathInsideBucket = new UriWrapper(inputFileLocation).toS3bucketPath();
        return s3Driver.getFile(inputFilePathInsideBucket);
    }

    private URI storePublicationInAnalyticsFolder(String publication, URI inputFileLocation) {
        var writeS3Driver = new S3Driver(s3Client, PERSISTED_ENTRIES_BUCKET);
        return attempt(() -> constructOutputPath(inputFileLocation))
            .map(outputFilePath -> writeS3Driver.insertFile(outputFilePath, publication))
            .orElseThrow();
    }

    private UnixPath constructOutputPath(URI inputFileLocation) {
        var filename = new UriWrapper(inputFileLocation).getFilename();
        return PersistenceConfig.ANALYTICS_FOLDER.addChild(filename);
    }

    private EventReference createEventWithOutputFileUri(URI outputFileUri) {
        return new EventReference(ANALYTICS_ENTRY_PERSISTED_EVENT_TOPIC, outputFileUri);
    }

    private ObjectNode parseAsJson(String contents) {
        return (ObjectNode) attempt(() -> JsonUtils.dtoObjectMapper.readTree(contents)).orElseThrow();
    }

    private Optional<String> removeJsonLdContext(ObjectNode json) {
        json.remove(CONTEXT);
        return Optional.of(attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(json)).orElseThrow());
    }

    private Optional<String> ignoreNotInterestingEntries() {
        return Optional.empty();
    }

    private boolean expandedResourceIsPublication(ObjectNode json) {
        return ExpandedResource.TYPE.equalsIgnoreCase(json.get(TYPE_FIELD).textValue());
    }

    private boolean topicIsInvalid(EventReference input) {
        return !EXPANDED_ENTRY_UPDATED_EVENT_TOPIC.equals(input.getTopic());
    }

    private void logErrorMessageAndThrowException(AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event) {
        String jsonStringEvent = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(event)).orElseThrow();
        logger.error(EXPECTED_EVENT_TOPIC_ERROR_MESSAGE);
        logger.error("Event:" + jsonStringEvent);
        throw new IllegalStateException(EXPECTED_EVENT_TOPIC_ERROR_MESSAGE);
    }
}

package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ExpandResourcesHandler extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, EventPayload> {

    private static final String EVENTS_BUCKET = ENVIRONMENT.readEnv("EVENTS_BUCKET");
    private static final String HANDLER_EVENTS_FOLDER = ENVIRONMENT.readEnv("HANDLER_EVENTS_FOLDER");
    private static final String EVENT_TYPE = "indexedEntry.update";
    private final S3Driver s3Driver;
    private static final Logger logger = LoggerFactory.getLogger(ExpandResourcesHandler.class);

    @JacocoGenerated
    public ExpandResourcesHandler() {
        this(new S3Driver(EVENTS_BUCKET));
    }

    public ExpandResourcesHandler(S3Client s3Client) {
        this(new S3Driver(s3Client, EVENTS_BUCKET));
    }

    private ExpandResourcesHandler(S3Driver s3Driver) {
        super(DynamoEntryUpdateEvent.class);
        this.s3Driver = s3Driver;
    }

    @Override
    protected EventPayload processInputPayload(DynamoEntryUpdateEvent input,
                                               AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> event,
                                               Context context) {

        String json = toJsonString(input.getNewData());
        URI uri = s3Driver.insertEvent(UnixPath.of(HANDLER_EVENTS_FOLDER), json);
        logger.info(uri.toString());
        return new EventPayload(EVENT_TYPE, uri);
    }

    private String toJsonString(ResourceUpdate newData) {
        return attempt(() -> JsonUtils.dynamoObjectMapper.writeValueAsString(newData)).orElseThrow();
    }

}

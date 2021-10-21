package no.unit.nva.publication.events.expandresources;

import static no.unit.nva.publication.events.PublicationEventsConfig.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.UUID;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.events.PublicationEventsConfig;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ExpandResourcesHandler extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, EventPayload> {

    private static final Logger logger = LoggerFactory.getLogger(ExpandResourcesHandler.class);
    private static final String EVENTS_BUCKET= ENVIRONMENT.readEnv("EVENTS_BUCKET");
    public static final UriWrapper EVENTS_BUCKET_URI = new UriWrapper(URI.create("s3://" + EVENTS_BUCKET));
    private static final String HANDLERS_EVENTS_FOLDER = "expand-resources-events";
    private static final String EVENT_TYPE = "indexedEntry.update";
    private final S3Client s3Client;
    private final S3Driver s3Driver;

    public ExpandResourcesHandler(S3Client s3Client) {
        super(DynamoEntryUpdateEvent.class);
        this.s3Client = s3Client;
        this.s3Driver = new S3Driver(s3Client,EVENTS_BUCKET);
    }

    @Override
    protected EventPayload processInputPayload(DynamoEntryUpdateEvent input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> event,
                                       Context context) {

        String json = toJsonString(input.getNewData());
        UnixPath fullPath = filePath();
        URI s3Uri=  EVENTS_BUCKET_URI.addChild(fullPath).getUri();
        s3Driver.insertFile(fullPath, json);
        return new EventPayload(EVENT_TYPE,s3Uri);
    }

    private String toJsonString(ResourceUpdate newData) {
        return attempt(()->JsonUtils.dynamoObjectMapper.writeValueAsString(newData)).orElseThrow();
    }

    private UnixPath filePath() {
        return UnixPath.of(EVENTS_BUCKET).addChild(HANDLERS_EVENTS_FOLDER).addChild(UUID.randomUUID().toString());
    }
}

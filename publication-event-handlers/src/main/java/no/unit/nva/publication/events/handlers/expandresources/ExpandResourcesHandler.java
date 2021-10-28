package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedResourceUpdate;
import no.unit.nva.expansion.restclients.IdentityClientImpl;
import no.unit.nva.expansion.restclients.InstitutionClientImpl;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ExpandResourcesHandler extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, EventPayload> {

    public static final String MISSING_SECRETS_ERROR = "Mising secrets for internal communication with user service";
    private static final String EVENTS_BUCKET = ENVIRONMENT.readEnv("EVENTS_BUCKET");
    private static final String HANDLER_EVENTS_FOLDER = ENVIRONMENT.readEnv("HANDLER_EVENTS_FOLDER");
    private static final Logger logger = LoggerFactory.getLogger(ExpandResourcesHandler.class);
    private final S3Driver s3Driver;
    private final ResourceExpansionService resourceExpansionService;

    @JacocoGenerated
    public ExpandResourcesHandler() {
        this(new S3Driver(EVENTS_BUCKET), defaultResourceExpansionService());
    }

    public ExpandResourcesHandler(S3Client s3Client, ResourceExpansionService resourceExpansionService) {
        this(new S3Driver(s3Client, EVENTS_BUCKET), resourceExpansionService);
    }

    private ExpandResourcesHandler(S3Driver s3Driver, ResourceExpansionService resourceExpansionService) {
        super(DynamoEntryUpdateEvent.class);
        this.s3Driver = s3Driver;
        this.resourceExpansionService = resourceExpansionService;
    }

    @Override
    protected EventPayload processInputPayload(DynamoEntryUpdateEvent input,
                                               AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> event,
                                               Context context) {
        ResourceUpdate newData = input.getNewData();

        return transformToJson(newData)
            .map(this::insertEventBodyToS3)
            .stream()
            .peek(uri -> logger.info(uri.toString()))
            .map(EventPayload::indexedEntryEvent)
            .collect(SingletonCollector.collectOrElse(EventPayload.emptyEvent()));
    }

    @JacocoGenerated
    private static ResourceExpansionService defaultResourceExpansionService() {
        return new ResourceExpansionServiceImpl(new IdentityClientImpl(), new InstitutionClientImpl());
    }

    private URI insertEventBodyToS3(String string) {
        return s3Driver.insertEvent(UnixPath.of(HANDLER_EVENTS_FOLDER), string);
    }

    private Optional<String> transformToJson(ResourceUpdate newData) {
        return attempt(() -> createResourceIndexDocument(newData))
            .map(ExpandedResourceUpdate::toJsonString)
            .toOptional();
    }

    private ExpandedResourceUpdate createResourceIndexDocument(ResourceUpdate input) {
        return attempt(() -> resourceExpansionService.expandEntry(input)).orElseThrow();
    }
}

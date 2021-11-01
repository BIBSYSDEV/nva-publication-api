package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.HANDLER_EVENTS_FOLDER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.restclients.IdentityClientImpl;
import no.unit.nva.expansion.restclients.InstitutionClientImpl;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ExpandResourcesHandler extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, EventPayload> {

    public static final String ERROR_EXPANDING_RESOURCE_WARNING = "Error expanding resource:";
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

        return Optional.ofNullable(input.getNewData())
            .filter(this::shouldBeEnriched)
            .flatMap(this::transformToJson)
            .map(this::insertEventBodyToS3)
            .stream()
            .peek(uri -> logger.info("S3 URI:" + uri.toString()))
            .map(EventPayload::indexedEntryEvent)
            .collect(SingletonCollector.collectOrElse(EventPayload.emptyEvent()));
    }

    private boolean shouldBeEnriched(ResourceUpdate entry) {
        if (entry instanceof Resource) {
            Resource resource = (Resource) entry;
            return PublicationStatus.PUBLISHED.equals(resource.getStatus());
        } else if (entry instanceof DoiRequest) {
            DoiRequest doiRequest = (DoiRequest) entry;
            return PublicationStatus.PUBLISHED.equals(doiRequest.getResourceStatus());
        } else {
            return true;
        }
    }

    @JacocoGenerated
    private static ResourceExpansionService defaultResourceExpansionService() {
        return new ResourceExpansionServiceImpl(new IdentityClientImpl(), new InstitutionClientImpl());
    }

    private URI insertEventBodyToS3(String string) {
        return attempt(() -> s3Driver.insertEvent(UnixPath.of(HANDLER_EVENTS_FOLDER), string)).orElseThrow();
    }

    private Optional<String> transformToJson(ResourceUpdate newData) {
        return attempt(() -> createExpandedResourceUpdate(newData))
            .toOptional(fail -> logError(fail, newData));
    }

    private String createExpandedResourceUpdate(ResourceUpdate input) throws JsonProcessingException {
        return resourceExpansionService.expandEntry(input).toJsonString();
    }

    private void logError(Failure<?> fail, ResourceUpdate input) {
        Exception exception = fail.getException();
        logger.warn(ERROR_EXPANDING_RESOURCE_WARNING + input.getIdentifier(), exception);
    }
}

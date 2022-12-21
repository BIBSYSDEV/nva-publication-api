package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.utils.UriRetriever;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ExpandDataEntriesHandler
    extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String ERROR_EXPANDING_RESOURCE_WARNING = "Error expanding resource:";
    public static final String HANDLER_EVENTS_FOLDER = "PublicationService-DataEntryExpansion";
    public static final String EXPANDED_ENTRY_UPDATED_EVENT_TOPIC = "PublicationService.ExpandedDataEntry.Update";
    public static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    private static final Logger logger = LoggerFactory.getLogger(ExpandDataEntriesHandler.class);
    private final S3Driver s3Driver;
    private final ResourceExpansionService resourceExpansionService;

    @JacocoGenerated
    public ExpandDataEntriesHandler() {
        this(new S3Driver(EVENTS_BUCKET), defaultResourceExpansionService());
    }

    public ExpandDataEntriesHandler(S3Client s3Client, ResourceExpansionService resourceExpansionService) {
        this(new S3Driver(s3Client, EVENTS_BUCKET), resourceExpansionService);
    }

    private ExpandDataEntriesHandler(S3Driver s3Driver, ResourceExpansionService resourceExpansionService) {
        super(EventReference.class);
        this.s3Driver = s3Driver;
        this.resourceExpansionService = resourceExpansionService;
    }

    @Override
    protected EventReference processInputPayload(EventReference input,
                                                 AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                 Context context) {

        var blobObject = readBlobFromS3(input);
        return Optional.ofNullable(blobObject.getNewData())
            .filter(this::shouldBeEnriched)
            .flatMap(this::enrich)
            .map(this::insertEventBodyToS3)
            .stream()
            .map(uri -> new EventReference(EXPANDED_ENTRY_UPDATED_EVENT_TOPIC, uri))
            .collect(SingletonCollector.collectOrElse(emptyEvent()));
    }

    @JacocoGenerated
    private static ResourceExpansionService defaultResourceExpansionService() {
        return new ResourceExpansionServiceImpl(defaultResourceService(),
                                                TicketService.defaultService(),
                                                new UriRetriever());
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService() {
        return new ResourceService(DEFAULT_DYNAMODB_CLIENT, Clock.systemDefaultZone());
    }

    private DataEntryUpdateEvent readBlobFromS3(EventReference input) {
        var blobString = s3Driver.readEvent(input.getUri());
        return DataEntryUpdateEvent.fromJson(blobString);
    }

    private EventReference emptyEvent() {
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }

    private boolean shouldBeEnriched(Entity entry) {
        if (entry instanceof Resource) {
            Resource resource = (Resource) entry;
            return PublicationStatus.PUBLISHED.equals(resource.getStatus());
        } else if (entry instanceof DoiRequest) {
            return isDoiRequestReadyForEvaluation((DoiRequest) entry);
        } else {
            return true;
        }
    }

    private boolean isDoiRequestReadyForEvaluation(DoiRequest doiRequest) {
        return PublicationStatus.PUBLISHED.equals(doiRequest.getResourceStatus());
    }

    private URI insertEventBodyToS3(String string) {
        return attempt(() -> s3Driver.insertEvent(UnixPath.of(HANDLER_EVENTS_FOLDER), string)).orElseThrow();
    }

    private Optional<String> enrich(Entity newData) {
        return attempt(() -> createExpandedResourceUpdate(newData))
            .toOptional(fail -> logError(fail, newData));
    }

    private String createExpandedResourceUpdate(Entity input) throws JsonProcessingException, NotFoundException {
        return resourceExpansionService.expandEntry(input).toJsonString();
    }

    private void logError(Failure<?> fail, Entity input) {
        Exception exception = fail.getException();
        logger.warn(ERROR_EXPANDING_RESOURCE_WARNING + input.getIdentifier(), exception);
    }
}

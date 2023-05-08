package no.unit.nva.publication.events.handlers.expandresources;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Clock;
import java.util.Optional;

import static java.util.Objects.isNull;
import static no.unit.nva.model.PublicationStatus.DELETED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static nva.commons.core.attempt.Try.attempt;

public class ExpandDataEntriesHandler
    extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String ERROR_EXPANDING_RESOURCE_WARNING = "Error expanding resource:";
    public static final String HANDLER_EVENTS_FOLDER = "PublicationService-DataEntryExpansion";
    public static final String EXPANDED_ENTRY_UPDATED_EVENT_TOPIC = "PublicationService.ExpandedDataEntry.Update";
    public static final String EXPANDED_ENTRY_DELETE_EVENT_TOPIC = "PublicationService.ExpandedDataEntry.Delete";
    public static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    public static final String PUBLICATION_SERVICE_DATA_ENTRY_DELETION = "Publicationservice-DataEntryDeletion";
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
        if (shouldBeDeleted(blobObject.getNewData())) {
            return createDeleteEventReference(blobObject.getNewData());
        } else if (shouldBeEnriched(blobObject.getNewData())) {
            return createEnrichedEventReference(blobObject.getNewData()).orElseGet(this::emptyEvent);
        } else {
            return emptyEvent();
        }
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

    private EventReference createDeleteEventReference(Entity newData) {
        var resource = (Resource) newData;
        var publication = resource.toPublication();
        var uri = insertDeleteEventBodyToS3(publication.toString());
        return new EventReference(EXPANDED_ENTRY_DELETE_EVENT_TOPIC, uri);
    }

    private Optional<EventReference> createEnrichedEventReference(Entity newData) {
        return enrich(newData)
                   .map(this::insertEnrichEventBodyToS3)
                   .map(uri -> new EventReference(EXPANDED_ENTRY_UPDATED_EVENT_TOPIC, uri));
    }

    private boolean shouldBeDeleted(Entity entity) {
        return getPublicationStatus(entity).map(DELETED::equals).orElse(false);
    }

    private Optional<PublicationStatus> getPublicationStatus(Entity entity) {
        if (entity instanceof Resource) {
            Resource resource = (Resource) entity;
            return Optional.of(resource.getStatus());
        } else {
            return Optional.empty();
        }
    }

    private DataEntryUpdateEvent readBlobFromS3(EventReference input) {
        var blobString = s3Driver.readEvent(input.getUri());
        return DataEntryUpdateEvent.fromJson(blobString);
    }

    private EventReference emptyEvent() {
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }

    private boolean shouldBeEnriched(Entity entry) {
        if (isNull(entry)) {
            return false;
        }
        var publicationStatus = getPublicationStatus(entry);
        if (publicationStatus.isPresent()) {
            return PUBLISHED.equals(publicationStatus.get()) || PUBLISHED_METADATA.equals(publicationStatus.get());
        } else if (entry instanceof DoiRequest) {
            return isDoiRequestReadyForEvaluation((DoiRequest) entry);
        } else {
            return true;
        }
    }

    private boolean isDoiRequestReadyForEvaluation(DoiRequest doiRequest) {
        return PUBLISHED.equals(doiRequest.getResourceStatus());
    }

    private URI insertDeleteEventBodyToS3(String body) {
        return attempt(
            () -> s3Driver.insertEvent(UnixPath.of(PUBLICATION_SERVICE_DATA_ENTRY_DELETION), body)).orElseThrow();
    }

    private URI insertEnrichEventBodyToS3(String string) {
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

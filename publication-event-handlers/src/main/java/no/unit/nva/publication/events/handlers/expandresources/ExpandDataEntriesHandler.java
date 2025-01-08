package no.unit.nva.publication.events.handlers.expandresources;

import static java.util.Objects.isNull;
import static no.unit.nva.model.PublicationStatus.DELETED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.ResourceQueueClient;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ExpandDataEntriesHandler extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String ERROR_EXPANDING_RESOURCE_WARNING = "Error expanding resource: {}";
    public static final String HANDLER_EVENTS_FOLDER = "PublicationService-DataEntryExpansion";
    public static final String EXPANDED_ENTRY_UPDATED_EVENT_TOPIC = "PublicationService.ExpandedDataEntry.Update";
    public static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String RECOVERY_QUEUE = new Environment().readEnv("RECOVERY_QUEUE");
    public static final List<PublicationStatus> PUBLICATION_STATUS_TO_BE_ENRICHED = List.of(PUBLISHED,
                                                                                            PUBLISHED_METADATA,
                                                                                            UNPUBLISHED, DELETED);
    public static final String BACKEND_CLIENT_AUTH_URL = "BACKEND_CLIENT_AUTH_URL";
    public static final String BACKEND_CLIENT_SECRET_NAME = "BACKEND_CLIENT_SECRET_NAME";
    private static final Logger logger = LoggerFactory.getLogger(ExpandDataEntriesHandler.class);
    public static final String SENT_TO_RECOVERY_QUEUE_MESSAGE = "DateEntry has been sent to recovery queue: {}";
    public static final String EXPANSION_FAILED_MESSAGE = "Error expanding entity %s with identifier %s";
    private final QueueClient sqsClient;
    private final S3Driver s3Driver;
    private final ResourceExpansionService resourceExpansionService;

    @JacocoGenerated
    public ExpandDataEntriesHandler() {
        this(ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE),
             new S3Driver(EVENTS_BUCKET),
             defaultResourceExpansionService());
    }

    public ExpandDataEntriesHandler(QueueClient sqsClient, S3Client s3Client,
                                    ResourceExpansionService resourceExpansionService) {
        this(sqsClient, new S3Driver(s3Client, EVENTS_BUCKET), resourceExpansionService);
    }

    private ExpandDataEntriesHandler(QueueClient sqsClient, S3Driver s3Driver,
                                     ResourceExpansionService resourceExpansionService) {
        super(EventReference.class);
        this.sqsClient = sqsClient;
        this.s3Driver = s3Driver;
        this.resourceExpansionService = resourceExpansionService;
    }

    @Override
    protected EventReference processInputPayload(EventReference input,
                                                 AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                 Context context) {
        var blobObject = readBlobFromS3(input);
        return attempt(() -> processDataEntryUpdateEvent(blobObject)).orElse(
            failure -> persistRecoveryMessage(failure, blobObject));
    }

    private static SortableIdentifier getIdentifier(DataEntryUpdateEvent blobObject) {
        return Optional.ofNullable(blobObject.getOldData())
                   .map(Entity::getIdentifier)
                   .orElseGet(() -> blobObject.getNewData().getIdentifier());
    }

    @JacocoGenerated
    private static ResourceExpansionService defaultResourceExpansionService() {
        var uriRetriever = new UriRetriever();
        var authorizedUriRetriever = new AuthorizedBackendUriRetriever(ENVIRONMENT.readEnv(BACKEND_CLIENT_AUTH_URL),
                                                                       ENVIRONMENT.readEnv(BACKEND_CLIENT_SECRET_NAME));
        return new ResourceExpansionServiceImpl(defaultResourceService(), TicketService.defaultService(),
                                                authorizedUriRetriever, uriRetriever);
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService() {
        return ResourceService.defaultService();
    }

    private EventReference persistRecoveryMessage(Failure<EventReference> failure, DataEntryUpdateEvent blobObject) {
        var identifier = getIdentifier(blobObject);
        RecoveryEntry.fromDataEntryUpdateEvent(blobObject)
            .withIdentifier(identifier)
            .withException(failure.getException())
            .persist(sqsClient);
        logger.error(SENT_TO_RECOVERY_QUEUE_MESSAGE, identifier);
        return null;
    }

    private EventReference processDataEntryUpdateEvent(DataEntryUpdateEvent blobObject) {
        return shouldBeEnriched(blobObject.getNewData())
                   ? createEnrichedEventReference(blobObject.getNewData())
                   : emptyEvent();
    }

    private EventReference createEnrichedEventReference(Entity newData) {
        return attempt(() -> resourceExpansionService.expandEntry(newData, true))
                   .map(ExpandedDataEntry::toJsonString)
                   .map(this::insertEnrichEventBodyToS3)
                   .map(uri -> new EventReference(EXPANDED_ENTRY_UPDATED_EVENT_TOPIC, uri))
                   .orElseThrow(failure -> throwError(failure, newData));
    }

    private ExpandedDataEntryException throwError(Failure<EventReference> failure, Entity newData) {
        logger.error(ERROR_EXPANDING_RESOURCE_WARNING, newData.getIdentifier());
        return new ExpandedDataEntryException(EXPANSION_FAILED_MESSAGE.formatted(newData.getType(), newData.getIdentifier()),
                                      failure.getException());
    }

    private Optional<PublicationStatus> getPublicationStatus(Entity entity) {
        if (entity instanceof Resource resource) {
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
            return PUBLICATION_STATUS_TO_BE_ENRICHED.contains(publicationStatus.get());
        } else if (entry instanceof DoiRequest doiRequest) {
            return isDoiRequestReadyForEvaluation(doiRequest);
        } else {
            return true;
        }
    }

    private boolean isDoiRequestReadyForEvaluation(DoiRequest doiRequest) {
        return PUBLISHED.equals(doiRequest.getResourceStatus());
    }

    private URI insertEnrichEventBodyToS3(String string) {
        return attempt(() -> s3Driver.insertEvent(UnixPath.of(HANDLER_EVENTS_FOLDER), string)).orElseThrow();
    }
}

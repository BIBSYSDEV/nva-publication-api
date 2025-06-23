package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.persistence.PersistenceConfig.PERSISTED_ENTRIES_BUCKET;
import static no.unit.nva.publication.queue.RecoveryEntry.FILE;
import static no.unit.nva.publication.queue.RecoveryEntry.MESSAGE;
import static no.unit.nva.publication.queue.RecoveryEntry.RESOURCE;
import static no.unit.nva.publication.queue.RecoveryEntry.TICKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.RecoveryEntry;
import no.unit.nva.publication.queue.ResourceQueueClient;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class ExpandDataEntriesHandler extends DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    private static final String EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC = "PublicationService.ExpandedEntry.Persisted";
    private static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    private static final Environment ENVIRONMENT = new Environment();
    private static final String RECOVERY_QUEUE = new Environment().readEnv("RECOVERY_QUEUE");
    private static final String BACKEND_CLIENT_AUTH_URL = "BACKEND_CLIENT_AUTH_URL";
    private static final String BACKEND_CLIENT_SECRET_NAME = "BACKEND_CLIENT_SECRET_NAME";
    private static final Logger logger = LoggerFactory.getLogger(ExpandDataEntriesHandler.class);
    private static final String SENT_TO_RECOVERY_QUEUE_MESSAGE = "DateEntry has been sent to recovery queue: {}";
    private final QueueClient sqsClient;
    private final S3Driver s3DriverEventsBucket;
    private final EntityExpansionResolverRegistry entityExpansionResolverRegistry;
    private final PersistedResourcesService persistedResourcesService;
    private final ResourceExpansionService resourceExpansionService;

    @JacocoGenerated
    public ExpandDataEntriesHandler() {
        this(ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE),
             new S3Driver(EVENTS_BUCKET), new S3Driver(PERSISTED_ENTRIES_BUCKET),
             defaultResourceExpansionService());
    }

    public ExpandDataEntriesHandler(QueueClient sqsClient, S3Client s3Client,
                                    ResourceExpansionService resourceExpansionService) {
        this(sqsClient, new S3Driver(s3Client, EVENTS_BUCKET), new S3Driver(s3Client, PERSISTED_ENTRIES_BUCKET),
             resourceExpansionService);
    }

    private ExpandDataEntriesHandler(QueueClient sqsClient, S3Driver s3DriverEventsBucket,
                                     S3Driver s3DriverPersistedResourcesBucket,
                                     ResourceExpansionService resourceExpansionService) {
        super(EventReference.class);
        this.sqsClient = sqsClient;
        this.s3DriverEventsBucket = s3DriverEventsBucket;
        this.resourceExpansionService = resourceExpansionService;
        this.persistedResourcesService = new PersistedResourcesService(s3DriverPersistedResourcesBucket);
        this.entityExpansionResolverRegistry = initializeEntityExpansionStrategyRegistry();
    }

    private EntityExpansionResolverRegistry initializeEntityExpansionStrategyRegistry() {
        var registry = new EntityExpansionResolverRegistry();

        registry.register(Resource.class, new ResourceExpansionResolver());
        registry.register(TicketEntry.class, new TicketExpansionResolver());
        registry.register(Message.class, new MessageExpansionResolver());
        registry.register(FileEntry.class, new FileEntryExpansionResolver());

        return registry;
    }

    @Override
    protected EventReference processInputPayload(EventReference input,
                                                 AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                 Context context) {
        var dataEntryUpdateEvent = readDataEntryUpdateEventFromS3(input);
        return attempt(() -> processDataEntryUpdateEvent(dataEntryUpdateEvent))
                   .orElse(failure -> persistRecoveryMessage(failure, dataEntryUpdateEvent));
    }

    private static SortableIdentifier getIdentifier(DataEntryUpdateEvent dataEntryUpdateEvent) {
        return Optional.ofNullable(dataEntryUpdateEvent.getOldData())
                   .map(Entity::getIdentifier)
                   .orElseGet(() -> dataEntryUpdateEvent.getNewData().getIdentifier());
    }

    @JacocoGenerated
    private static ResourceExpansionService defaultResourceExpansionService() {
        var uriRetriever = new UriRetriever();
        var authorizedUriRetriever = new AuthorizedBackendUriRetriever(ENVIRONMENT.readEnv(BACKEND_CLIENT_AUTH_URL),
                                                                       ENVIRONMENT.readEnv(BACKEND_CLIENT_SECRET_NAME));
        return new ResourceExpansionServiceImpl(defaultResourceService(), TicketService.defaultService(),
                                                authorizedUriRetriever, uriRetriever,
                                                ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE));
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService() {
        return ResourceService.defaultService();
    }

    private EventReference persistRecoveryMessage(Failure<EventReference> failure,
                                                  DataEntryUpdateEvent dataEntryUpdateEvent) {
        var identifier = getIdentifier(dataEntryUpdateEvent);
        RecoveryEntry.create(findType(dataEntryUpdateEvent), identifier)
            .withException(failure.getException())
            .persist(sqsClient);
        logger.error(SENT_TO_RECOVERY_QUEUE_MESSAGE, identifier);
        return null;
    }

    private static String findType(DataEntryUpdateEvent dataEntryUpdateEvent) {
        var entity = Optional.ofNullable(dataEntryUpdateEvent.getOldData()).orElseGet(dataEntryUpdateEvent::getNewData);
        return switch (entity) {
            case Resource resource -> RESOURCE;
            case TicketEntry ticket -> TICKET;
            case Message message -> MESSAGE;
            case FileEntry fileEntry -> FILE;
            default -> throw new IllegalStateException("Unexpected value: " + entity);
        };
    }

    private EventReference processDataEntryUpdateEvent(DataEntryUpdateEvent dataEntryUpdateEvent) {
        return entityExpansionResolverRegistry
                   .resolveEntityToExpand(dataEntryUpdateEvent.getOldData(), dataEntryUpdateEvent.getNewData())
                   .map(this::expandEntityOrThrow)
                   .flatMap(expandedDataEntry -> expandedDataEntry.map(this::createEnrichedEventReference))
                   .orElseGet(this::emptyEvent);
    }

    private Optional<ExpandedDataEntry> expandEntityOrThrow(Entity entity) {
        return attempt(() -> resourceExpansionService.expandEntry(entity, true))
                   .orElseThrow(
                       failure -> new EntityExpansionException("Failed to expand " + entity, failure.getException()));
    }

    private EventReference createEnrichedEventReference(ExpandedDataEntry expandedDataEntry) {
        return Optional.of(persistedResourcesService.persist(expandedDataEntry))
                   .map(uri -> new EventReference(EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC, uri))
                   .orElseThrow();
    }

    private DataEntryUpdateEvent readDataEntryUpdateEventFromS3(EventReference input) {
        var dataEntryUpdateEventAsString = s3DriverEventsBucket.readEvent(input.getUri());
        return DataEntryUpdateEvent.fromJson(dataEntryUpdateEventAsString);
    }

    private EventReference emptyEvent() {
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }
}

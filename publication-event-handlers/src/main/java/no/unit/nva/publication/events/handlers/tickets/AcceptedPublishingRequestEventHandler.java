package no.unit.nva.publication.events.handlers.tickets;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class AcceptedPublishingRequestEventHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private static final String DOI_REQUEST_CREATION_MESSAGE = "Doi request has been created for publication: {}";
    private static final Logger logger = LoggerFactory.getLogger(AcceptedPublishingRequestEventHandler.class);
    private static final String PUBLISHING_FILES_MESSAGE =
        "Publishing files for publication {} via approved " + "publishing request {}";
    private static final String COULD_NOT_FETCH_TICKET_MESSAGE = "Could not fetch PublishingRequest with identifier: ";
    private static final String PUBLISHING_ERROR_MESSAGE = "Could not publish publication: %s";
    private static final String EXCEPTION_MESSAGE = "Exception: {}";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final S3Driver s3Driver;

    @JacocoGenerated
    public AcceptedPublishingRequestEventHandler() {
        this(ResourceService.defaultService(),
             new TicketService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT, new UriRetriever()),
             S3Driver.defaultS3Client().build());
    }

    protected AcceptedPublishingRequestEventHandler(ResourceService resourceService, TicketService ticketService,
                                                    S3Client s3Client) {
        super(EventReference.class);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
    }

    // TODO: hasEffectiveChanges method should be implemented in a predecessor eventHandler.
    @Override
    protected Void processInputPayload(EventReference input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                       Context context) {
        var eventBlob = s3Driver.readEvent(input.getUri());
        var ticketUpdate = parseInput(eventBlob);
        if (hasEffectiveChanges(eventBlob)) {
            handleChanges(ticketUpdate);
        }
        return null;
    }

    private static boolean hasDoi(Resource resource) {
        return nonNull(resource.getDoi());
    }

    private static boolean noEffectiveChanges(DataEntryUpdateEvent updateEvent) {
        var newStatus = getNewStatus(updateEvent);
        var oldStatus = getOldStatus(updateEvent);
        return oldStatus.equals(newStatus);
    }

    private static Optional<String> getOldStatus(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent).map(DataEntryUpdateEvent::getOldData).map(Entity::getStatusString);
    }

    private static Optional<String> getNewStatus(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent).map(DataEntryUpdateEvent::getNewData).map(Entity::getStatusString);
    }

    private void handleChanges(FilesApprovalEntry filesApprovalEntry) {
        switch (filesApprovalEntry.getStatus()) {
            case PENDING -> handlePendingPublishingRequest(filesApprovalEntry);
            case COMPLETED -> handleCompletedPublishingRequest(filesApprovalEntry);
            case CLOSED -> handleClosedPublishingRequest(filesApprovalEntry);
            default -> {
                // Ignore other non-final statuses
            }
        }
    }

    private void handlePendingPublishingRequest(FilesApprovalEntry entry) {
        var resource = fetchResource(entry.getResourceIdentifier());
        if (REGISTRATOR_PUBLISHES_METADATA_ONLY.equals(entry.getWorkflow())) {
            publishResource(resource, entry);
            refreshPublishingRequestAfterPublishingMetadata(entry);
        }
        createDoiRequestIfNeeded(resource.getIdentifier(), UserInstance.fromTicket(entry));
    }

    /**
     * Is needed in order to populate publication status changes in search-index when publication is being published.
     *
     * @param publishingRequest to refresh
     */
    private void refreshPublishingRequestAfterPublishingMetadata(FilesApprovalEntry publishingRequest) {
        publishingRequest.persistUpdate(ticketService);
    }

    private void handleClosedPublishingRequest(FilesApprovalEntry publishingRequestCase) {
        publishingRequestCase.rejectRejectedFiles(resourceService);
    }

    private boolean hasEffectiveChanges(String eventBlob) {
        return !noEffectiveChanges(DataEntryUpdateEvent.fromJson(eventBlob));
    }

    private void handleCompletedPublishingRequest(FilesApprovalEntry entry) {
        var resource = fetchResource(entry.getResourceIdentifier());
        var filesApprovalEntry = fetchTicket(entry);

        publishWhenPublicationStatusDraft(resource, filesApprovalEntry);

        if (!filesApprovalEntry.getApprovedFiles().isEmpty()) {
            filesApprovalEntry.publishApprovedFiles(resourceService);
        }

        logger.info(PUBLISHING_FILES_MESSAGE, resource.getIdentifier(), filesApprovalEntry.getIdentifier());

        createDoiRequestIfNeeded(resource.getIdentifier(), UserInstance.fromTicket(filesApprovalEntry));
    }

    private void publishWhenPublicationStatusDraft(Resource resource, FilesApprovalEntry filesApprovalEntry) {
        if (PublicationStatus.DRAFT.equals(resource.getStatus())) {
            publishResource(resource, filesApprovalEntry);
        }
    }

    private FilesApprovalEntry fetchTicket(FilesApprovalEntry entry) {
        return attempt(() -> ticketService.fetchTicket(entry)).map(FilesApprovalEntry.class::cast)
                   .orElseThrow(failure -> throwException(COULD_NOT_FETCH_TICKET_MESSAGE + entry.getIdentifier(),
                                                          failure.getException()));
    }

    private RuntimeException throwException(String message, Exception exception) {
        logger.error(message);
        logger.error(EXCEPTION_MESSAGE, exception.getMessage());
        throw new RuntimeException();
    }

    private void publishResource(Resource resource, FilesApprovalEntry entry) {
        var userInstance = UserInstance.fromTicket(entry);
        logger.info("Publishing resource: {}", resource.getIdentifier());
        try {
            resource.publish(resourceService, userInstance);
        } catch (Exception e) {
            throwException(String.format(PUBLISHING_ERROR_MESSAGE, resource.getIdentifier()), e);
        }
    }

    private Resource fetchResource(SortableIdentifier resourceIdentifier) {
        return Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService).orElseThrow();
    }

    /**
     * Creating DoiRequest for a sortableIdentifier necessarily owned by sortableIdentifier owner institution and not
     * the institution that requests the doi.
     *
     * @param resourceIdentifier to create a DoiRequest for
     * @param userInstance
     */
    private void createDoiRequestIfNeeded(SortableIdentifier resourceIdentifier, UserInstance userInstance) {
        var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService).orElseThrow();
        if (hasDoi(resource) && !doiRequestExists(resource)) {
            var doiRequest = DoiRequest.create(resource, userInstance);
            attempt(() -> doiRequest.persistNewTicket(ticketService)).orElseThrow();
            logger.info(DOI_REQUEST_CREATION_MESSAGE, resource.getIdentifier());
        }
    }

    private boolean doiRequestExists(Resource resource) {
        return fetchDoiRequest(resource).isPresent();
    }

    private Optional<DoiRequest> fetchDoiRequest(Resource resource) {
        return ticketService.fetchTicketByResourceIdentifier(resource.getCustomerId(), resource.getIdentifier(),
                                                             DoiRequest.class);
    }

    private FilesApprovalEntry parseInput(String eventBlob) {
        var entryUpdate = DataEntryUpdateEvent.fromJson(eventBlob);
        return (FilesApprovalEntry) entryUpdate.getNewData();
    }
}

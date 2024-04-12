package no.unit.nva.publication.events.handlers.tickets;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class AcceptedPublishingRequestEventHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    public static final String DOI_REQUEST_CREATION_MESSAGE = "Doi request has been created for publication: {}";
    private static final Logger logger = LoggerFactory.getLogger(AcceptedPublishingRequestEventHandler.class);
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final S3Driver s3Driver;

    @JacocoGenerated
    public AcceptedPublishingRequestEventHandler() {
        this(ResourceService.defaultService(),
             new TicketService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT, UriRetriever.defaultUriRetriever()),
             S3Driver.defaultS3Client().build());
    }

    protected AcceptedPublishingRequestEventHandler(ResourceService resourceService,
                                                    TicketService ticketService,
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
        if (isCompleted(ticketUpdate) && hasEffectiveChanges(eventBlob)) {
            publishPublicationAndFiles(ticketUpdate);
        }
        return null;
    }

    private static boolean isCompleted(PublishingRequestCase latestUpdate) {
        return TicketStatus.COMPLETED.equals(latestUpdate.getStatus());
    }

    private static boolean hasDoi(Publication publication) {
        return nonNull(publication.getDoi());
    }

    private static boolean noEffectiveChanges(DataEntryUpdateEvent updateEvent) {
        var newStatus = getNewStatus(updateEvent);
        var oldStatus = getOldStatus(updateEvent);
        return oldStatus.equals(newStatus);
    }

    private static Optional<String> getOldStatus(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
                   .map(DataEntryUpdateEvent::getOldData)
                   .map(Entity::getStatusString);
    }

    private static Optional<String> getNewStatus(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
                   .map(DataEntryUpdateEvent::getNewData)
                   .map(Entity::getStatusString);
    }

    private boolean hasEffectiveChanges(String eventBlob) {
        return !noEffectiveChanges(DataEntryUpdateEvent.fromJson(eventBlob));
    }

    private void publishPublicationAndFiles(PublishingRequestCase latestUpdate) {
        var userInstance = UserInstance.create(latestUpdate.getOwner(), latestUpdate.getCustomerId());
        var publication = fetchPublication(userInstance, latestUpdate.getResourceIdentifier());
        var updatedPublication = toPublicationWithPublishedFiles(publication);
        if (PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY.equals(latestUpdate.getWorkflow())
            || PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES.equals(latestUpdate.getWorkflow())) {
            publishFiles(updatedPublication);
        }
        if (PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES.equals(
            latestUpdate.getWorkflow())) {
            publishFiles(updatedPublication);
            publishPublication(latestUpdate, userInstance);
        }
        createDoiRequestIfNeeded(updatedPublication);
    }

    private void publishPublication(PublishingRequestCase latestUpdate, UserInstance userInstance) {
        attempt(() -> resourceService.publishPublication(userInstance, latestUpdate.getResourceIdentifier()))
            .orElse(fail -> logError(fail.getException()));
    }

    private void publishFiles(Publication updatedPublication) {
        attempt(() -> resourceService.updatePublication(updatedPublication));
    }

    private Publication toPublicationWithPublishedFiles(Publication publication) {
        return publication.copy()
                   .withAssociatedArtifacts(convertFilesToPublished(publication.getAssociatedArtifacts()))
                   .build();
    }

    private List<AssociatedArtifact> convertFilesToPublished(AssociatedArtifactList associatedArtifacts) {
        return associatedArtifacts.stream()
                   .map(this::updateFileToPublished)
                   .collect(Collectors.toList());
    }

    private AssociatedArtifact updateFileToPublished(AssociatedArtifact artifact) {
        if (artifact instanceof UnpublishedFile) {
            var file = (File) artifact;
            return file.toPublishedFile();
        } else {
            return artifact;
        }
    }

    private Publication fetchPublication(UserInstance userInstance, SortableIdentifier publicationIdentifier) {
        return attempt(() -> resourceService.getPublication(userInstance, publicationIdentifier))
                   .orElseThrow();
    }

    private void createDoiRequestIfNeeded(Publication publication) {
        if (hasDoi(publication) && !doiRequestExists(publication)) {
            attempt(() -> DoiRequest.fromPublication(publication).persistNewTicket(ticketService)).orElseThrow();
            logger.info(DOI_REQUEST_CREATION_MESSAGE, publication.getIdentifier());
        }
    }

    private boolean doiRequestExists(Publication publication) {
        return fetchTicket(publication).isPresent();
    }

    private Optional<DoiRequest> fetchTicket(Publication publication) {
        return ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                             publication.getIdentifier(),
                                                             DoiRequest.class);
    }

    private PublishPublicationStatusResponse logError(Exception exception) {
        logger.warn(ExceptionUtils.stackTraceInSingleLine(exception));
        return null;
    }

    private PublishingRequestCase parseInput(String eventBlob) {
        var entryUpdate = DataEntryUpdateEvent.fromJson(eventBlob);
        return (PublishingRequestCase) entryUpdate.getNewData();
    }
}

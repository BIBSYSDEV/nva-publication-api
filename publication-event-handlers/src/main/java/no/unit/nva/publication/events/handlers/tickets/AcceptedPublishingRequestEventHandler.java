package no.unit.nva.publication.events.handlers.tickets;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.model.associatedartifacts.file.PendingInternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class AcceptedPublishingRequestEventHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    public static final String UNKNOWN_WORKFLOW_MESSAGE = "Unknown workflow: {}";
    private static final String DOI_REQUEST_CREATION_MESSAGE = "Doi request has been created for publication: {}";
    private static final Logger logger = LoggerFactory.getLogger(AcceptedPublishingRequestEventHandler.class);
    private static final String PUBLISHING_METADATA_AND_FILES_MESSAGE =
        "Publishing files and publication metadata {}" + " via approved publishing request {}";
    private static final String PUBLISHING_FILES_MESSAGE =
        "Publishing files for publication {} via approved " + "publishing request {}";
    private static final String COULD_NOT_FETCH_TICKET_MESSAGE = "Could not fetch PublishingRequest with identifier: ";
    private static final String PUBLISHING_ERROR_MESSAGE = "Could not publish publication: %s";
    private static final String EXCEPTION_MESSAGE = "Exception: {}";
    private static final String PUBLICATION_UPDATE_ERROR_MESSAGE = "Could not update publication: %s";
    private static final String PUBLISHING_FILE_MESSAGE =
        "Publishing file {} of type {} from approved " + "PublishingRequest {} for publication {}";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final S3Driver s3Driver;

    @JacocoGenerated
    public AcceptedPublishingRequestEventHandler() {
        this(ResourceService.defaultService(),
             new TicketService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT, UriRetriever.defaultUriRetriever()),
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

    private void handleChanges(PublishingRequestCase publishingRequest) {
        switch (publishingRequest.getStatus()) {
            case COMPLETED -> handleCompletedPublishingRequest(publishingRequest);
            case CLOSED -> handleClosedPublishingRequest(publishingRequest);
            default -> {
                // Ignore other non-final statuses
            }
        }
    }

    private void handleClosedPublishingRequest(PublishingRequestCase publishingRequestCase) {
        var publication = fetchPublication(publishingRequestCase.getResourceIdentifier());
        var updatedPublication = publication.copy()
                                     .withAssociatedArtifacts(rejectFiles(publication))
                                     .build();
        resourceService.updatePublication(updatedPublication);
    }

    private List<AssociatedArtifact> rejectFiles(Publication publication) {
        var associatedArtifacts = publication.getAssociatedArtifacts();
        return associatedArtifacts.stream()
                   .map(this::rejectFile)
                   .toList();
    }

    //TODO: Remove unpublishable file and logic related to it after we have migrated files
    private AssociatedArtifact rejectFile(AssociatedArtifact associatedArtifact) {
        return switch (associatedArtifact) {
            case UnpublishedFile unpublishedFile -> unpublishedFile.toUnpublishableFile();
            case PendingFile<?> pendingFile -> pendingFile.reject();
            default -> associatedArtifact;
        };
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
        return Optional.of(updateEvent).map(DataEntryUpdateEvent::getOldData).map(Entity::getStatusString);
    }

    private static Optional<String> getNewStatus(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent).map(DataEntryUpdateEvent::getNewData).map(Entity::getStatusString);
    }

    private static void logFilePublish(File unpublishedFile, PublishingRequestCase publishingRequestCase) {
        logger.info(PUBLISHING_FILE_MESSAGE, unpublishedFile.getIdentifier(),
                    unpublishedFile.getClass().getSimpleName(), publishingRequestCase.getIdentifier(),
                    publishingRequestCase.getResourceIdentifier());
    }

    private static File toPublishedFile(PendingFile<?> pendingFile,
                                        PublishingRequestCase publishingRequestCase) {
        logFilePublish((File) pendingFile, publishingRequestCase);
        return pendingFile.approve();
    }

    private boolean hasEffectiveChanges(String eventBlob) {
        return !noEffectiveChanges(DataEntryUpdateEvent.fromJson(eventBlob));
    }

    private void handleCompletedPublishingRequest(PublishingRequestCase publishingRequestCase) {
        var publication = fetchPublication(publishingRequestCase.getResourceIdentifier());
        var publishingRequest = fetchPublishingRequest(publishingRequestCase);
        var updatedPublication = toPublicationWithPublishedFiles(publication, publishingRequest);
        var ticketIdentifier = publishingRequest.getIdentifier();
        switch (publishingRequestCase.getWorkflow()) {
            case REGISTRATOR_PUBLISHES_METADATA_ONLY, REGISTRATOR_PUBLISHES_METADATA_AND_FILES -> {
                publishFiles(updatedPublication);
                logger.info(PUBLISHING_FILES_MESSAGE, publication.getIdentifier(), ticketIdentifier);
            }
            case REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES -> {
                publishFiles(updatedPublication);
                publishPublication(publication);
                logger.info(PUBLISHING_METADATA_AND_FILES_MESSAGE, publication.getIdentifier(), ticketIdentifier);
            }
            default -> logger.error(UNKNOWN_WORKFLOW_MESSAGE, publishingRequestCase.getWorkflow());
        }
        createDoiRequestIfNeeded(updatedPublication);
    }

    private PublishingRequestCase fetchPublishingRequest(PublishingRequestCase latestUpdate) {
        return attempt(() -> ticketService.fetchTicket(latestUpdate)).map(PublishingRequestCase.class::cast)
                   .orElseThrow(failure -> throwException(COULD_NOT_FETCH_TICKET_MESSAGE + latestUpdate.getIdentifier(),
                                                          failure));
    }

    private RuntimeException throwException(String message, Failure<?> failure) {
        logger.error(message);
        logger.error(EXCEPTION_MESSAGE, failure.getException().getMessage());
        return new RuntimeException();
    }

    private void publishPublication(Publication publication) {
        var publicationIdentifier = publication.getIdentifier();
        var userInstance = UserInstance.fromPublication(publication);
        attempt(() -> resourceService.publishPublication(userInstance, publicationIdentifier))
            .orElseThrow(fail -> throwException(PUBLISHING_ERROR_MESSAGE.formatted(publicationIdentifier), fail));
    }

    private void publishFiles(Publication updatedPublication) {
        attempt(() -> resourceService.updatePublication(updatedPublication)).orElseThrow(failure -> throwException(
            String.format(PUBLICATION_UPDATE_ERROR_MESSAGE, updatedPublication.getIdentifier()), failure));
    }

    private Publication toPublicationWithPublishedFiles(Publication publication,
                                                        PublishingRequestCase publishingRequest) {
        var updatedAssociatedArtifacts = publishFilesFromPublishingRequest(publication.getAssociatedArtifacts(),
                                                                           publishingRequest);
        return publication.copy().withAssociatedArtifacts(updatedAssociatedArtifacts).build();
    }

    private List<AssociatedArtifact> publishFilesFromPublishingRequest(AssociatedArtifactList associatedArtifacts,
                                                                       PublishingRequestCase publishingRequestCase) {
        return associatedArtifacts.stream()
                   .map(associatedArtifact -> publishFileIfApproved(associatedArtifact, publishingRequestCase))
                   .toList();
    }

    @JacocoGenerated
    //TODO: should be refactored and use PendingFile interface
    private AssociatedArtifact publishFileIfApproved(AssociatedArtifact associatedArtifact,
                                                     PublishingRequestCase publishingRequest) {
        return switch (associatedArtifact) {
            case PendingFile<?> pendingFile when publishingRequest.fileIsApproved((File) pendingFile) ->
                toPublishedFile(pendingFile, publishingRequest);
            case null, default -> associatedArtifact;
        };
    }

    private Publication fetchPublication(SortableIdentifier publicationIdentifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(publicationIdentifier)).orElseThrow();
    }

    /**
     * Creating DoiRequest for a publication necessarily owned by publication owner institution and not the institution
     * that requests the doi.
     *
     * @param publication to create a DoiRequest for
     */
    private void createDoiRequestIfNeeded(Publication publication) {
        if (hasDoi(publication) && !doiRequestExists(publication)) {
            attempt(() -> DoiRequest.fromPublication(publication)
                              .withOwner(publication.getResourceOwner().getOwner().getValue())
                              .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation())
                              .persistNewTicket(ticketService)).orElseThrow();
            logger.info(DOI_REQUEST_CREATION_MESSAGE, publication.getIdentifier());
        }
    }

    private boolean doiRequestExists(Publication publication) {
        return fetchTicket(publication).isPresent();
    }

    private Optional<DoiRequest> fetchTicket(Publication publication) {
        return ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                             publication.getIdentifier(), DoiRequest.class);
    }

    private PublishingRequestCase parseInput(String eventBlob) {
        var entryUpdate = DataEntryUpdateEvent.fromJson(eventBlob);
        return (PublishingRequestCase) entryUpdate.getNewData();
    }
}

package no.unit.nva.publication.events.handlers.tickets;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class UpdatedPublicationChannelEventHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatedPublicationChannelEventHandler.class);
    private static final String NULL_STRING = "null";

    private final S3Driver s3Driver;
    private final TicketService ticketService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public UpdatedPublicationChannelEventHandler() {
        this(
            S3Driver.defaultS3Client().build(),
            new TicketService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT, new UriRetriever()),
            ResourceService.defaultService());
    }

    public UpdatedPublicationChannelEventHandler(
        S3Client s3Client, TicketService ticketService, ResourceService resourceService) {
        super(EventReference.class);
        this.s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInputPayload(
        EventReference eventReference,
        AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> awsEventBridgeEvent,
        Context context) {
        var eventBlob = s3Driver.readEvent(eventReference.getUri());
        var entryUpdate = DataEntryUpdateEvent.fromJson(eventBlob);

        var oldData = (PublicationChannel) entryUpdate.getOldData();
        var newData = (PublicationChannel) entryUpdate.getNewData();
        if (claimedChannelAdded(oldData, newData)) {
            var publicationChannel = (ClaimedPublicationChannel) newData;
            fetchAndFilterTicketsToUpdate(publicationChannel.getResourceIdentifier())
                .map(filesApprovalEntry ->
                         filesApprovalEntry.applyPublicationChannelClaim(publicationChannel.getOrganizationId(),
                                                                         publicationChannel.getIdentifier()))
                .forEach(ticketService::updateTicket);
            LOGGER.info("Updated pending tickets based on claimed publication channel.");
        } else if (claimedChannelRemoved(oldData, newData)) {
            var publicationChannel = (ClaimedPublicationChannel) oldData;
            fetchAndFilterTicketsToUpdate(publicationChannel.getResourceIdentifier())
                .map(filesApprovalEntry -> filesApprovalEntry.clearPublicationChannelClaim(
                    publicationChannel.getIdentifier()))
                .forEach(ticketService::updateTicket);
            LOGGER.info("Updated pending tickets based on claimed publication channel removal.");
        } else {
            var action = entryUpdate.getAction();
            LOGGER.info("Ignoring event {}", dumpEvent(action, oldData, newData));
        }
        return null;
    }

    private String dumpEvent(String action, PublicationChannel oldData, PublicationChannel newData) {
        return String.format("action=%s, oldData=%s/%s, newData=%s/%s",
                             action,
                             nonNull(oldData) ? oldData.getType() : NULL_STRING,
                             nonNull(oldData) ? oldData.getIdentifier() : NULL_STRING,
                             nonNull(newData) ? newData.getType() : NULL_STRING,
                             nonNull(newData) ? newData.getIdentifier() : NULL_STRING);
    }

    private Stream<FilesApprovalEntry> fetchAndFilterTicketsToUpdate(
        SortableIdentifier resourceIdentifier) {
        var resource =
            Resource.resourceQueryObject(resourceIdentifier)
                .fetch(resourceService)
                .orElseThrow();
        return resourceService.fetchAllTicketsForResource(resource)
                   .filter(TicketEntry::isPending)
                   .filter(ticketEntry -> ticketEntry instanceof FilesApprovalEntry)
                   .map(FilesApprovalEntry.class::cast);
    }

    private boolean claimedChannelRemoved(PublicationChannel oldData, PublicationChannel newData) {
        return nonNull(oldData) && ClaimedPublicationChannel.TYPE.equals(oldData.getType()) && isNull(newData);
    }

    private static boolean claimedChannelAdded(PublicationChannel oldData, PublicationChannel newData) {
        return isNull(oldData) && nonNull(newData) && ClaimedPublicationChannel.TYPE.equals(newData.getType());
    }
}

package no.unit.nva.publication.events.handlers.tickets;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;

public class UpdatedPublicationChannelConstraintsHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private final S3Driver s3Driver;
    private final TicketService ticketService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public UpdatedPublicationChannelConstraintsHandler() {
        this(
            S3Driver.defaultS3Client().build(),
            new TicketService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT, new UriRetriever()),
            ResourceService.defaultService());
    }

    public UpdatedPublicationChannelConstraintsHandler(
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
        if (isNull(oldData)
            && nonNull(newData)
            && NonClaimedPublicationChannel.TYPE.equals(newData.getType())) {
            // do nothing
        } else if (isNull(oldData) && nonNull(newData) && ClaimedPublicationChannel.TYPE.equals(newData.getType())) {
            var channelConstraint = (ClaimedPublicationChannel) newData;
            var resource =
                Resource.resourceQueryObject(channelConstraint.getResourceIdentifier())
                    .fetch(resourceService)
                    .orElseThrow();
            resourceService.fetchAllTicketsForResource(resource)
                .filter(TicketEntry::isPending)
                .filter(ticketEntry -> ticketEntry instanceof FilesApprovalEntry)
                .forEach(ticket -> updateTicket(ticket, channelConstraint.getOrganizationId()));
        } else if (nonNull(oldData) && nonNull(newData)) {
            if (NonClaimedPublicationChannel.TYPE.equals(oldData.getType()) && ClaimedPublicationChannel.TYPE.equals(
                newData.getType())) {
                var channelConstraint = (ClaimedPublicationChannel) newData;
                var resource = Resource.resourceQueryObject(channelConstraint.getResourceIdentifier());
                resourceService.fetchAllTicketsForResource(resource)
                    .filter(TicketEntry::isPending)
                    .filter(ticketEntry -> ticketEntry instanceof FilesApprovalEntry)
                    .forEach(ticket -> updateTicket(ticket, channelConstraint.getOrganizationId()));
            } else if (ClaimedPublicationChannel.TYPE.equals(oldData.getType())
                       && NonClaimedPublicationChannel.TYPE.equals(newData.getType())) {
                var channelConstraint = (ClaimedPublicationChannel) newData;
                var resource = Resource.resourceQueryObject(channelConstraint.getResourceIdentifier());
                resourceService.fetchAllTicketsForResource(resource)
                    .filter(TicketEntry::isPending)
                    .filter(ticketEntry -> ticketEntry instanceof FilesApprovalEntry)
                    .forEach(ticket -> updateTicket(ticket, channelConstraint.getOrganizationId()));
            }
        }
        return null;
    }

    private void updateTicket(TicketEntry ticket, URI ownerAffiliation) {
        var copy = ticket.copy();
        copy.setOwnerAffiliation(ownerAffiliation);
        ticketService.updateTicket(copy);
    }
}

package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

public class PublicationChannelsUpdateHandler extends DestinationsEventBridgeEventHandler<ChannelUpdateEvent, Void> {

    private final PublicationChannelsBatchUpdateService service;

    @JacocoGenerated
    protected PublicationChannelsUpdateHandler() {
        this(new PublicationChannelsBatchUpdateService(ResourceService.defaultService()));
    }

    public PublicationChannelsUpdateHandler(PublicationChannelsBatchUpdateService service) {
        super(ChannelUpdateEvent.class);
        this.service = service;
    }

    @Override
    protected Void processInputPayload(ChannelUpdateEvent event,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<ChannelUpdateEvent>> awsEventBridgeEvent,
                                       Context context) {
        service.updateChannels(event);
        return null;
    }
}

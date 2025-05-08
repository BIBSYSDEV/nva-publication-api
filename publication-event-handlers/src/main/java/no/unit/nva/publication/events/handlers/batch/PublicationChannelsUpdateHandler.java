package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationChannelsUpdateHandler extends DestinationsEventBridgeEventHandler<ChannelUpdateEvent, Void> {

    protected static final String UPDATING_CHANNELS_FAILED_MESSAGE = "Something went wrong updating publication " +
                                                                     "channels for channel {}";
    protected static final String CONSUMED_EVENT_MESSAGE = "Consumed event {}";
    private static final Logger logger = LoggerFactory.getLogger(PublicationChannelsUpdateHandler.class);
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
        logger.info(CONSUMED_EVENT_MESSAGE, event);
        try {
            service.updateChannels(event);
        } catch (Exception e) {
            logger.error(UPDATING_CHANNELS_FAILED_MESSAGE, event.getChannelIdentifier());
        }
        return null;
    }
}

package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationChannelsUpdateHandler extends EventHandler<ChannelUpdateEvent, Void> {

    protected static final String UPDATING_CHANNELS_FAILED_MESSAGE =
        "Something went wrong updating publication channels for channel {}";
    protected static final String CONSUMED_EVENT_MESSAGE = "Consumed event {}";
    private static final Logger logger = LoggerFactory.getLogger(PublicationChannelsUpdateHandler.class);
    private final PublicationChannelsBatchUpdateService service;

    @JacocoGenerated
    public PublicationChannelsUpdateHandler() {
        this(new PublicationChannelsBatchUpdateService(ResourceService.defaultService()));
    }

    public PublicationChannelsUpdateHandler(PublicationChannelsBatchUpdateService service) {
        super(ChannelUpdateEvent.class);
        this.service = service;
    }

    @Override
    protected Void processInput(ChannelUpdateEvent channelUpdateEvent, AwsEventBridgeEvent<ChannelUpdateEvent> event,
                                Context context) {
        logger.info(CONSUMED_EVENT_MESSAGE, channelUpdateEvent);
        try {
            service.updateChannels(channelUpdateEvent);
        } catch (Exception e) {
            logger.error(UPDATING_CHANNELS_FAILED_MESSAGE, channelUpdateEvent.getChannelIdentifier());
        }
        return null;
    }
}

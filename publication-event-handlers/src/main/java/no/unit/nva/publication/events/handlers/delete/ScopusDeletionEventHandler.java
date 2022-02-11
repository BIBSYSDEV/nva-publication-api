package no.unit.nva.publication.events.handlers.delete;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopusDeletionEventHandler extends EventHandler<ScopusDeleteEventBody, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ScopusDeletionEventHandler.class);

    protected ScopusDeletionEventHandler() {
        super(ScopusDeleteEventBody.class);
    }

    @Override
    protected Void processInput(ScopusDeleteEventBody input,
                                AwsEventBridgeEvent<ScopusDeleteEventBody> event,
                                Context context) {
        logger.info("Received Event:{}", event.toJsonString());
        return null;
    }
}

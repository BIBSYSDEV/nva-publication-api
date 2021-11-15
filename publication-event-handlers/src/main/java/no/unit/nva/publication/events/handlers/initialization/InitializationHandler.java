package no.unit.nva.publication.events.handlers.initialization;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stack initialization handler. Does nothing for now.
 */
public class InitializationHandler extends EventHandler<PipelineEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(InitializationHandler.class);

    protected InitializationHandler() {
        super(PipelineEvent.class);
    }

    @Override
    protected Void processInput(PipelineEvent input, AwsEventBridgeEvent<PipelineEvent> event, Context context) {
        logger.info(event.toJsonString());
        return null;
    }
}

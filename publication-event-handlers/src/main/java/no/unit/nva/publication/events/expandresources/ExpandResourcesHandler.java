package no.unit.nva.publication.events.expandresources;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import nva.commons.core.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandResourcesHandler extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ExpandResourcesHandler.class);

    protected ExpandResourcesHandler() {
        super(DynamoEntryUpdateEvent.class);
    }

    @Override
    protected Void processInputPayload(DynamoEntryUpdateEvent input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> event,
                                       Context context) {
        String json = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(event)).orElseThrow();
        logger.info(json);
        return null;
    }
}

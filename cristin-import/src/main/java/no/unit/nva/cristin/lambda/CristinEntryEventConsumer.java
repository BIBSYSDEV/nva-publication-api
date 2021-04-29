package no.unit.nva.cristin.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;

public class CristinEntryEventConsumer extends EventHandler<CristinObject, Publication> {

    protected CristinEntryEventConsumer() {
        super(CristinObject.class);
    }

    @Override
    protected Publication processInput(CristinObject input, AwsEventBridgeEvent<CristinObject> event, Context context) {
        return input.toPublication();
    }
}

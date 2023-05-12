package no.unit.nva.publication.events.handlers.fanout;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;

public class ImportCandidateDataEntryUpdateHandler extends EventHandler<EventReference, EventReference> {

    public ImportCandidateDataEntryUpdateHandler() {
        super(EventReference.class);
    }

    @Override
    protected EventReference processInput(EventReference input, AwsEventBridgeEvent<EventReference> event,
                                          Context context) {
        return null;
    }
}

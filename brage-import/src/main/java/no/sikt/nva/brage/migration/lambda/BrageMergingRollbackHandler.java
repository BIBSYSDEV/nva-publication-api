package no.sikt.nva.brage.migration.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;

public class BrageMergingRollbackHandler extends EventHandler<EventReference, Void> {

    public final static String TOPIC = "BrageMerging.Rollback.Request";

    protected BrageMergingRollbackHandler() {
        super(EventReference.class);
    }

    @Override
    protected Void processInput(EventReference eventReference,
                                AwsEventBridgeEvent<EventReference> event,
                                Context context) {
        return null;
    }
}

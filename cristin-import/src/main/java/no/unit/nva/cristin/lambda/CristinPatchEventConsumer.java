package no.unit.nva.cristin.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;

public class CristinPatchEventConsumer extends EventHandler<EventReference, Publication> {

    public static final String TOPIC = "PublicationService.DataImport.Filename";
    public static final String SUBTOPIC = "PublicationService.CristinData.PatchEntry";

    public CristinPatchEventConsumer() {
        super(EventReference.class);
    }

    @Override
    protected Publication processInput(EventReference input, AwsEventBridgeEvent<EventReference> event,
                                       Context context) {
        return null;
    }
}

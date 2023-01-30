package no.unit.nva.publication.events.handlers.delete;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.model.events.DeleteEntryEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

public class DeletePublicationHandler extends EventHandler<DeleteEntryEvent, Void> {

    private final ResourceService resourceService;

    public DeletePublicationHandler(ResourceService resourceService) {
        super(DeleteEntryEvent.class);
        this.resourceService = resourceService;
    }

    @JacocoGenerated
    public DeletePublicationHandler() {
        this(ResourceService.defaultService());
    }

    @Override
    protected Void processInput(DeleteEntryEvent input, AwsEventBridgeEvent<DeleteEntryEvent> event, Context context) {
        attempt(() -> resourceService.updatePublishedStatusToDeleted(input.getIdentifier())).orElseThrow();
        return null;
    }
}

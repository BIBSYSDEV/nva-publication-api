package no.unit.nva.publication.events.handlers.delete;

import static java.util.Objects.nonNull;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.bodies.DeleteResourceEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

public class DeletePublicationHandler extends
                                      DestinationsEventBridgeEventHandler<DataEntryUpdateEvent,
                                                                             DeleteResourceEvent> {

    private final ResourceService resourceService;

    @JacocoGenerated
    public DeletePublicationHandler() {
        this(ResourceService.defaultService());
    }

    public DeletePublicationHandler(ResourceService resourceService) {
        super(DataEntryUpdateEvent.class);
        this.resourceService = resourceService;
    }

    @Override
    protected DeleteResourceEvent processInputPayload(DataEntryUpdateEvent input,
                                                      AwsEventBridgeEvent<AwsEventBridgeDetail<DataEntryUpdateEvent>> event,
                                                      Context context) {
        Publication publication = toPublication(input.getNewData());
        if (isDeleted(publication)) {
            return toDeletePublicationEvent(publication);
        }
        return null;
    }

    private DeleteResourceEvent toDeletePublicationEvent(Publication publication) {
        return new DeleteResourceEvent(DeleteResourceEvent.EVENT_TOPIC,
                                       publication.getIdentifier(),
                                       publication.getStatus().getValue(),
                                       publication.getDoi(),
                                       publication.getPublisher().getId());
    }

    private boolean isDeleted(Publication publication) {
        return nonNull(publication) && PublicationStatus.DELETED.equals(publication.getStatus());
    }

    private Publication toPublication(Entity dataEntry) {
        Publication publication = null;
        if (dataEntry instanceof Resource) {
            publication = dataEntry.toPublication(resourceService);
        }
        return publication;
    }
}

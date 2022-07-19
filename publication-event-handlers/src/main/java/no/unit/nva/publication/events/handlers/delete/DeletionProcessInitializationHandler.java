package no.unit.nva.publication.events.handlers.delete;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.bodies.ResourceDraftedForDeletionEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.DoiRequest;
import nva.commons.core.JacocoGenerated;

public class DeletionProcessInitializationHandler
    extends DestinationsEventBridgeEventHandler<DataEntryUpdateEvent, ResourceDraftedForDeletionEvent> {
    
    @JacocoGenerated
    public DeletionProcessInitializationHandler() {
        super(DataEntryUpdateEvent.class);
    }
    
    @Override
    protected ResourceDraftedForDeletionEvent processInputPayload(
        DataEntryUpdateEvent input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<DataEntryUpdateEvent>> event,
        Context context) {
        Publication publication = toPublication(input.getNewData());
        if (isDraftForDeletion(publication)) {
            return toDeletePublicationEvent(publication);
        }
        return null;
    }
    
    private boolean isDraftForDeletion(Publication publication) {
        return publication != null
               && publication.getStatus().equals(PublicationStatus.DRAFT_FOR_DELETION);
    }
    
    private Publication toPublication(Entity dataEntry) {
        Publication publication = null;
        if (dataEntry instanceof DoiRequest) {
            publication = dataEntry.toPublication();
        }
        return publication;
    }
    
    private ResourceDraftedForDeletionEvent toDeletePublicationEvent(Publication publication) {
        return new ResourceDraftedForDeletionEvent(
            ResourceDraftedForDeletionEvent.EVENT_TOPIC,
            publication.getIdentifier(),
            publication.getStatus().getValue(),
            publication.getDoi(),
            publication.getPublisher().getId()
        );
    }
}

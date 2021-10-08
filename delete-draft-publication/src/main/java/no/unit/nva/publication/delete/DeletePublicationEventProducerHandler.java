package no.unit.nva.publication.delete;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.DeletePublicationEvent;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import nva.commons.core.JacocoGenerated;

public class DeletePublicationEventProducerHandler
    extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, DeletePublicationEvent> {

    @JacocoGenerated
    public DeletePublicationEventProducerHandler() {
        super(DynamoEntryUpdateEvent.class);
    }

    @Override
    protected DeletePublicationEvent processInputPayload(
        DynamoEntryUpdateEvent input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> event,
        Context context) {
        Publication publication = input.getNewPublication();
        if (isDraftForDeletion(publication)) {
            return toDeletePublicationEvent(publication);
        }
        return null;
    }

    private boolean isDraftForDeletion(Publication publication) {
        return publication != null
                && publication.getStatus().equals(PublicationStatus.DRAFT_FOR_DELETION);
    }

    private DeletePublicationEvent toDeletePublicationEvent(Publication publication) {
        return new DeletePublicationEvent(
                DeletePublicationEvent.DELETE_PUBLICATION,
                publication.getIdentifier(),
                publication.getStatus().getValue(),
                publication.getDoi(),
                publication.getPublisher().getId()
        );
    }
}

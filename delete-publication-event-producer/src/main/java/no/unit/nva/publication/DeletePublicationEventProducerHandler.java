package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.DeletePublicationEvent;
import no.unit.nva.publication.events.PublicationUpdateEvent;

public class DeletePublicationEventProducerHandler
        extends DestinationsEventBridgeEventHandler<PublicationUpdateEvent, DeletePublicationEvent> {


    protected DeletePublicationEventProducerHandler() {
        super(PublicationUpdateEvent.class);
    }

    @Override
    protected DeletePublicationEvent processInputPayload(
            PublicationUpdateEvent input,
            AwsEventBridgeEvent<AwsEventBridgeDetail<PublicationUpdateEvent>> event,
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

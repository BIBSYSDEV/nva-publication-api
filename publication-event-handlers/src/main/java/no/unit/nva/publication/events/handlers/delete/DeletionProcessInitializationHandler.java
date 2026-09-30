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
import no.unit.nva.publication.model.business.Resource;

public class DeletionProcessInitializationHandler
    extends DestinationsEventBridgeEventHandler<
        DataEntryUpdateEvent, ResourceDraftedForDeletionEvent> {

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
    return publication != null && publication.getStatus() == PublicationStatus.DRAFT_FOR_DELETION;
  }

  private Publication toPublication(Entity dataEntry) {
    return dataEntry instanceof Resource resource ? resource.toPublication() : null;
  }

  private ResourceDraftedForDeletionEvent toDeletePublicationEvent(Publication publication) {
    return new ResourceDraftedForDeletionEvent(
        ResourceDraftedForDeletionEvent.EVENT_TOPIC,
        publication.getIdentifier(),
        publication.getStatus().getValue(),
        publication.getDoi(),
        publication.getPublisher().getId());
  }
}

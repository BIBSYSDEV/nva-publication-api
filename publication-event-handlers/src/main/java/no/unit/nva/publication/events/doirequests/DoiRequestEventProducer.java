package no.unit.nva.publication.events.doirequests;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.doi.update.dto.DoiRegistrarEntryFields;
import no.unit.nva.publication.doi.update.dto.PublicationHolder;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes DynamodbEvent's that's been published on EventBridge, and produces new PublicationCollection DTO with type
 * `publication.doirequest`.
 */
public class DoiRequestEventProducer
    extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, PublicationHolder> {

    public static final String TYPE_REQUEST_FOR_NEW_DRAFT_DOI = "publication.doiupdate.newdraftdoirequest";
    public static final String TYPE_UPDATE_EXISTING_DOI = "publication.doiupdate.updateexistingdoi";
    public static final String NO_RESOURCE_IDENTIFIER_ERROR = "Resource has no identifier:";

    private static final String EMPTY_EVENT_TYPE = "empty";
    public static final PublicationHolder EMPTY_EVENT = emptyEvent();
    private static final Logger logger = LoggerFactory.getLogger(DoiRequestEventProducer.class);

    @JacocoGenerated
    public DoiRequestEventProducer() {
        super(DynamoEntryUpdateEvent.class);
    }

    @Override
    protected PublicationHolder processInputPayload(
        DynamoEntryUpdateEvent input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> event,
        Context context) {

        logger.info(event.toJsonString());
        PublicationHolder updatedDoiInformationEvent = fromDynamoEntryUpdate(input);
        validate(updatedDoiInformationEvent);
        return updatedDoiInformationEvent;
    }

    private static PublicationHolder emptyEvent() {
        return new PublicationHolder(EMPTY_EVENT_TYPE, null);
    }

    private void validate(PublicationHolder updatedDoiInformationEvent) {
        if (isInvalid(updatedDoiInformationEvent)) {
            throw new IllegalStateException(NO_RESOURCE_IDENTIFIER_ERROR);
        }
    }

    private boolean isInvalid(PublicationHolder updatedDoiInformationEvent) {
        return nonNull(updatedDoiInformationEvent.getItem())
               && isNull(updatedDoiInformationEvent.getItem().getIdentifier());
    }

    private PublicationHolder fromDynamoEntryUpdate(DynamoEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
                   .filter(this::shouldPropagateEvent)
                   .map(DynamoEntryUpdateEvent::getNewData)
                   .filter(data -> data instanceof Publication)
                   .map(data -> (Publication) data)
                   .map(pub -> new PublicationHolder(calculateEventType(updateEvent), pub))
                   .orElse(EMPTY_EVENT);
    }

    private String calculateEventType(DynamoEntryUpdateEvent updateEvent) {
        if (isFirstDoiRequest(updateEvent)) {
            return TYPE_REQUEST_FOR_NEW_DRAFT_DOI;
        } else if (isDoiRequestUpdate(updateEvent)) {
            return TYPE_UPDATE_EXISTING_DOI;
        }
        return null;
    }

    private boolean isDoiRequestUpdate(DynamoEntryUpdateEvent updateEvent) {
        return resourceHasDoiRequest(updateEvent) || resourceHasDoi(updateEvent);
    }

    private boolean resourceHasDoi(DynamoEntryUpdateEvent updateEvent) {
        Publication publication = toPublication(updateEvent.getNewData());
        return nonNull(publication) && nonNull(publication.getDoi());
    }

    private Publication toPublication(ResourceUpdate resourceUpdate) {
        return resourceUpdate != null ? resourceUpdate.toPublication() : null;
    }

    private boolean resourceHasDoiRequest(DynamoEntryUpdateEvent updateEvent) {
        return nonNull(toPublication(updateEvent.getOldData()))
               && nonNull(toPublication(updateEvent.getNewData()))
               && nonNull(toPublication(updateEvent.getNewData()).getDoiRequest());
    }

    private boolean isFirstDoiRequest(DynamoEntryUpdateEvent updateEvent) {
        return isNull(toPublication(updateEvent.getOldData()))
               && updateHasDoiRequest(updateEvent)
               && isNull(toPublication(updateEvent.getNewData()).getDoi());
    }

    private boolean updateHasDoiRequest(DynamoEntryUpdateEvent updateEvent) {
        return nonNull(toPublication(updateEvent.getNewData()))
                && nonNull(toPublication(updateEvent.getNewData()).getDoiRequest());
    }

    private boolean publicationHasDoiRequest(DynamoEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
                   .map(DynamoEntryUpdateEvent::getNewData)
                   .filter(data -> data instanceof Publication)
                   .map(data -> (Publication) data)
                   .map(Publication::getDoiRequest)
                   .isPresent();
    }

    private boolean shouldPropagateEvent(DynamoEntryUpdateEvent updateEvent) {
        boolean publicationHasDoiRequest = publicationHasDoiRequest(updateEvent);
        boolean isChange = isEffectiveChange(updateEvent);

        return isChange && publicationHasDoiRequest;
    }

    private boolean isEffectiveChange(DynamoEntryUpdateEvent updateEvent) {
        var newPublication = toPublication(updateEvent.getNewData());
        var oldPublication = toPublication(updateEvent.getOldData());
        if (nonNull(newPublication)) {
            return
                doiRequestGotApproved(updateEvent)
                || newDoiRelatedMetadataDifferFromOld(newPublication, oldPublication);
        }
        return false;
    }

    private boolean doiRequestGotApproved(DynamoEntryUpdateEvent updateEvent) {
        DoiRequestStatus oldStatus = extractOldPublicationStatus(updateEvent);
        DoiRequestStatus newStatus = extractNewPublicationStatus(updateEvent);
        return DoiRequestStatus.REQUESTED.equals(oldStatus) && DoiRequestStatus.APPROVED.equals(newStatus);
    }

    private DoiRequestStatus extractNewPublicationStatus(DynamoEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
                   .map(DynamoEntryUpdateEvent::getNewData)
                   .filter(data -> data instanceof Publication)
                   .map(data -> (Publication) data)
                   .map(Publication::getDoiRequest)
                   .map(DoiRequest::getStatus)
                   .orElse(null);
    }

    private DoiRequestStatus extractOldPublicationStatus(DynamoEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
                   .map(DynamoEntryUpdateEvent::getOldData)
                   .filter(data -> data instanceof Publication)
                   .map(data -> (Publication) data)
                   .map(Publication::getDoiRequest)
                   .map(DoiRequest::getStatus)
                   .orElse(DoiRequestStatus.REQUESTED);
    }

    private boolean newDoiRelatedMetadataDifferFromOld(Publication newPublication, Publication oldPublication) {
        DoiRegistrarEntryFields doiInfoNew = DoiRegistrarEntryFields.fromPublication(newPublication);
        DoiRegistrarEntryFields doiInfoOld = extractInfoFromOldInstance(oldPublication);
        return !doiInfoNew.equals(doiInfoOld);
    }

    private DoiRegistrarEntryFields extractInfoFromOldInstance(Publication oldPublication) {
        return nonNull(oldPublication) ? DoiRegistrarEntryFields.fromPublication(oldPublication) : null;
    }
}

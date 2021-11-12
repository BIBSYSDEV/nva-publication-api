package no.unit.nva.publication.events.handlers.doirequests;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.doi.update.dto.DoiRegistrarEntryFields;
import no.unit.nva.publication.doi.update.dto.PublicationHolder;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.DataEntry;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends messages to DoiRegistrar service for creating and updating DOIs.
 */
public class DoiRequestEventProducer
    extends DestinationsEventBridgeEventHandler<DataEntryUpdateEvent, PublicationHolder> {

    public static final String REQUEST_DRAFT_DOI_EVENT_TOPIC = "PublicationService.Doi.CreationRequest";
    public static final String UPDATE_DOI_EVENT_TOPIC = "PublicationService.Doi.UpdateRequest";
    public static final String NO_RESOURCE_IDENTIFIER_ERROR = "Resource has no identifier:";

    protected static final String EMPTY_EVENT_TYPE = "empty";
    public static final PublicationHolder EMPTY_EVENT = emptyEvent();
    private static final Logger logger = LoggerFactory.getLogger(DoiRequestEventProducer.class);

    @JacocoGenerated
    public DoiRequestEventProducer() {
        super(DataEntryUpdateEvent.class);
    }

    @Override
    protected PublicationHolder processInputPayload(
        DataEntryUpdateEvent input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<DataEntryUpdateEvent>> event,
        Context context) {

        logger.info(event.toJsonString());
        PublicationHolder updatedDoiInformationEvent = fromDynamoEntryUpdate(input);
        validate(updatedDoiInformationEvent);
        return updatedDoiInformationEvent;
    }

    private static PublicationHolder emptyEvent() {
        return toPublicationHolder(null, EMPTY_EVENT_TYPE);
    }

    private static PublicationHolder toPublicationHolder(DoiRequest doiRequest, String eventType) {
        Publication publication = nonNull(doiRequest) ? doiRequest.toPublication() : null;
        return new PublicationHolder(eventType, publication);
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

    private PublicationHolder fromDynamoEntryUpdate(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
            .filter(this::shouldPropagateEvent)
            .map(DataEntryUpdateEvent::getNewData)
            .filter(data -> data instanceof DoiRequest)
            .map(data -> (DoiRequest) data)
            .map(pub -> toPublicationHolder(pub, calculateEventType(updateEvent)))
            .orElse(EMPTY_EVENT);
    }

    private String calculateEventType(DataEntryUpdateEvent updateEvent) {
        if (isFirstDoiRequest(updateEvent)) {
            return REQUEST_DRAFT_DOI_EVENT_TOPIC;
        } else {
            return UPDATE_DOI_EVENT_TOPIC;
        }
    }

    private Publication toPublication(DataEntry dataEntry) {
        return dataEntry != null ? dataEntry.toPublication() : null;
    }

    private boolean isFirstDoiRequest(DataEntryUpdateEvent updateEvent) {
        return isNull(toPublication(updateEvent.getOldData()))
               && updateHasDoiRequest(updateEvent)
               && isNull(toPublication(updateEvent.getNewData()).getDoi());
    }

    private boolean updateHasDoiRequest(DataEntryUpdateEvent updateEvent) {
        return nonNull(toPublication(updateEvent.getNewData()))
               && nonNull(toPublication(updateEvent.getNewData()).getDoiRequest());
    }

    private boolean publicationHasDoiRequest(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
            .map(DataEntryUpdateEvent::getNewData)
            .filter(data -> data instanceof DoiRequest)
            .map(data -> (DoiRequest) data)
            .isPresent();
    }

    private boolean shouldPropagateEvent(DataEntryUpdateEvent updateEvent) {
        boolean publicationHasDoiRequest = publicationHasDoiRequest(updateEvent);
        boolean isChange = isEffectiveChange(updateEvent);

        return isChange && publicationHasDoiRequest;
    }

    private boolean isEffectiveChange(DataEntryUpdateEvent updateEvent) {
        var newPublication = toPublication(updateEvent.getNewData());
        var oldPublication = toPublication(updateEvent.getOldData());
        if (nonNull(newPublication)) {
            return
                doiRequestGotApproved(updateEvent)
                || newDoiRelatedMetadataDifferFromOld(newPublication, oldPublication);
        }
        return false;
    }

    private boolean doiRequestGotApproved(DataEntryUpdateEvent updateEvent) {
        DoiRequestStatus oldStatus = extractOldPublicationStatus(updateEvent);
        DoiRequestStatus newStatus = extractNewPublicationStatus(updateEvent);
        return DoiRequestStatus.REQUESTED.equals(oldStatus) && DoiRequestStatus.APPROVED.equals(newStatus);
    }

    private DoiRequestStatus extractNewPublicationStatus(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
            .map(DataEntryUpdateEvent::getNewData)
            .filter(data -> data instanceof DoiRequest)
            .map(data -> (DoiRequest) data)
            .map(DoiRequest::getStatus)
            .orElse(null);
    }

    private DoiRequestStatus extractOldPublicationStatus(DataEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
            .map(DataEntryUpdateEvent::getOldData)
            .filter(data -> data instanceof DoiRequest)
            .map(data -> (DoiRequest) data)
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

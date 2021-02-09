package no.unit.nva.doi.event.producer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.doi.update.dto.PublicationHolder;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes DynamodbEvent's that's been published on EventBridge, and produces new PublicationCollection DTO with type
 * `publication.doirequest`.
 */
public class DynamoDbFanoutPublicationDtoProducer
    extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, PublicationHolder> {

    public static final String TYPE_REQUEST_FOR_NEW_DRAFT_DOI = "publication.doiupdate.newdraftdoirequest";
    private static final String TYPE_UPDATE_DRAFT_DOI = "publication.doiupdate.updatedraftdoi";
    public static final String TYPE_RESOURCE_WITH_FINDABLE_DOI_UPDATE = "publication.doiupdate.updatefindabledoi";
    public static final String NO_RESOURCE_IDENTIFIER_ERROR = "Resource has no identifier:";
    private static final String EMPTY_EVENT_TYPE = "empty";
    public static final PublicationHolder EMPTY_EVENT = emptyEvent();
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbFanoutPublicationDtoProducer.class);
    public static final String ILLEGA_STATE_WHILE_UPDATING_DOI = "Illegal state while updating doi: ";

    @JacocoGenerated
    public DynamoDbFanoutPublicationDtoProducer() {
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
            .map(DynamoEntryUpdateEvent::getNewPublication)
            .map(pub -> new PublicationHolder(calculateEventType(updateEvent), pub))
            .orElse(EMPTY_EVENT);
    }

    private String calculateEventType(DynamoEntryUpdateEvent updateEvent) {
        if(isNull(updateEvent.getOldPublication())){
            return TYPE_REQUEST_FOR_NEW_DRAFT_DOI;
        }
        if(nonNull(updateEvent.getOldPublication()) && isNull(updateEvent.getNewPublication().getDoi())){
            return TYPE_UPDATE_DRAFT_DOI;
        }
        if(nonNull(updateEvent.getOldPublication())&& nonNull(updateEvent.getNewPublication().getDoi())){
            return TYPE_RESOURCE_WITH_FINDABLE_DOI_UPDATE;
        }

        throw handleIllegalState(updateEvent);
    }

    private IllegalStateException handleIllegalState(DynamoEntryUpdateEvent updateEvent) {
        String json = attempt(() -> objectMapper.writeValueAsString(updateEvent)).orElseThrow();
        return new IllegalStateException(ILLEGA_STATE_WHILE_UPDATING_DOI + json);
    }

    private boolean publicationHasDoiRequest(DynamoEntryUpdateEvent updateEvent) {
        return Optional.of(updateEvent)
            .map(DynamoEntryUpdateEvent::getNewPublication)
            .map(Publication::getDoiRequest)
            .isPresent();
    }

    private boolean shouldPropagateEvent(DynamoEntryUpdateEvent updateEvent) {
        boolean publicationHasDoiRequest = publicationHasDoiRequest(updateEvent);
        boolean isChange = isEffectiveChange(updateEvent);

        return isChange && publicationHasDoiRequest;
    }

    private boolean isEffectiveChange(DynamoEntryUpdateEvent updateEvent) {
        var newPublication = updateEvent.getNewPublication();
        var oldPublication = updateEvent.getOldPublication();

        if (nonNull(newPublication)) {
            return !newPublication.equals(oldPublication);
        }
        return false;
    }
}

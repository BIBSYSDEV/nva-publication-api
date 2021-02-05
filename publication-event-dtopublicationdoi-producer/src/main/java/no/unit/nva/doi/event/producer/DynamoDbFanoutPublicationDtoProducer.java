package no.unit.nva.doi.event.producer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes DynamodbEvent's that's been published on EventBridge, and produces new PublicationCollection DTO with type
 * `doi.publication`.
 */
public class DynamoDbFanoutPublicationDtoProducer
    extends DestinationsEventBridgeEventHandler<DynamoEntryUpdateEvent, PublicationHolder> {

    public static final String TYPE_DTO_DOI_PUBLICATION = "doi.publication";
    public static final String NO_RESOURCE_IDENTIFIER_ERROR = "Resource has no identifier:";
    private static final String EMPTY_EVENT_TYPE = "empty";
    public static final PublicationHolder EMPTY_EVENT = emptyEvent();
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbFanoutPublicationDtoProducer.class);
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
            .map(pub -> new PublicationHolder(TYPE_DTO_DOI_PUBLICATION, pub))
            .orElse(EMPTY_EVENT);
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

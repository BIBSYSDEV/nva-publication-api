package no.unit.nva.publication.events.handlers.log;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.s3.S3Driver;
import software.amazon.awssdk.services.s3.S3Client;

public class PersistLogEntryEventHandler extends EventHandler<EventReference, Void> {

    private final S3Client s3Client;

    protected PersistLogEntryEventHandler(Class iclass, ObjectMapper objectMapper) {
        super(iclass, objectMapper);
    }

    @Override
    protected Void processInput(EventReference eventReference, AwsEventBridgeEvent<EventReference> awsEventBridgeEvent,
                                Context context) {
        eventReference.getUri();
        return null;
    }

    private SortableIdentifier readBlobFromS3(EventReference input) {
        return attempt(() -> new S3Driver(s3Client, EVENTS_BUCKET)).map(s3Driver -> s3Driver.readEvent(input.getUri()))
                   .map(DataEntryUpdateEvent::fromJson)
                   .map(DataEntryUpdateEvent::getNewData)
                   .map(Entity::getIdentifier)
                   .orElseThrow();
    }
}

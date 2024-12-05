package no.unit.nva.publication.events.handlers.log;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class PersistLogEntryEventHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    public static final String PERSISTING_LOG_ENTRY_MESSAGE = "Persisting log entry for event {} for resource {}";
    public static final Logger logger = LoggerFactory.getLogger(PersistLogEntryEventHandler.class);
    private final S3Client s3Client;
    private final ResourceService resourceService;

    @JacocoGenerated
    public PersistLogEntryEventHandler() {
        this(S3Driver.defaultS3Client().build(), ResourceService.defaultService());
    }

    protected PersistLogEntryEventHandler(S3Client s3Client, ResourceService resourceService) {
        super(EventReference.class);
        this.s3Client = s3Client;
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInputPayload(EventReference eventReference,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> awsEventBridgeEvent,
                                       Context context) {
        return readNewImageResourceIdentifier(eventReference)
                   .map(this::handleNewImage)
                   .orElse(null);
    }

    private Void handleNewImage(SortableIdentifier resourceIdentifier) {
        var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService);
        if (resource.hasResourceEvent()) {
            logger.info(PERSISTING_LOG_ENTRY_MESSAGE, resource.getResourceEvent().getClass().getSimpleName(),
                        resourceIdentifier);
            resource.getResourceEvent().toLogEntry(resourceIdentifier).persist(resourceService);
            resource.clearResourceEvent(resourceService);
        }
        return null;
    }

    private Optional<SortableIdentifier> readNewImageResourceIdentifier(EventReference eventReference) {
        return attempt(() -> fetchDynamoDbStreamRecord(eventReference))
                   .map(DataEntryUpdateEvent::fromJson)
                   .map(DataEntryUpdateEvent::getNewData)
                   .map(Entity::getIdentifier)
                   .toOptional();
    }

    private String fetchDynamoDbStreamRecord(EventReference eventReference) {
        return new S3Driver(s3Client, EVENTS_BUCKET).readEvent(eventReference.getUri());
    }
}

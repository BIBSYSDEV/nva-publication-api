package no.unit.nva.publication.events.handlers.log;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.log.service.LogEntryService;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class PersistLogEntryEventHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    public static final Logger logger = LoggerFactory.getLogger(PersistLogEntryEventHandler.class);
    private final S3Client s3Client;
    private final LogEntryService logEntryService;

    @JacocoGenerated
    public PersistLogEntryEventHandler() {
        this(S3Driver.defaultS3Client().build(), ResourceService.defaultService(), IdentityServiceClient.prepare(),
             CristinClient.defaultClient());
    }

    protected PersistLogEntryEventHandler(S3Client s3Client, ResourceService resourceService,
                                          IdentityServiceClient identityServiceClient, CristinClient cristinClient) {
        super(EventReference.class);
        this.s3Client = s3Client;
        this.logEntryService = new LogEntryService(resourceService, identityServiceClient, cristinClient);
    }

    @Override
    protected Void processInputPayload(EventReference eventReference,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> awsEventBridgeEvent,
                                       Context context) {

        readNewImage(eventReference).ifPresent(this::persistLogEntry);

        return null;
    }

    private void persistLogEntry(Entity entity) {
        logEntryService.persistLogEntry(entity);
    }

    private Optional<Entity> readNewImage(EventReference eventReference) {
        return attempt(() -> fetchDynamoDbStreamRecord(eventReference))
                   .map(DataEntryUpdateEvent::fromJson)
                   .map(DataEntryUpdateEvent::getNewData)
                   .toOptional();
    }

    private String fetchDynamoDbStreamRecord(EventReference eventReference) {
        return new S3Driver(s3Client, EVENTS_BUCKET).readEvent(eventReference.getUri());
    }
}

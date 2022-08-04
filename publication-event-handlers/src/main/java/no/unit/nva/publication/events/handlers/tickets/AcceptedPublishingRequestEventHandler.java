package no.unit.nva.publication.events.handlers.tickets;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingRequestStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class AcceptedPublishingRequestEventHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(AcceptedPublishingRequestEventHandler.class);
    private final ResourceService resourceService;
    private final S3Driver s3Driver;
    
    @JacocoGenerated
    public AcceptedPublishingRequestEventHandler() {
        this(PublicationServiceConfig.defaultResourceService(), S3Driver.defaultS3Client().build());
    }
    
    protected AcceptedPublishingRequestEventHandler(ResourceService resourceService, S3Client s3Client) {
        super(EventReference.class);
        this.resourceService = resourceService;
        this.s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
    }
    
    @Override
    protected Void processInputPayload(EventReference input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                       Context context) {
        var eventBlob = s3Driver.readEvent(input.getUri());
        var latestUpdate = parseInput(eventBlob);
        if (PublishingRequestStatus.COMPLETED.equals(latestUpdate.getStatus())) {
            var userInstance = UserInstance.create(latestUpdate.getOwner(), latestUpdate.getCustomerId());
            attempt(() -> resourceService.publishPublication(userInstance, latestUpdate.getResourceIdentifier()))
                .orElse(fail -> logError(fail.getException()));
        }
        return null;
    }
    
    private PublishPublicationStatusResponse logError(Exception exception) {
        logger.warn(ExceptionUtils.stackTraceInSingleLine(exception));
        return null;
    }
    
    private PublishingRequestCase parseInput(String eventBlob) {
        var entryUpdate = DataEntryUpdateEvent.fromJson(eventBlob);
        return (PublishingRequestCase) entryUpdate.getNewData();
    }
}

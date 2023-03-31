package no.unit.nva.publication.events.handlers.tickets;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;

import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.DEFAULT_S3_CLIENT;
import static nva.commons.core.attempt.Try.attempt;

public class PendingPublishingRequestEventHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(PendingPublishingRequestEventHandler.class);
    private final S3Driver s3Driver;
    private final TicketService ticketService;
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    private final ResourceService resourceService;
    
    @JacocoGenerated
    public PendingPublishingRequestEventHandler() {
        this(ResourceService.defaultService(),
             defaultPublishingRequestService(),
             DEFAULT_S3_CLIENT);
    }
    
    protected PendingPublishingRequestEventHandler(ResourceService resourceService,
                                                   TicketService ticketService,
                                                   S3Client s3Client) {
        super(EventReference.class);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
    }
    
    @Override
    protected Void processInputPayload(EventReference input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                       Context context) {
        var updateEvent = parseInput(input);
        var publishingRequest = extractPublishingRequestCaseUpdate(updateEvent);
        var workFlow = publishingRequest.getWorkflow();

        if (workFlow.registratorsAllowedToPublishDataAndMetadata()  && ticketHasNotBeenCompleted(publishingRequest)) {
            attempt(() -> ticketService.updateTicketStatus(publishingRequest, TicketStatus.COMPLETED))
                .orElseThrow();
        }
        if (workFlow.registratorsAllowedToPublishMetadata() && ticketHasNotBeenCompleted(publishingRequest)) {
                publishMetadata(publishingRequest);
        }

        return null;
    }


    private void publishMetadata(PublishingRequestCase publishingRequest) {
        var userInstance = UserInstance.create(publishingRequest.getOwner(), publishingRequest.getCustomerId());
        attempt(() -> resourceService.publishPublicationMetadata(userInstance,
                                                                 publishingRequest.extractPublicationIdentifier()))
            .orElse(fail -> logError(fail.getException()));
    }

    private PublishPublicationStatusResponse logError(Exception exception) {
        logger.warn(ExceptionUtils.stackTraceInSingleLine(exception));
        return null;
    }

    private static boolean ticketHasNotBeenCompleted(PublishingRequestCase publishingRequest) {
        return !TicketStatus.COMPLETED.equals(publishingRequest.getStatus());
    }
    
    @JacocoGenerated
    private static TicketService defaultPublishingRequestService() {
        return
            new TicketService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT);
    }
    

    private PublishingRequestCase extractPublishingRequestCaseUpdate(DataEntryUpdateEvent updateEvent) {
        return Optional.ofNullable(updateEvent)
                   .map(DataEntryUpdateEvent::getNewData)
                   .map(PublishingRequestCase.class::cast)
                   .orElseThrow();
    }
    
    private DataEntryUpdateEvent parseInput(EventReference input) {
        var blob = s3Driver.readEvent(input.getUri());
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(blob, DataEntryUpdateEvent.class)).orElseThrow();
    }

}

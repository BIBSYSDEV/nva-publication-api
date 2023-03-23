package no.unit.nva.publication.events.handlers.tickets;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class AcceptedPublishingRequestEventHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private static final Logger logger = LoggerFactory.getLogger(AcceptedPublishingRequestEventHandler.class);
    public static final String DOI_REQUEST_CREATION_MESSAGE = "Doi request has been created for publication: {}";
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final S3Driver s3Driver;
    
    @JacocoGenerated
    public AcceptedPublishingRequestEventHandler() {
        this(ResourceService.defaultService(), new TicketService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT),
             S3Driver.defaultS3Client().build());
    }
    
    protected AcceptedPublishingRequestEventHandler(ResourceService resourceService,
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
        var eventBlob = s3Driver.readEvent(input.getUri());
        var latestUpdate = parseInput(eventBlob);
        if (TicketStatus.COMPLETED.equals(latestUpdate.getStatus())) {
            var userInstance = UserInstance.create(latestUpdate.getOwner(), latestUpdate.getCustomerId());
            var publication = fetchPublication(userInstance, latestUpdate.extractPublicationIdentifier());
            attempt(() -> resourceService.publishPublication(userInstance, latestUpdate.extractPublicationIdentifier()))
                .orElse(fail -> logError(fail.getException()));
            createDoiRequestIfNeeded(publication);
        }
        return null;
    }

    private Publication fetchPublication(UserInstance userInstance, SortableIdentifier publicationIdentifier) {
        return attempt(() -> resourceService.getPublication(userInstance, publicationIdentifier))
                .orElseThrow();

    }

    private void createDoiRequestIfNeeded(Publication publication) {
        if(hasDoi(publication) && !doiRequestExists(publication)) {
            attempt(() -> createAndPersistDoiRequestForPublication(publication)).orElseThrow();
            logger.info(DOI_REQUEST_CREATION_MESSAGE, publication.getIdentifier());
        }
    }

    private TicketEntry createAndPersistDoiRequestForPublication(Publication publication) throws ApiGatewayException {
        return DoiRequest.createNewTicket(publication, DoiRequest.class, SortableIdentifier::next)
                   .persistNewTicket(ticketService);
    }

    private boolean doiRequestExists(Publication publication) {
        return fetchTicket(publication).isPresent();
    }

    private Optional<DoiRequest> fetchTicket(Publication publication) {
        return ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                             publication.getIdentifier(),
                                                             DoiRequest.class);
    }

    private static boolean hasDoi(Publication publication) {
        return nonNull(publication.getDoi());
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

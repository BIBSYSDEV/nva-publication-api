package no.unit.nva.publication.ticket.create;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.WorkFlowDto;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Map;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;

public class CreateTicketHandler extends ApiGatewayHandler<TicketDto, Void> {

    private final Logger logger = LoggerFactory.getLogger(CreateTicketHandler.class);
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String LOCATION_HEADER = "Location";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final RawContentRetriever uriRetriever;

    @JacocoGenerated
    public CreateTicketHandler() {
        this(TicketService.defaultService(), ResourceService.defaultService(),
                new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL,
                        BACKEND_CLIENT_SECRET_NAME));
    }
    
    public CreateTicketHandler(TicketService ticketService, ResourceService resourceService,
                               RawContentRetriever uriRetriever) {
        super(TicketDto.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.uriRetriever = uriRetriever;
    }
    
    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = new SortableIdentifier(requestInfo.getPathParameter("publicationIdentifier"));
        var user = UserInstance.fromRequestInfo(requestInfo);
        var publication = fetchPublication(publicationIdentifier, user);
        var newTicket = TicketEntry.requestNewTicket(publication, input.ticketType());

        if (PublishingRequestCase.class.equals(input.ticketType())) {
            var workflow = uriRetriever.getDto(publication.getPublisher().getId(), WorkFlowDto.class).get();
            ((PublishingRequestCase)newTicket).setWorkflow(workflow.getPublicationWorkflow());
        }

        var createdTicket = persistTicket(newTicket);
        var ticketLocation = createTicketLocation(publicationIdentifier, createdTicket);
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, ticketLocation));
    
        return null;
    }
    
    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }
    
    private static String createTicketLocation(SortableIdentifier publicationIdentifier, TicketEntry createdTicket) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_PATH)
                   .addChild(publicationIdentifier.toString())
                   .addChild(PublicationServiceConfig.TICKET_PATH)
                   .addChild(createdTicket.getIdentifier().toString())
                   .getUri()
                   .toString();
    }
    
    private TicketEntry persistTicket(TicketEntry newTicket) throws ApiGatewayException {
        return attempt(() -> newTicket.persistNewTicket(ticketService))
                   .orElse(fail -> handleCreationException(fail.getException(), newTicket));
    }

    @JacocoGenerated
    private TicketEntry handleCreationException(Exception exception, TicketEntry newTicket) throws ApiGatewayException {
        if (exception instanceof TransactionFailedException) {
            return updateAlreadyExistingTicket(newTicket);
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        if (exception instanceof ApiGatewayException) {
            throw (ApiGatewayException) exception;
        }
        throw new RuntimeException(exception);
    }
    
    private TicketEntry updateAlreadyExistingTicket(TicketEntry newTicket) {
        var customerId = newTicket.getCustomerId();
        var resourceIdentifier = newTicket.extractPublicationIdentifier();
        return ticketService.fetchTicketByResourceIdentifier(customerId, resourceIdentifier, newTicket.getClass())
                   .map(this::updateTicket)
                   .orElseThrow();
    }
    
    private TicketEntry updateTicket(TicketEntry ticket) {
        ticket.persistUpdate(ticketService);
        return ticket;
    }

    private Publication fetchPublication(SortableIdentifier publicationIdentifier, UserInstance user)
            throws ApiGatewayException {
        return attempt(() -> resourceService.getPublication(user, publicationIdentifier))
                .orElseThrow(fail -> loggingFailureReporter(fail.getException()));
    }

    private ApiGatewayException loggingFailureReporter(Exception exception) {
        logger.error("Request failed: {}", Arrays.toString(exception.getStackTrace()));
        return new ForbiddenException();
    }


}

package no.unit.nva.publication.publishingrequest.create;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class CreateTicketHandler extends ApiGatewayHandler<TicketDto, Void> {
    
    public static final String LOCATION_HEADER = "Location";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    
    @JacocoGenerated
    public CreateTicketHandler() {
        this(TicketService.defaultService(), ResourceService.defaultService());
    }
    
    public CreateTicketHandler(TicketService ticketService, ResourceService resourceService) {
        super(TicketDto.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
    }
    
    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = new SortableIdentifier(requestInfo.getPathParameter("publicationIdentifier"));
        var user = UserInstance.fromRequestInfo(requestInfo);
        var publication = fetchPublication(publicationIdentifier, user);
        var newTicket = TicketEntry.requestNewTicket(publication, input.ticketType());
        var createdTicket = persistTicket(newTicket);
        var ticketLocation = createTicketLocation(publicationIdentifier, createdTicket);
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, ticketLocation));
    
        return null;
    }
    
    private TicketEntry persistTicket(TicketEntry newTicket) throws ApiGatewayException {
        return attempt(() -> newTicket.persistNewTicket(ticketService))
                   .orElse(fail -> handleCreationException(fail.getException(), newTicket));
    }
    
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
        throws ForbiddenException {
        return attempt(() -> resourceService.getPublication(user, publicationIdentifier))
                   .orElseThrow(fail -> new ForbiddenException());
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
    
    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_SEE_OTHER;
    }
}

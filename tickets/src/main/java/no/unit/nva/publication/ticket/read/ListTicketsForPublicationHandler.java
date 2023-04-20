package no.unit.nva.publication.ticket.read;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketHandler;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class ListTicketsForPublicationHandler extends TicketHandler<Void, TicketCollection> {
    
    private final ResourceService resourceService;

    private final TicketService ticketService;
    
    @JacocoGenerated
    public ListTicketsForPublicationHandler() {
        this(ResourceService.defaultService(), TicketService.defaultService());
    }
    
    public ListTicketsForPublicationHandler(ResourceService resourceService, TicketService ticketService) {
        super(Void.class);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
    }
    
    @Override
    protected TicketCollection processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = extractPublicationIdentifierFromPath(requestInfo);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        
        var ticketDtos = fetchTickets(requestInfo, publicationIdentifier, userInstance);
        return TicketCollection.fromTickets(ticketDtos);
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, TicketCollection output) {
        return HttpURLConnection.HTTP_OK;
    }
    
    private static boolean userIsAuthorizedToViewOtherUsersTickets(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString());
    }
    
    private List<TicketDto> fetchTickets(RequestInfo requestInfo,
                                         SortableIdentifier publicationIdentifier,
                                         UserInstance userInstance) throws ApiGatewayException {
        var ticketEntries = userIsAuthorizedToViewOtherUsersTickets(requestInfo)
                   ? fetchTicketsForElevatedUser(userInstance, publicationIdentifier)
                   : fetchTicketsForPublicationOwner(publicationIdentifier, userInstance);

        return ticketEntries.map(this::createDto).collect(Collectors.toList());
    }
    
    private Stream<TicketEntry> fetchTicketsForPublicationOwner(SortableIdentifier publicationIdentifier,
                                                              UserInstance userInstance)
        throws ApiGatewayException {
        
        return attempt(() -> resourceService.fetchAllTicketsForPublication(userInstance, publicationIdentifier))
                          .orElseThrow(fail -> handleFetchingError(fail.getException()));
    }
    
    private Stream<TicketEntry> fetchTicketsForElevatedUser(UserInstance userInstance,
                                                          SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        
        return attempt(() -> resourceService.fetchAllTicketsForElevatedUser(userInstance, publicationIdentifier))
                .orElseThrow(fail -> handleFetchingError(fail.getException()));
    }

    private TicketDto createDto(TicketEntry ticket) {
        var messages = ticket.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticket, messages);
    }
    
    private ApiGatewayException handleFetchingError(Exception exception) {
        if (exception instanceof NotFoundException) {
            return new ForbiddenException();
        } else if (exception instanceof ApiGatewayException) {
            return (ApiGatewayException) exception;
        } else if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else {
            throw new RuntimeException(exception);
        }
    }
}

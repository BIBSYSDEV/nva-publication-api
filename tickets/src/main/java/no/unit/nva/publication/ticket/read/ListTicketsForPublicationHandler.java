package no.unit.nva.publication.ticket.read;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
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
    
    @JacocoGenerated
    public ListTicketsForPublicationHandler() {
        this(ResourceService.defaultService());
    }
    
    public ListTicketsForPublicationHandler(ResourceService resourceService) {
        super(Void.class);
        this.resourceService = resourceService;
    }
    
    @Override
    protected TicketCollection processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = extractPublicationIdentifierFromPath(requestInfo);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        
        var ticketDtos = fetchTickets(requestInfo, publicationIdentifier, userInstance);
        return TicketCollection.fromTickets(ticketDtos.collect(Collectors.toList()));
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, TicketCollection output) {
        return HttpURLConnection.HTTP_OK;
    }
    
    private static boolean userIsAuthorizedToViewOtherUsersTickets(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString());
    }
    
    private Stream<TicketDto> fetchTickets(RequestInfo requestInfo,
                                           SortableIdentifier publicationIdentifier,
                                           UserInstance userInstance) throws ApiGatewayException {
        return userIsAuthorizedToViewOtherUsersTickets(requestInfo)
                   ? fetchTicketsForElevatedUser(userInstance, publicationIdentifier)
                   : fetchTicketsForPublicationOwner(publicationIdentifier, userInstance);
    }
    
    private Stream<TicketDto> fetchTicketsForPublicationOwner(SortableIdentifier publicationIdentifier,
                                                              UserInstance userInstance)
        throws ApiGatewayException {
        
        var tickets = attempt(
            () -> resourceService.fetchAllTicketsForPublication(userInstance, publicationIdentifier))
                          .orElseThrow(fail -> handleFetchingError(fail.getException()));
        return tickets.map(TicketDto::fromTicket);
    }
    
    private Stream<TicketDto> fetchTicketsForElevatedUser(UserInstance userInstance,
                                                          SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        
        var tickets =
            attempt(() -> resourceService.fetchAllTicketsForElevatedUser(userInstance, publicationIdentifier))
                .orElseThrow(fail -> handleFetchingError(fail.getException()));
        return tickets.map(TicketDto::fromTicket);
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

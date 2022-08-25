package no.unit.nva.publication.publishingrequest.update;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.publishingrequest.TicketHandler;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public class UpdateTicketStatusHandler extends TicketHandler<TicketDto, Void> {
    
    private final TicketService ticketService;
    
    public UpdateTicketStatusHandler(TicketService ticketService) {
        super(TicketDto.class);
        this.ticketService = ticketService;
    }
    
    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var ticketIdentifier = extractTicketIdentifierFromPath(requestInfo);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);
        
        if (userIsNotAuthorized(requestInfo, ticket)) {
            throw new ForbiddenException();
        }
        
        ticketService.completeTicket(ticket);
        return null;
    }
    
    private static boolean userIsNotAuthorized(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return !(
            isAuthorizedToCompleteTickets(requestInfo)
            && isUserFromSameCustomerAsTicket(requestInfo, ticket)
        );
    }
    
    private static boolean isUserFromSameCustomerAsTicket(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return requestInfo.getCurrentCustomer().equals(ticket.getCustomerId());
    }
    
    private static boolean isAuthorizedToCompleteTickets(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString());
    }
    
    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}

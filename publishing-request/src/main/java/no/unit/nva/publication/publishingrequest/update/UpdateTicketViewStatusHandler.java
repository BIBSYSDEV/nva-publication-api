package no.unit.nva.publication.publishingrequest.update;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.TicketHandler;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public class UpdateTicketViewStatusHandler extends TicketHandler<UpdateViewStatusRequest, Void> {
    
    private final TicketService ticketService;
    
    @JacocoGenerated
    public UpdateTicketViewStatusHandler() {
        this(TicketService.defaultService());
    }
    
    public UpdateTicketViewStatusHandler(TicketService ticketService) {
        super(UpdateViewStatusRequest.class);
        this.ticketService = ticketService;
    }
    
    @Override
    protected Void processInput(UpdateViewStatusRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var ticketIdentifier = extractTicketIdentifierFromPath(requestInfo);
        if (requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString())) {
            markTicketForElevatedUser(input, requestInfo, ticketIdentifier);
        } else {
            markTicketForOwner(input, requestInfo, ticketIdentifier);
        }
        
        return null;
    }
    
    private void markTicketForElevatedUser(UpdateViewStatusRequest input,
                                           RequestInfo requestInfo,
                                           SortableIdentifier ticketIdentifier)
        throws NotFoundException, UnauthorizedException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var ticket = ticketService.fetchTicketForElevatedUser(userInstance, ticketIdentifier);
        markTicketForElevatedUser(input, ticket);
    }
    
    private void markTicketForOwner(UpdateViewStatusRequest input, RequestInfo requestInfo,
                                    SortableIdentifier ticketIdentifier)
        throws UnauthorizedException, NotFoundException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var ticket = ticketService.fetchTicket(userInstance, ticketIdentifier);
        markTicketForOwner(input, ticket);
    }
    
    private void markTicketForOwner(UpdateViewStatusRequest input, TicketEntry ticket) {
        if (ViewStatus.READ.equals(input.getViewStatus())) {
            ticket.markReadByOwner().persistUpdate(ticketService);
        } else if (ViewStatus.UNREAD.equals(input.getViewStatus())) {
            ticket.markUnreadByOwner().persistUpdate(ticketService);
        } else {
            throw new UnsupportedOperationException("Unknown ViewedStatus");
        }
    }
    
    private void markTicketForElevatedUser(UpdateViewStatusRequest input, TicketEntry ticket) {
        if (ViewStatus.READ.equals(input.getViewStatus())) {
            ticket.markReadForCurators().persistUpdate(ticketService);
        } else if (ViewStatus.UNREAD.equals(input.getViewStatus())) {
            ticket.markUnreadForCurators().persistUpdate(ticketService);
        } else {
            throw new UnsupportedOperationException("Unknown ViewedStatus");
        }
    }
    
    @Override
    protected Integer getSuccessStatusCode(UpdateViewStatusRequest input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}

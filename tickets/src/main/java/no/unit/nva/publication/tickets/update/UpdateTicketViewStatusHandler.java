package no.unit.nva.publication.tickets.update;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.tickets.TicketDto.createTicketId;
import static no.unit.nva.publication.tickets.create.CreateTicketHandler.LOCATION_HEADER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.tickets.TicketHandler;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public class UpdateTicketViewStatusHandler extends TicketHandler<UpdateViewStatusRequest, Void> {
    
    public static final String EMPTY_REQUEST_ERROR_MESSAGE = "Request must not be empty";
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
        var oldTicket = markTicket(input, requestInfo, ticketIdentifier);
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, createTicketId(oldTicket).toString()));
        return null;
    }
    
    @Override
    protected Integer getSuccessStatusCode(UpdateViewStatusRequest input, Void output) {
        return HttpURLConnection.HTTP_SEE_OTHER;
    }
    
    private static boolean elevatedUserCanViewTicket(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString());
    }
    
    private TicketEntry markTicket(UpdateViewStatusRequest input, RequestInfo requestInfo,
                                   SortableIdentifier ticketIdentifier) throws ApiGatewayException {
        if (isNull(input)) {
            throw new BadRequestException(EMPTY_REQUEST_ERROR_MESSAGE);
        }
        return elevatedUserCanViewTicket(requestInfo)
                   ? markTicketForElevatedUser(input, requestInfo, ticketIdentifier)
                   : markTicketForOwner(input, requestInfo, ticketIdentifier);
    }
    
    private TicketEntry markTicketForElevatedUser(UpdateViewStatusRequest input, RequestInfo requestInfo,
                                                  SortableIdentifier ticketIdentifier) throws ApiGatewayException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var oldTicket = fetchTicketForElevatedUser(ticketIdentifier, userInstance);
        assertThatPublicationIdentifierInPathReferencesCorrectPublication(oldTicket, requestInfo);
        markTicketForElevatedUser(input, oldTicket);
        return oldTicket;
    }
    
    private void assertThatPublicationIdentifierInPathReferencesCorrectPublication(TicketEntry ticket,
                                                                                   RequestInfo requestInfo)
        throws ForbiddenException {
        var suppliedPublicationIdentifier = extractPublicationIdentifierFromPath(requestInfo);
        if (!suppliedPublicationIdentifier.equals(ticket.extractPublicationIdentifier())) {
            throw new ForbiddenException();
        }
    }
    
    private TicketEntry fetchTicketForElevatedUser(SortableIdentifier ticketIdentifier, UserInstance userInstance)
        throws ForbiddenException {
        return attempt(() -> ticketService.fetchTicketForElevatedUser(userInstance, ticketIdentifier)).orElseThrow(
            fail -> new ForbiddenException());
    }
    
    private TicketEntry markTicketForOwner(UpdateViewStatusRequest input, RequestInfo requestInfo,
                                           SortableIdentifier ticketIdentifier)
        throws UnauthorizedException, ForbiddenException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var ticket = fetchTicketForUser(ticketIdentifier, userInstance);
        assertThatPublicationIdentifierInPathReferencesCorrectPublication(ticket, requestInfo);
        markTicketForOwner(input, ticket);
        return ticket;
    }
    
    private TicketEntry fetchTicketForUser(SortableIdentifier ticketIdentifier, UserInstance userInstance)
        throws ForbiddenException {
        return attempt(() -> ticketService.fetchTicket(userInstance, ticketIdentifier)).orElseThrow(
            fail -> new ForbiddenException());
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
}

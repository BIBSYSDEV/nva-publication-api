package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.GoneException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class GetTicketHandler extends ApiGatewayHandler<Void, TicketDto> {
    
    public static final String TICKET_NOT_FOUND = "Ticket not found";
    private final TicketService ticketService;
    
    @JacocoGenerated
    public GetTicketHandler() {
        this(TicketService.defaultService());
    }
    
    public GetTicketHandler(TicketService ticketService) {
        super(Void.class);
        this.ticketService = ticketService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected TicketDto processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var requestUtils = RequestUtils.fromRequestInfo(requestInfo);
        var ticket = ticketService.fetchTicketByIdentifier(requestUtils.ticketIdentifier());
        validateRequest(requestUtils.publicationIdentifier(), ticket);
        var messages = ticket.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticket, messages);
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, TicketDto output) {
        return HTTP_OK;
    }
    
    private static void validateRequest(SortableIdentifier publicationIdentifier, TicketEntry ticket)
        throws NotFoundException, GoneException {
        if (!ticket.getResourceIdentifier().equals(publicationIdentifier)) {
            throw new NotFoundException(TICKET_NOT_FOUND);
        }
        if (TicketStatus.REMOVED.equals(ticket.getStatus())) {
            throw new GoneException("Ticket has beem removed!");
        }
    }
}

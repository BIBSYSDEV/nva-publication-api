package no.unit.nva.publication.publishingrequest.read;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.publishingrequest.TicketUtils;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class GetTicketHandler extends ApiGatewayHandler<Void, TicketDto> {
    
    private final TicketService ticketService;
    
    public GetTicketHandler(TicketService ticketService) {
        super(Void.class);
        this.ticketService = ticketService;
    }
    
    @Override
    protected TicketDto processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var ticketIdentifier = extractTicketIdentifierFromPath(requestInfo);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);
        
        return TicketDto.fromTicket(ticket);
    }
    
    private static SortableIdentifier extractTicketIdentifierFromPath(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameter(TicketUtils.TICKET_IDENTIFIER_PATH_PARAMETER));
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, TicketDto output) {
        return HTTP_OK;
    }
}

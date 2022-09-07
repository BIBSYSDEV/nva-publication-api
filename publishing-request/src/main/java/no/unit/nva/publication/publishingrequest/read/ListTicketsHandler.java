package no.unit.nva.publication.publishingrequest.read;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.stream.Collectors;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public class ListTicketsHandler extends ApiGatewayHandler<Void, TicketCollection> {
    
    private final TicketService ticketService;
    
    public ListTicketsHandler(TicketService ticketService) {
        super(Void.class);
        this.ticketService = ticketService;
    }
    
    @Override
    protected TicketCollection processInput(Void input, RequestInfo requestInfo, Context context)
        throws UnauthorizedException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var tickets = ticketService.fetchTicketsForUser(userInstance)
                          .map(this::createDto)
                          .collect(Collectors.toList());
        return TicketCollection.fromTickets(tickets);
    }
    
    private TicketDto createDto(TicketEntry ticket) {
        var messages = ticket.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticket, messages);
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, TicketCollection output) {
        return HTTP_OK;
    }
}

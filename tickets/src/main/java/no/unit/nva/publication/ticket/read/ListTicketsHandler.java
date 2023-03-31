package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.stream.Collectors;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public class ListTicketsHandler extends ApiGatewayHandler<Void, TicketCollection> {
    
    private final TicketService ticketService;
    
    @JacocoGenerated
    public ListTicketsHandler() {
        this(TicketService.defaultService());
    }
    
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
    
    @Override
    protected Integer getSuccessStatusCode(Void input, TicketCollection output) {
        return HTTP_OK;
    }
    
    private TicketDto createDto(TicketEntry ticket) {
        var messages = ticket.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticket, messages, null);
    }
}

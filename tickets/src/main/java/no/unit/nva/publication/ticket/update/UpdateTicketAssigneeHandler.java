package no.unit.nva.publication.ticket.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketConfig;
import no.unit.nva.publication.ticket.TicketDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public class UpdateTicketAssigneeHandler extends ApiGatewayHandler<Void, TicketDto> {

    private final TicketService ticketService;

    @JacocoGenerated
    public UpdateTicketAssigneeHandler() {
        this(TicketService.defaultService()
        );
    }

    public UpdateTicketAssigneeHandler(TicketService ticketService) {
        super(Void.class);
        this.ticketService = ticketService;
    }

    @Override
    protected TicketDto processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var ticketIdentifier = extractTicketIdentifierFromPath(requestInfo);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);

        if (userIsNotAuthorized(requestInfo, ticket)) {
            throw new ForbiddenException();
        }
        var user = UserInstance.fromTicket(ticket);
        ticketService.updateTicketAssignee(ticket, user.getUser());
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, TicketDto output) {
        return HTTP_ACCEPTED;
    }

    private static SortableIdentifier extractTicketIdentifierFromPath(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameter(TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME));
    }

    private static boolean userIsNotAuthorized(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return !(isAuthorizedToClaimTickets(requestInfo) && isUserFromSameCustomerAsTicket(requestInfo, ticket));
    }

    private static boolean isUserFromSameCustomerAsTicket(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return requestInfo.getCurrentCustomer().equals(ticket.getCustomerId());
    }

    private static boolean isAuthorizedToClaimTickets(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString());
    }
}

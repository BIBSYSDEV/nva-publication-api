package no.unit.nva.publication.ticket.update;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

public class UpdateTicketAssigneeHandler extends ApiGatewayHandler<Void, TicketDto> {

    private final TicketService ticketService;
    private static final Logger logger = LoggerFactory.getLogger(UpdateTicketAssigneeHandler.class);

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
        var user = UserInstance.fromRequestInfo(requestInfo);
        ticketService.updateTicketAssignee(ticket, new Username(user.getUsername()));
        logger.info("Assignee has been set to: {}:" , user.getUsername());
        logger.info("Username from requestInfo: {}:", requestInfo.getUserName());
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

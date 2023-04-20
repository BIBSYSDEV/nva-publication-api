package no.unit.nva.publication.ticket.update;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketHandler;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class updateTicketAssigneeHandler extends TicketHandler<TicketDto, Void> {

    private final TicketService ticketService;

    @JacocoGenerated
    public updateTicketAssigneeHandler() {
        this(TicketService.defaultService()
        );
    }

    public updateTicketAssigneeHandler(TicketService ticketService) {
        super(TicketDto.class);
        this.ticketService = ticketService;
    }

    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var ticketIdentifier = extractTicketIdentifierFromPath(requestInfo);
        var user = UserInstance.fromRequestInfo(requestInfo);
        var ticket = fetchTicketForUser(requestInfo, ticketIdentifier, user);
        ticketService.updateTicketAssignee(ticket, input.getAssignee());
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }

    private TicketEntry fetchTicketForUser(RequestInfo requestInfo, SortableIdentifier ticketIdentifier,
                                           UserInstance user)
        throws ApiGatewayException {
        return isAuthorizedToClaimTickets(requestInfo)
                   ? fetchTicketForAuthorizeddUser(ticketIdentifier, user)
                   : fetchTicketForPublicationOwner(ticketIdentifier, user);
    }

    private static boolean isAuthorizedToClaimTickets(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString());
    }

    private TicketEntry fetchTicketForPublicationOwner(SortableIdentifier ticketIdentifier, UserInstance user)
        throws ApiGatewayException {
        return attempt(() -> ticketService.fetchTicket(user, ticketIdentifier))
            .orElseThrow(fail -> handleFetchingTicketForUserError(fail.getException()));
    }

    private TicketEntry fetchTicketForAuthorizeddUser(SortableIdentifier ticketIdentifier, UserInstance user)
        throws ApiGatewayException {
        return attempt(() -> ticketService.fetchTicketForElevatedUser(user, ticketIdentifier))
            .orElseThrow(fail -> handleFetchingTicketForUserError(fail.getException()));
    }

    private ApiGatewayException handleFetchingTicketForUserError(Exception exception) {
        if (exception instanceof NotFoundException) {
            return new ForbiddenException();
        } else if (exception instanceof ApiGatewayException) {
            return (ApiGatewayException) exception;
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        throw new RuntimeException(exception);
    }
}

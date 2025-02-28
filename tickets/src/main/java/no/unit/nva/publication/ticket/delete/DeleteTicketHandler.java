package no.unit.nva.publication.ticket.delete;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.RequestUtil.TICKET_IDENTIFIER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;

public class DeleteTicketHandler extends TicketHandler<Void, Void> {

    public static final String UNEXPECTED_ERROR_MESSAGE = "An unknown error occurred!";
    public static final String TICKET_NOT_FOUND_MESSAGE = "Ticket is not found!";
    private final TicketService ticketService;

    @JacocoGenerated
    public DeleteTicketHandler() {
        this(TicketService.defaultService());
    }

    protected DeleteTicketHandler(TicketService ticketService) {
        super(Void.class);
        this.ticketService = ticketService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        var userInstance = UserInstance.fromRequestInfo(requestInfo);

        attempt(() -> extractTicketIdentifier(requestInfo))
            .map(ticketService::fetchTicketByIdentifier)
            .map(ticket -> ticket.remove(userInstance))
            .forEach(ticketEntry -> ticketEntry.persistUpdate(ticketService))
            .orElseThrow(this::mapException);

        return null;
    }

    private ApiGatewayException mapException(Failure<Void> failure) {
        if (failure.getException() instanceof NotFoundException) {
            return new NotFoundException(TICKET_NOT_FOUND_MESSAGE);
        }
        if (failure.getException() instanceof ForbiddenException) {
            return (ApiGatewayException) failure.getException();
        }
        return new BadGatewayException(UNEXPECTED_ERROR_MESSAGE);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HTTP_OK;
    }

    private SortableIdentifier extractTicketIdentifier(RequestInfo requestInfo) {
        var identifierString = requestInfo.getPathParameter(TICKET_IDENTIFIER);
        return new SortableIdentifier(identifierString);
    }
}

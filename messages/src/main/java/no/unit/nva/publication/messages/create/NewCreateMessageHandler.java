package no.unit.nva.publication.messages.create;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.messages.MessageApiConfig;
import no.unit.nva.publication.messages.model.NewMessageDto;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class NewCreateMessageHandler extends ApiGatewayHandler<CreateMessageRequest, Void> {

    private final MessageService messageService;
    private final TicketService ticketService;

    @JacocoGenerated
    public NewCreateMessageHandler() {
        this(MessageService.defaultService(), TicketService.defaultService());
    }

    public NewCreateMessageHandler(MessageService messageService, TicketService ticketService) {
        super(CreateMessageRequest.class);
        this.messageService = messageService;
        this.ticketService = ticketService;
    }

    @Override
    protected Void processInput(CreateMessageRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var ticketIdentifier = extractTicketIdentifier(requestInfo);
        var user = UserInstance.fromRequestInfo(requestInfo);
        var ticket = fetchTicketForUser(requestInfo, ticketIdentifier, user);
        injectAssigneeWhenUnassignedTicket(ticket, requestInfo, user.getUser());
        var message = messageService.createMessage(ticket, user, input.getMessage());

        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, createLocationHeader(message)));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateMessageRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static boolean userIsElevatedUser(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString());
    }

    private static String createLocationHeader(Message message) {
        return NewMessageDto.constructMessageId(message).toString();
    }

    private static SortableIdentifier extractTicketIdentifier(RequestInfo requestInfo) {
        var identifierString = requestInfo.getPathParameter(MessageApiConfig.TICKET_IDENTIFIER_PATH_PARAMETER);
        return new SortableIdentifier(identifierString);
    }

    private void injectAssigneeWhenUnassignedTicket(TicketEntry ticket, RequestInfo requestInfo, User user) {
        if (isNull(ticket.getAssignee()) && userIsElevatedUser(requestInfo)) {
            ticket.setAssignee(user);
        }
    }

    private TicketEntry fetchTicketForUser(RequestInfo requestInfo, SortableIdentifier ticketIdentifier,
                                           UserInstance user)
        throws ApiGatewayException {
        return userIsElevatedUser(requestInfo)
                   ? fetchTicketForElevatedUser(ticketIdentifier, user)
                   : fetchTicketForPublicationOwner(ticketIdentifier, user);
    }

    private TicketEntry fetchTicketForPublicationOwner(SortableIdentifier ticketIdentifier, UserInstance user)
        throws ApiGatewayException {
        return attempt(() -> ticketService.fetchTicket(user, ticketIdentifier))
                   .orElseThrow(fail -> handleFetchingTicketForUserError(fail.getException()));
    }

    private TicketEntry fetchTicketForElevatedUser(SortableIdentifier ticketIdentifier, UserInstance user)
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

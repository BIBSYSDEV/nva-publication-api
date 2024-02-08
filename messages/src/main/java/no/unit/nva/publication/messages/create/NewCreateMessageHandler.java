package no.unit.nva.publication.messages.create;

import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.publication.messages.model.NewMessageDto;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.utils.RequestUtils;
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
        var requestUtils = RequestUtils.fromRequestInfo(requestInfo);
        var ticket = fetchTicketForUser(requestUtils, requestUtils.ticketIdentifier());
        isAuthorizedToManageTicket(requestUtils, ticket);
        updateStatusToPendingWhenCompletedGeneralSupportRequest(ticket);
        injectAssigneeWhenUnassignedTicket(ticket, requestUtils);
        var message = messageService.createMessage(ticket, requestUtils.toUserInstance(), input.getMessage());
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, createLocationHeader(message)));
        return null;
    }

    private static void isAuthorizedToManageTicket(RequestUtils requestUtils, TicketEntry ticket)
        throws ForbiddenException {
        if (!requestUtils.isAuthorizedToManage(ticket) && !requestUtils.isTicketOwner(ticket)) {
            throw new ForbiddenException();
        }
    }

    @Override
    protected Integer getSuccessStatusCode(CreateMessageRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static String createLocationHeader(Message message) {
        return NewMessageDto.constructMessageId(message).toString();
    }

    private static void updateStatusToPendingWhenCompletedGeneralSupportRequest(TicketEntry ticket) {
        if (ticket instanceof GeneralSupportRequest) {
            ticket.setStatus(TicketStatus.PENDING);
        }
    }

    private static void injectAssigneeWhenUnassignedTicket(TicketEntry ticket, RequestUtils requestUtils) {
        if (userCanBeSetAsAssignee(ticket, requestUtils)) {
            ticket.setAssignee(new Username(requestUtils.username()));
        }
    }

    private static boolean userCanBeSetAsAssignee(TicketEntry ticket, RequestUtils requestUtils) {
        return !ticket.hasAssignee() && !requestUtils.isTicketOwner(ticket) && requestUtils.isAuthorizedToManage(ticket);
    }

    private TicketEntry fetchTicketForUser(RequestUtils requestUtils, SortableIdentifier ticketIdentifier)
        throws ApiGatewayException {
        return requestUtils.hasOneOfAccessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                   ? fetchTicketAndValidateAccessRightsForElevatedUser(requestUtils, ticketIdentifier)
                   : fetchTicketForPublicationOwner(ticketIdentifier, requestUtils);
    }

    private TicketEntry fetchTicketAndValidateAccessRightsForElevatedUser(RequestUtils requestUtils,
                                                                          SortableIdentifier ticketIdentifier)
        throws ApiGatewayException {
        return fetchTicketForElevatedUser(ticketIdentifier, requestUtils);
    }

    private TicketEntry fetchTicketForPublicationOwner(SortableIdentifier ticketIdentifier, RequestUtils requestUtils)
        throws ApiGatewayException {
        return attempt(() -> ticketService.fetchTicket(requestUtils.toUserInstance(), ticketIdentifier))
                   .orElseThrow(fail -> handleFetchingTicketForUserError(fail.getException()));
    }

    private TicketEntry fetchTicketForElevatedUser(SortableIdentifier ticketIdentifier, RequestUtils requestUtils)
        throws ApiGatewayException {
        return attempt(() -> ticketService.fetchTicketForElevatedUser(requestUtils.toUserInstance(), ticketIdentifier))
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

package no.unit.nva.publication.messages.create;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.publication.messages.MessageApiConfig;
import no.unit.nva.publication.messages.model.NewMessageDto;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.Map;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import static nva.commons.core.attempt.Try.attempt;

public class NewCreateMessageHandler extends ApiGatewayHandler<CreateMessageRequest, Void> {

    private static final String ACCESS_RIGHT_APPROVE_PUBLISH_REQUEST = AccessRight.APPROVE_PUBLISH_REQUEST.toString();
    private static final String ACCESS_RIGHT_APPROVE_DOI_REQUEST = AccessRight.APPROVE_DOI_REQUEST.toString();
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
        injectAssigneeWhenUnassignedTicket(ticket, requestInfo);
        var message = messageService.createMessage(ticket, user, input.getMessage());

        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, createLocationHeader(message)));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateMessageRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static String createLocationHeader(Message message) {
        return NewMessageDto.constructMessageId(message).toString();
    }

    private static SortableIdentifier extractTicketIdentifier(RequestInfo requestInfo) {
        var identifierString = requestInfo.getPathParameter(MessageApiConfig.TICKET_IDENTIFIER_PATH_PARAMETER);
        return new SortableIdentifier(identifierString);
    }

    private static Username usernameFromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        return new Username(requestInfo.getUserName());
    }

    private static boolean userIsElevatedUser(RequestInfo requestInfo) {
        return userIsAuthorizedToApproveDoiRequest(requestInfo)
               || userIsAuthorizedToApprovePublishingRequest(requestInfo);
    }

    private static boolean userIsAuthorizedToApprovePublishingRequest(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(ACCESS_RIGHT_APPROVE_PUBLISH_REQUEST);
    }

    private static boolean userIsAuthorizedToApproveDoiRequest(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(ACCESS_RIGHT_APPROVE_DOI_REQUEST);
    }

    private void injectAssigneeWhenUnassignedTicket(TicketEntry ticket, RequestInfo requestInfo)
        throws UnauthorizedException {
        if (isNull(ticket.getAssignee()) && userIsElevatedUser(requestInfo)) {
            ticket.setAssignee(usernameFromRequestInfo(requestInfo));
        }
    }

    private TicketEntry fetchTicketForUser(RequestInfo requestInfo, SortableIdentifier ticketIdentifier,
                                           UserInstance user)
        throws ApiGatewayException {
        return userIsElevatedUser(requestInfo)
                   ? fetchTicketAndValidateAccessRightsForElevatedUser(requestInfo, ticketIdentifier, user)
                   : fetchTicketForPublicationOwner(ticketIdentifier, user);
    }

    private TicketEntry fetchTicketAndValidateAccessRightsForElevatedUser(RequestInfo requestInfo,
                                                                          SortableIdentifier ticketIdentifier,
                                                                          UserInstance user)
        throws ApiGatewayException {
        var ticketEntry = fetchTicketForElevatedUser(ticketIdentifier, user);
        validateAccessRightsForTicketType(requestInfo, ticketEntry.getClass());
        return ticketEntry;
    }

    private TicketEntry fetchTicketForPublicationOwner(SortableIdentifier ticketIdentifier, UserInstance user)
        throws ApiGatewayException {
        return attempt(() -> ticketService.fetchTicket(user, ticketIdentifier))
            .orElseThrow(fail -> handleFetchingTicketForUserError(fail.getException()));
    }

    private void validateAccessRightsForTicketType(RequestInfo requestInfo, Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        if (userIsNotAuthorizedToCreateMessageForDoiRequest(requestInfo, ticketType)
            || userIsNotAuthorizedToCreateMessageForPublishingRequestCase(requestInfo, ticketType)) {
            throw new ForbiddenException();
        }
    }

    private boolean userIsNotAuthorizedToCreateMessageForPublishingRequestCase(
        RequestInfo requestInfo, Class<? extends TicketEntry> ticketType) {
        return ticketType.equals(PublishingRequestCase.class)
               && userIsNotAuthorizedToApprovePublishingRequest(requestInfo);
    }

    private boolean userIsNotAuthorizedToCreateMessageForDoiRequest(
        RequestInfo requestInfo, Class<? extends TicketEntry> ticketType) {
        return ticketType.equals(DoiRequest.class) && userIsNotAuthorizedToApproveDoiRequest(requestInfo);
    }

    private boolean userIsNotAuthorizedToApprovePublishingRequest(RequestInfo requestInfo) {
        return !userIsAuthorizedToApprovePublishingRequest(requestInfo);
    }

    private boolean userIsNotAuthorizedToApproveDoiRequest(RequestInfo requestInfo) {
        return !userIsAuthorizedToApproveDoiRequest(requestInfo);
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

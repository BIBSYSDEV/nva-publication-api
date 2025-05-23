package no.unit.nva.publication.messages.create;

import static no.unit.nva.model.PublicationOperation.DOI_REQUEST_CREATE;
import static no.unit.nva.model.PublicationOperation.PARTIAL_UPDATE;
import static no.unit.nva.model.PublicationOperation.PUBLISHING_REQUEST_CREATE;
import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.model.Username;
import no.unit.nva.publication.messages.model.NewMessageDto;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class NewCreateMessageHandler extends ApiGatewayHandler<CreateMessageRequest, Void> {

    private final MessageService messageService;
    private final TicketService ticketService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public NewCreateMessageHandler() {
        this(MessageService.defaultService(), TicketService.defaultService(), ResourceService.defaultService(), new Environment());
    }

    public NewCreateMessageHandler(MessageService messageService, TicketService ticketService,
                                   ResourceService resourceService, Environment environment) {
        super(CreateMessageRequest.class, environment);
        this.messageService = messageService;
        this.ticketService = ticketService;
        this.resourceService = resourceService;
    }

    @Override
    protected void validateRequest(CreateMessageRequest createMessageRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(CreateMessageRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var requestUtils = RequestUtils.fromRequestInfo(requestInfo);
        var resource = resourceService.getResourceByIdentifier(requestUtils.publicationIdentifier());
        var ticket = ticketService.fetchTicketByIdentifier(requestUtils.ticketIdentifier());
        var permissions = fetchPermissions(requestInfo, resource);
        isAuthorizedToManageTicket(permissions, ticket);
        updateStatusToPendingWhenCompletedGeneralSupportRequest(ticket);
        injectAssigneeWhenUnassignedTicket(ticket, requestUtils);
        var message = messageService.createMessage(ticket, requestUtils.toUserInstance(), input.getMessage());
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, createLocationHeader(message)));
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateMessageRequest input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static void userHasPermissionToCreateMessageForTicket(PublicationPermissions permissionStrategy,
                                                                     TicketEntry ticketDto)
        throws UnauthorizedException {
        switch (ticketDto) {
            case DoiRequest ignored -> permissionStrategy.authorize(DOI_REQUEST_CREATE);
            case FilesApprovalEntry ignored -> permissionStrategy.authorize(PUBLISHING_REQUEST_CREATE);
            case GeneralSupportRequest ignored -> permissionStrategy.authorize(PARTIAL_UPDATE);
            default -> throw new IllegalStateException("Unexpected value: " + ticketDto);
        }
    }

    private static String createLocationHeader(Message message) {
        return NewMessageDto.constructMessageId(message).toString();
    }

    private static void updateStatusToPendingWhenCompletedGeneralSupportRequest(TicketEntry ticket) {
        if (ticket instanceof GeneralSupportRequest) {
            ticket.setStatus(TicketStatus.PENDING);
        }
    }

    private PublicationPermissions fetchPermissions(RequestInfo requestInfo, Resource resource)
        throws UnauthorizedException {
        return PublicationPermissions.create(resource, UserInstance.fromRequestInfo(requestInfo));
    }

    private void isAuthorizedToManageTicket(PublicationPermissions permissions, TicketEntry ticket)
        throws UnauthorizedException {
        userHasPermissionToCreateMessageForTicket(permissions, ticket);
    }

    private void injectAssigneeWhenUnassignedTicket(TicketEntry ticket, RequestUtils requestUtils) {
        if (userCanBeSetAsAssignee(ticket, requestUtils)) {
            ticket.setAssignee(new Username(requestUtils.username()));
        }
    }

    private boolean userCanBeSetAsAssignee(TicketEntry ticket, RequestUtils requestUtils) {
        return !ticket.hasAssignee() && !requestUtils.isTicketOwner(ticket) &&
               requestUtils.isAuthorizedToManage(ticket);
    }
}

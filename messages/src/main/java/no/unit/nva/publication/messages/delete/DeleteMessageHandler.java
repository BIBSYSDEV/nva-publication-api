package no.unit.nva.publication.messages.delete;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.messages.MessageApiConfig.MESSAGE_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.messages.MessageApiConfig.TICKET_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.model.business.MessageStatus.DELETED;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;

public class DeleteMessageHandler extends ApiGatewayHandler<Void, Void> {

    public static final String SOMETHING_WENT_WRONG_MESSAGE = "An unknown exception occurred!";
    public static final String MESSAGE_NOT_FOUND = "Message not found!";
    private final MessageService messageService;
    private final TicketService ticketService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public DeleteMessageHandler() {
        this(MessageService.defaultService(), TicketService.defaultService(), ResourceService.defaultService(),
             new Environment());
    }

    public DeleteMessageHandler(MessageService messageService, TicketService ticketService,
                                ResourceService resourceService, Environment environment) {
        super(Void.class, environment);
        this.messageService = messageService;
        this.ticketService = ticketService;
        this.resourceService = resourceService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) {
        //Do nothing
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        attempt(() -> extractMessageIdentifier(requestInfo)).map(this::fetchMessage)
            .forEach(message -> messageService.deleteMessage(userInstance, message))
            .orElseThrow(this::mapException);

        var ticketIdentifier = getTicketIdentifier(requestInfo);
        completeTicketWhenGeneralSupportWithNoActiveMessages(ticketIdentifier, userInstance);

        return null;
    }

    private static SortableIdentifier getTicketIdentifier(RequestInfo requestInfo) {
        return new SortableIdentifier(requestInfo.getPathParameter(TICKET_IDENTIFIER_PATH_PARAMETER));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HTTP_OK;
    }

    private static boolean allMessagesAreDeleted(List<Message> messages) {
        return messages.stream().allMatch(message -> DELETED.equals(message.getStatus()));
    }

    private void completeTicketWhenGeneralSupportWithNoActiveMessages(SortableIdentifier ticketIdentifier,
                                                                      UserInstance userInstance)
        throws NotFoundException {
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);
        if (ticket instanceof GeneralSupportRequest) {
            var messages = ticket.fetchMessages(ticketService);
            if (allMessagesAreDeleted(messages)) {
                var resource = getResource(ticket.getResourceIdentifier());
                ticket.complete(resource.toPublication(), userInstance).persistUpdate(ticketService);
            }
        }
    }

    private Resource getResource(SortableIdentifier resourceIdentifier) {
        return Resource.resourceQueryObject(resourceIdentifier)
                   .fetch(resourceService)
                   .orElseThrow();
    }

    private Message fetchMessage(SortableIdentifier messageIdentifier) throws NotFoundException {
        return messageService.getMessageByIdentifier(messageIdentifier)
                   .orElseThrow(() -> new NotFoundException(MESSAGE_NOT_FOUND));
    }

    private ApiGatewayException mapException(Failure<Void> failure) {
        return switch (failure.getException()) {
            case NotFoundException ex -> ex;
            case UnauthorizedException ex -> ex;
            default -> new BadGatewayException(SOMETHING_WENT_WRONG_MESSAGE);
        };
    }

    private SortableIdentifier extractMessageIdentifier(RequestInfo requestInfo) {
        var identifierString = requestInfo.getPathParameter(MESSAGE_IDENTIFIER_PATH_PARAMETER);
        return new SortableIdentifier(identifierString);
    }
}

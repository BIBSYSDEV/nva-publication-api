package no.unit.nva.publication.messages.update;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.messages.MessageApiConfig.MESSAGE_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.messages.MessageApiConfig.MESSAGE_NOT_FOUND;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.function.Function;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.service.impl.MessageService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.attempt.Try;

public class UpdateMessageHandler extends ApiGatewayHandler<UpdateMessageRequest, MessageDto> {
    
    private final MessageService messageService;
    
    public UpdateMessageHandler(MessageService messageService) {
        super(UpdateMessageRequest.class);
        this.messageService = messageService;
    }
    
    @Override
    protected MessageDto processInput(UpdateMessageRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var message = fetchMessage(requestInfo);
        return markMessageAsRead(requestInfo, message);
    }
    
    @Override
    protected Integer getSuccessStatusCode(UpdateMessageRequest input, MessageDto output) {
        return HTTP_OK;
    }
    
    private MessageDto markMessageAsRead(RequestInfo requestInfo, Message message)
        throws UnauthorizedException, ForbiddenException, NotFoundException {
        if (callerIsNotTheMessageRecipient(message, requestInfo)) {
            throw new ForbiddenException();
        }
        var updatedMessage = messageService.markAsRead(message);
        return MessageDto.fromMessage(updatedMessage);
    }
    
    private boolean callerIsNotTheMessageRecipient(Message message, RequestInfo requestInfo)
        throws UnauthorizedException {
        return !callerIsTheMessageRecipient(message, requestInfo);
    }
    
    private boolean callerIsTheMessageRecipient(Message message, RequestInfo requestInfo) throws UnauthorizedException {
        return callerIsTheOwnerAndTheRecipient(message, requestInfo)
               ||callerIsCuratorAndSupportIsTheRecipient(message, requestInfo);
    }
    
    private boolean callerIsCuratorAndSupportIsTheRecipient(Message message, RequestInfo requestInfo) {
        return callerIsCurator(requestInfo) && supportIsTheRecipient(message);
    }
    
    private boolean supportIsTheRecipient(Message message) {
        return Message.SUPPORT_SERVICE_RECIPIENT.equals(message.getRecipient());
    }
    
    private boolean callerIsCurator(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.toString());
    }
    
    private boolean callerIsTheOwnerAndTheRecipient(Message message, RequestInfo requestInfo)
        throws UnauthorizedException {
        return callerIsTheOwner(message, requestInfo) && callerIsTheRecipient(message, requestInfo);
    }
    
    private boolean callerIsTheRecipient(Message message, RequestInfo requestInfo) throws UnauthorizedException {
        return requestInfo.getNvaUsername().equals(message.getRecipient());
    }
    
    private boolean callerIsTheOwner(Message message, RequestInfo requestInfo) throws UnauthorizedException {
        return requestInfo.getNvaUsername().equals(message.getOwner());
    }
    
    private Message fetchMessage(RequestInfo requestInfo) throws NotFoundException {
        return extractMessageIdentifier(requestInfo)
            .map(messageService::getMessageByIdentifier)
            .toOptional()
            .flatMap(Function.identity())
            .orElseThrow(() -> new NotFoundException(MESSAGE_NOT_FOUND));
    }
    
    private Try<SortableIdentifier> extractMessageIdentifier(RequestInfo requestInfo) {
        return attempt(() -> requestInfo.getPathParameter(MESSAGE_IDENTIFIER_PATH_PARAMETER))
            .map(SortableIdentifier::new);
    }
}

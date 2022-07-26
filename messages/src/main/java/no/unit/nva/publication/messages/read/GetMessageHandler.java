package no.unit.nva.publication.messages.read;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_CLOCK;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.messages.MessageApiConfig;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.impl.MessageService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class GetMessageHandler extends ApiGatewayHandler<Void, MessageDto> {
    
    public static final String MESSAGE_NOT_FOUND = "Could not find message";
    private final MessageService messageService;
    
    @JacocoGenerated
    public GetMessageHandler() {
        this(new MessageService(DEFAULT_DYNAMODB_CLIENT, DEFAULT_CLOCK));
    }
    
    protected GetMessageHandler(MessageService messageService) {
        super(Void.class);
        this.messageService = messageService;
    }
    
    @Override
    protected MessageDto processInput(Void input, RequestInfo requestInfo, Context context) throws NotFoundException {
        var messageIdentifier = extractIdentifier(requestInfo);
        return messageService.getMessageByIdentifier(messageIdentifier)
            .map(MessageDto::fromMessage)
            .filter(message -> clientIsAuthorizedToSeeTheMessage(message, requestInfo))
            .orElseThrow(() -> new NotFoundException(MESSAGE_NOT_FOUND));
    }
    
    @Override
    protected Integer getSuccessStatusCode(Void input, MessageDto output) {
        return HTTP_OK;
    }
    
    private boolean clientIsAuthorizedToSeeTheMessage(MessageDto message, RequestInfo requestInfo) {
        return userIsCurator(requestInfo)
               || userIsPublicationOwner(message, requestInfo)
               || requestInfo.clientIsInternalBackend();
    }
    
    private boolean userIsPublicationOwner(MessageDto message, RequestInfo requestInfo) {
        return attempt(() -> message.getOwnerIdentifier().equals(requestInfo.getNvaUsername()))
            .orElse(fail -> false);
    }
    
    private boolean userIsCurator(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.toString());
    }
    
    private SortableIdentifier extractIdentifier(RequestInfo requestInfo) throws NotFoundException {
        return attempt(() -> new SortableIdentifier(
            requestInfo.getPathParameter(MessageApiConfig.MESSAGE_IDENTIFIER_PATH_PARAMETER)))
            .orElseThrow(fail -> new NotFoundException(MESSAGE_NOT_FOUND));
    }
}

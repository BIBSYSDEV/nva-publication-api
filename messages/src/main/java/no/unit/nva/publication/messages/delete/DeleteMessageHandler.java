package no.unit.nva.publication.messages.delete;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.messages.MessageApiConfig.MESSAGE_IDENTIFIER_PATH_PARAMETER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;

public class DeleteMessageHandler extends ApiGatewayHandler<Void, Void> {

    public static final String SOMETHING_WENT_WRONG_MESSAGE = "An unknown exception occurred!";
    public static final String MESSAGE_NOT_FOUND = "Message not found!";
    private final MessageService messageService;

    @JacocoGenerated
    public DeleteMessageHandler() {
        this(MessageService.defaultService());
    }

    public DeleteMessageHandler(MessageService messageService) {
        super(Void.class);
        this.messageService = messageService;
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        var userInstance = UserInstance.fromRequestInfo(requestInfo);

        attempt(() -> extractMessageIdentifier(requestInfo))
            .map(this::fetchMessage)
            .forEach(message -> messageService.deleteMessage(userInstance, message))
            .orElseThrow(this::mapException);

        return null;
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

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HTTP_OK;
    }
}

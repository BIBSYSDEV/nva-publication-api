package no.unit.nva.publication.messages.read;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.model.MessageDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class GetMessageHandler extends ApiGatewayHandler<Void, MessageDto> {
    
    public static final String MESSAGE_NOT_FOUND = "Could not find message";
    
    public GetMessageHandler() {
        super(Void.class);
    }
    
    @Override
    protected MessageDto processInput(Void input, RequestInfo requestInfo, Context context) throws NotFoundException {
        throw new NotFoundException(MESSAGE_NOT_FOUND);
    }
    
    //TODO: remove JacocoGenerated annotation when more functionality has been added
    @JacocoGenerated
    @Override
    protected Integer getSuccessStatusCode(Void input, MessageDto output) {
        return HTTP_OK;
    }
}

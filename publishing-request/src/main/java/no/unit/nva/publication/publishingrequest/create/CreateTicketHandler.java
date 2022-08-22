package no.unit.nva.publication.publishingrequest.create;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.publication.publishingrequest.TicketDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class CreateTicketHandler extends ApiGatewayHandler<TicketDto,Void> {
    
    public CreateTicketHandler() {
        super(TicketDto.class);
    }
    
    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return null;
    }
    
    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}

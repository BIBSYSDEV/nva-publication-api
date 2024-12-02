package no.unit.nva.publication.log.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;

public class FetchPublicationLogHandler extends ApiGatewayHandler<Void, PublicationLogResponse> {

    public FetchPublicationLogHandler() {
        super(Void.class);
    }

    @Override
    protected void validateRequest(Void input, RequestInfo requestInfo, Context context) {
        //NO OP
    }

    @Override
    protected PublicationLogResponse processInput(Void input, RequestInfo requestInfo, Context context) {
        return new PublicationLogResponse(List.of());
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PublicationLogResponse publicationLogResponse) {
        return HTTP_OK;
    }
}

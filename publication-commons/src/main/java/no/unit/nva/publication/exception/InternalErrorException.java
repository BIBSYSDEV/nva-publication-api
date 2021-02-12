package no.unit.nva.publication.exception;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class InternalErrorException extends ApiGatewayException {
    
    public InternalErrorException(Exception exception) {
        super(exception);
    }
    
    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_INTERNAL_ERROR;
    }
}

package no.unit.nva.publication.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

public class GatewayTimeoutException extends ApiGatewayException {

    public GatewayTimeoutException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_GATEWAY_TIMEOUT;
    }
}

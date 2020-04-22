package no.unit.publication.exception;

import nva.commons.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

public class NoResponseException extends ApiGatewayException {

    public NoResponseException(String message, Exception exception) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_SERVICE_UNAVAILABLE;
    }
}
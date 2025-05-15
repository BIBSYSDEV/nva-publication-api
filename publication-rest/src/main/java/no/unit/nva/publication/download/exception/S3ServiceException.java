package no.unit.nva.publication.download.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

public class S3ServiceException extends ApiGatewayException {

    public S3ServiceException(String message, Exception exception) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_BAD_GATEWAY;
    }
}
package no.unit.nva.publication.download.exception;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class S3ServiceException extends ApiGatewayException {

    public S3ServiceException(String message, Exception exception) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HTTP_BAD_GATEWAY;
    }
}
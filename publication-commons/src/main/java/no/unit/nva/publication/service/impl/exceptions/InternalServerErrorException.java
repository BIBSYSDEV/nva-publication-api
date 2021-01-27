package no.unit.nva.publication.service.impl.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class InternalServerErrorException extends ApiGatewayException {

    public InternalServerErrorException(Exception exception) {
        super(exception);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_INTERNAL_ERROR;
    }
}

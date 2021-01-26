package no.unit.nva.publication.service.impl.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class DoiRequestForNonExistingResourceException extends ApiGatewayException {

    public DoiRequestForNonExistingResourceException(Exception exception) {
        super(exception);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}

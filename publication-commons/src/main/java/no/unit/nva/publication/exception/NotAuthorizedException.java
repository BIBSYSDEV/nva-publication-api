package no.unit.nva.publication.exception;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class NotAuthorizedException extends ApiGatewayException {

    public static final String DEFAULT_MESSAGE = "Unauthorized";

    public NotAuthorizedException() {
        super(DEFAULT_MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_UNAUTHORIZED;
    }
}

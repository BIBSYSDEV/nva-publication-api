package no.unit.nva.publication.service.impl.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class ResourceCannotBeDeletedException extends ApiGatewayException {

    public static final String DEFAULT_MESSAGE = "Resource cannot be deleted: ";

    public ResourceCannotBeDeletedException(String resourceIdentifier) {
        super(DEFAULT_MESSAGE + resourceIdentifier);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}

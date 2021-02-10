package no.unit.nva.publication.exception;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class TransactionFailedException extends ApiGatewayException {

    public TransactionFailedException(Exception exception) {
        super(exception);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}

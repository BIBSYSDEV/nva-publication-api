package no.unit.nva.publication.exception;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class TransactionFailedException extends ApiGatewayException {

    public static final String ERROR_MESSAGE = "Conflict: This error is thrown when the transaction could not be "
                                               + "completed. In most cases this is because uniqueness conditions did "
                                               + "not hold (Typically a duplicate DoiRequest or PublishingRequest)";

    public TransactionFailedException(Exception exception) {
        super(exception, ERROR_MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_CONFLICT;
    }
}

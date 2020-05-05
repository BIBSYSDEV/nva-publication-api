package no.unit.nva.publication.exception;

import nva.commons.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

import java.util.List;

public class InvalidPublicationException extends ApiGatewayException {

    public static final String ERROR_MESSAGE_TEMPLATE =
            "The Publication cannot be published because the following fields are not populated: %s";

    public InvalidPublicationException(List<String> missingFields) {
        super(String.format(ERROR_MESSAGE_TEMPLATE, String.join(", ", missingFields)));
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_CONFLICT;
    }
}

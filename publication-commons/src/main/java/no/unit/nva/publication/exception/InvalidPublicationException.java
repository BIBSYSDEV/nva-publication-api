package no.unit.nva.publication.exception;

import java.util.List;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

public class InvalidPublicationException extends ApiGatewayException {
    
    public static final String ERROR_MESSAGE_TEMPLATE =
        "The Publication cannot be published because the following fields are not populated: ";
    
    public InvalidPublicationException(String message) {
        super(message);
    }
    
    public InvalidPublicationException(List<String> missingFields) {
        super(ERROR_MESSAGE_TEMPLATE + String.join(", ", missingFields));
    }
    
    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_CONFLICT;
    }
}

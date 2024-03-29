package no.unit.nva.publication.exception;

import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class DynamoDBException extends ApiGatewayException {
    
    public DynamoDBException(Exception exception) {
        super(exception);
    }
    
    public DynamoDBException(String message, Exception exception) {
        super(exception, message);
    }
    
    @Override
    protected Integer statusCode() {
        return SC_BAD_GATEWAY;
    }
}

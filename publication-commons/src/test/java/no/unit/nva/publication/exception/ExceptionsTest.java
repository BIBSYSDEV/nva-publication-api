package no.unit.nva.publication.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExceptionsTest {
    
    public static final String MESSAGE = "Message";
    
    @Test
    void dynamoDBExceptionHasStatusCode() {
        ApiGatewayException exception = new DynamoDBException(MESSAGE, new RuntimeException());
        Assertions.assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.getStatusCode());
    }
    
    @Test
    void errorResponseExceptionHasStatusCode() {
        ApiGatewayException exception = new ErrorResponseException(MESSAGE);
        Assertions.assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.getStatusCode());
    }
    
    @Test
    void noResponseExceptionHasStatusCode() {
        ApiGatewayException exception = new NoResponseException(MESSAGE, new RuntimeException());
        Assertions.assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, exception.getStatusCode());
    }
    
    @Test
    void inputExceptionHasStatusCode() {
        ApiGatewayException exception = new BadRequestException(MESSAGE, new RuntimeException());
        Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getStatusCode());
    }
    
    @Test
    void notFoundExceptionHasStatusCode() {
        ApiGatewayException exception = new NotFoundException(MESSAGE);
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, exception.getStatusCode());
    }
    
    @Test
    void notImplementedExceptionHasStatusCode() {
        ApiGatewayException exception = new NotImplementedException();
        Assertions.assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, exception.getStatusCode());
    }
}

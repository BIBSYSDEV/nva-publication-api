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
    void inputExceptionHasStatusCode() {
        ApiGatewayException exception = new BadRequestException(MESSAGE, new RuntimeException());
        Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getStatusCode());
    }
    
    @Test
    void notFoundExceptionHasStatusCode() {
        ApiGatewayException exception = new NotFoundException(MESSAGE);
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, exception.getStatusCode());
    }

}

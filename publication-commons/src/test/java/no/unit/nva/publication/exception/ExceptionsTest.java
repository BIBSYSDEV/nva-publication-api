package no.unit.nva.publication.exception;

import nva.commons.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionsTest {

    public static final String MESSAGE = "Message";

    @Test
    public void errorResponseExceptionHasStatusCode() {
        ApiGatewayException exception = new ErrorResponseException(MESSAGE);
        Assertions.assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    public void noResponseExceptionHasStatusCode() {
        ApiGatewayException exception = new NoResponseException(MESSAGE, new RuntimeException());
        Assertions.assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    public void inputExceptionHasStatusCode() {
        ApiGatewayException exception = new InputException(MESSAGE, new RuntimeException());
        Assertions.assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void notFoundExceptionHasStatusCode() {
        ApiGatewayException exception = new NotFoundException(MESSAGE);
        Assertions.assertEquals(HttpStatus.SC_NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void notImplementedExceptionHasStatusCode() {
        ApiGatewayException exception = new NotImplementedException();
        Assertions.assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, exception.getStatusCode());
    }

}

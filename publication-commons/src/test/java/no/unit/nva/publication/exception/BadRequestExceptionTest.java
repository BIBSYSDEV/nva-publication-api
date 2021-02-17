package no.unit.nva.publication.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

public class BadRequestExceptionTest {
    
    @Test
    public void exceptionReturnsStatusCodeIndicatingUnknownInternalError() {
        Exception cause = new Exception();
        BadRequestException exception = new BadRequestException(cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }
    
    @Test
    public void exceptionContainsSuppliedMessage() {
        String message = "someMessage";
        BadRequestException exception = new BadRequestException(message);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(exception.getMessage(), is(equalTo(message)));
    }
}
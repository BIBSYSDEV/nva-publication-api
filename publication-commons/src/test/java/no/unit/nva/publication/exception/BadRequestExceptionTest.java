package no.unit.nva.publication.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

public class BadRequestExceptionTest {
    
    public static final String SOME_MESSAGE = "someMessage";
    
    @Test
    public void badRequestExceptionReturnsBadRequestCode() {
        BadRequestException exception = new BadRequestException(SOME_MESSAGE);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
}
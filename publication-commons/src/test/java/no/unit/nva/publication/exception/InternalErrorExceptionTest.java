package no.unit.nva.publication.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

public class InternalErrorExceptionTest {
    
    @Test
    public void internalServerErrorReturnsTheRespectiveCode() {
        InternalErrorException exception = new InternalErrorException(new Exception());
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
    }
}
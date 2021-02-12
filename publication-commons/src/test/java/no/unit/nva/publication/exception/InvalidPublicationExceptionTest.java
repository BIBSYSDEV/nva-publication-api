package no.unit.nva.publication.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

public class InvalidPublicationExceptionTest {
    
    public static final String SOME_MESSAGE = "some message";
    
    @Test
    public void invalidPublicationExceptionReturnsCodeIndicatingConflictWithInternalState() {
        InvalidPublicationException exception = new InvalidPublicationException(SOME_MESSAGE);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
    }
}
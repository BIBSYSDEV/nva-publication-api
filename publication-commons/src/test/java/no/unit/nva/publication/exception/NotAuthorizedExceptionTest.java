package no.unit.nva.publication.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

public class NotAuthorizedExceptionTest {
    
    @Test
    public void getStatusCodeReturnsNotAuthorized() {
        NotAuthorizedException exception = new NotAuthorizedException();
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }
}
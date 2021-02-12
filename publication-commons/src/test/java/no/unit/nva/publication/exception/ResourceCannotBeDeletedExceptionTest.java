package no.unit.nva.publication.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

public class ResourceCannotBeDeletedExceptionTest {
    
    public static final String SOME_STRING = "Some string";
    
    @Test
    public void resourceCannotBeDeletedExceptionReturnsIndicationsThatTheClientHasMadeAnIllegalRequest() {
        ResourceCannotBeDeletedException exception = new ResourceCannotBeDeletedException(SOME_STRING);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
}
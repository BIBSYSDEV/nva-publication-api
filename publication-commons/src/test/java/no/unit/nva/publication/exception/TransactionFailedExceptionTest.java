package no.unit.nva.publication.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

class TransactionFailedExceptionTest {

    @Ignore
    @Test
    void getStatusCodeReturnsBadRequest() {
        TransactionFailedException exception = new TransactionFailedException(new Exception());
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
    }
}
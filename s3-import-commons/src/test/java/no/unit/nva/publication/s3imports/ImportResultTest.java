package no.unit.nva.publication.s3imports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.junit.jupiter.api.Test;

public class ImportResultTest {

    @Test
    public void reportFailureReturnsFailureReport() {
        String someInput = "someInput";
        String exceptionMessage = "someMessage";
        ImportResult<String> failure = ImportResult.reportFailure(someInput, new Exception(exceptionMessage));
        assertThat(failure.getStatus(), is(equalTo(ImportResult.FAILURE)));
    }
}
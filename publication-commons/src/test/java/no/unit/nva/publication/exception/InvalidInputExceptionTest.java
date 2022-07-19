package no.unit.nva.publication.exception;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.junit.jupiter.api.Test;

class InvalidInputExceptionTest {
    
    @Test
    public void exceptionContainsSuppliedMessage() {
        String expectedMessage = "someMessage";
        var exception = new InvalidInputException(expectedMessage);
        assertThat(exception.getMessage(), is(equalTo(expectedMessage)));
    }
}
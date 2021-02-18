package no.unit.nva.publication.storage.model.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.junit.jupiter.api.Test;

public class EmptyValueMapExceptionTest {
    
    @Test
    public void exceptionHasDefaultMessage() {
        EmptyValueMapException exception = new EmptyValueMapException();
        assertThat(exception.getMessage(), is(equalTo(EmptyValueMapException.DEFAULT_MESSAGE)));
    }
}
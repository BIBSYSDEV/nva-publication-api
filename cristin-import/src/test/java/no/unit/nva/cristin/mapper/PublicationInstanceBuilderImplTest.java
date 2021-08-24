package no.unit.nva.cristin.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PublicationInstanceBuilderImplTest {

    @Test
    public void publicationInstanceBuilderConstructorThrowsNullPointerExceptionIfParameterIsNull() {
        CristinObject cristinObjectThatIsNull = null;
        AtomicReference<PublicationInstanceBuilderImpl> publicationInstanceBuilder = null;
        Executable action = () ->
                publicationInstanceBuilder.set(new PublicationInstanceBuilderImpl(cristinObjectThatIsNull));
        NullPointerException exception = assertThrows(NullPointerException.class, action);
        assertThat(exception.getMessage(),containsString(PublicationInstanceBuilderImpl.ERROR_CRISTIN_OBJECT_IS_NULL));
    }
}

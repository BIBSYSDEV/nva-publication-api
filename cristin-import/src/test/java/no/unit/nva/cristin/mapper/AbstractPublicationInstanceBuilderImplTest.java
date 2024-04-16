package no.unit.nva.cristin.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.s3.S3Client;

public class AbstractPublicationInstanceBuilderImplTest {

    @Test
    public void publicationInstanceBuilderConstructorThrowsNullPointerExceptionIfParameterIsNull() {
        CristinObject cristinObjectThatIsNull = null;
        AtomicReference<PublicationInstanceBuilderImpl> publicationInstanceBuilder = null;
        Executable action = () ->
                                publicationInstanceBuilder.set(
                                    new PublicationInstanceBuilderImpl(cristinObjectThatIsNull, mock(S3Client.class)));
        NullPointerException exception = assertThrows(NullPointerException.class, action);
        assertThat(exception.getMessage(), containsString(PublicationInstanceBuilderImpl.ERROR_CRISTIN_OBJECT_IS_NULL));
    }
}
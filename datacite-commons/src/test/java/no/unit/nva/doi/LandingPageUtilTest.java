package no.unit.nva.doi;

import static no.unit.nva.doi.LandingPageUtil.ERROR_PUBLICATION_LANDING_PAGE_COULD_NOT_BE_CONSTRUCTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Optional;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class LandingPageUtilTest {

    public static final String INVALID_HOST = "~~!@#$";
    public static final String SOME_IDENTIFIER = "someIdentifier";

    @Test
    public void constructResourceUriReturnsTheIdOfTheResource() {
        LandingPageUtil landingPageUtil = new LandingPageUtil(new Environment());
        URI uri = landingPageUtil.constructResourceUri(SOME_IDENTIFIER);
        assertThat(uri.toString(), containsString(LandingPageUtil.DEFAULT_RESOURCES_HOST));
        assertThat(uri.toString(), containsString(SOME_IDENTIFIER));
    }

    @Test
    public void constructResourceThrowsExceptionWhenHostIsInvalid() {
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.readEnvOpt(LandingPageUtil.RESOURCES_HOST_ENV_VARIABLE)).thenReturn(Optional.of(INVALID_HOST));
        LandingPageUtil landingPageUtil = new LandingPageUtil(mockEnv);
        Executable action = () -> landingPageUtil.constructResourceUri(SOME_IDENTIFIER);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), is(equalTo(ERROR_PUBLICATION_LANDING_PAGE_COULD_NOT_BE_CONSTRUCTED)));
    }
}

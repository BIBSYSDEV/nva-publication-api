package no.unit.nva.expansion.restclients;

import static no.unit.nva.expansion.restclients.IdentityClientImpl.ERROR_READING_SECRETS_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class IdentityClientImplTest {

    @Test
    void shouldLogMessageWhenSecretsFailToRead() throws ErrorReadingSecretException {
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        SecretsReader secretsReader = failingSecretsReader();
        HttpClient httpClient = HttpClient.newBuilder().build();
        Executable action = () -> new IdentityClientImpl(secretsReader, httpClient);
        assertThrows(RuntimeException.class, action);
        assertThat(appender.getMessages(), containsString(ERROR_READING_SECRETS_ERROR));
    }

    private SecretsReader failingSecretsReader() throws ErrorReadingSecretException {
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(secretsReader.fetchSecret(anyString(), anyString())).thenThrow(new ErrorReadingSecretException());
        return secretsReader;
    }
}
package no.unit.nva.publication.utils;

import static no.unit.nva.publication.utils.CristinUnitsUtil.API_HOST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

public class CristinUnitsUtilTest {

    public static final String CRISTIN_API_URI = "https://mock-api.cristin.no/v2/units";// prod: https://api.cristin.no/v2/units
    public static final String CRISTIN_BOT_FILTER_BYPASS_HEADER_NAME = "cristinBotFilterBypassHeaderName";
    public static final String CRISTIN_BOT_FILTER_BYPASS_HEADER_VALUE = "cristinBotFilterBypassHeaderValue";
    private HttpClient httpClient;
    private Environment environment;

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        this.httpClient = mock(HttpClient.class);
        this.environment = mock(Environment.class);
        when(this.environment.readEnv(API_HOST)).thenReturn("https://api.unittest.nva.aws.sikt.no");

        when(httpClient.send(any(HttpRequest.class),
                             ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenAnswer(
            (Answer<HttpResponse<String>>) invocationOnMock -> {
                HttpRequest request = invocationOnMock.getArgument(0);
                int lastDigit =
                    Character.getNumericValue(request.uri().toString().charAt(request.uri().toString().length() - 1));
                var result = IoUtils.stringFromResources(Path.of("cristinUnits/units%d.json".formatted(lastDigit)));
                HttpResponse<String> httpResponse = mock(HttpResponse.class);
                when(httpResponse.body()).thenReturn(result);
                return httpResponse;
            });
    }

    @ParameterizedTest(name = "should return top level unit or null for {0} expecting {1}")
    @CsvSource({
        "https://api.cristin.no/v2/units/217.0.0.0, https://api.cristin.no/v2/units/217.0.0.0",
        "https://api.cristin.no/v2/units/217.6.6.0, https://api.cristin.no/v2/units/217.0.0.0",
        "https://api.cristin.no/v2/units/217.6.6.1, https://api.cristin.no/v2/units/217.0.0.0",
        "https://api.cristin.no/v2/units/999.6.6.1,",
        "https://api.dev.nva.aws.unit.no/cristin/organization/217.13.1.0, https://api.dev.nva.aws.unit"
        + ".no/cristin/organization/217.0.0.0",
        "217.13.1.0, 217.0.0.0"
    })
    void shouldReturnTopLevel(String inputUri, String expectedUri) {
        var apiUri = URI.create(CRISTIN_API_URI);
        var result = new CristinUnitsUtil(httpClient, apiUri, environment, this.getClass(),
                                          CRISTIN_BOT_FILTER_BYPASS_HEADER_NAME,
                                          CRISTIN_BOT_FILTER_BYPASS_HEADER_VALUE).getTopLevel(
            URI.create(inputUri));
        Assertions.assertEquals(expectedUri != null ? URI.create(expectedUri) : null, result);
    }
}

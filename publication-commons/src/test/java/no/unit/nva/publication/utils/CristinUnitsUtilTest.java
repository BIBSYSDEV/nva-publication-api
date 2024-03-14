package no.unit.nva.publication.utils;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.streamToString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.publication.external.services.UriRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.stubbing.Answer;

public class CristinUnitsUtilTest {

    public static final String CRISTIN_API_URI = "https://mock-api.cristin.no/v2/units";// prod: https://api.cristin.no/v2/units
    private UriRetriever uriRetriever;

    @BeforeEach
    public void setUp() {
        this.uriRetriever = mock(UriRetriever.class);

            when(uriRetriever.getRawContent(any(), any())).thenAnswer(
                (Answer<Optional<String>>) invocationOnMock -> {
                    URI uri = invocationOnMock.getArgument(0);
                    int lastDigit = Character.getNumericValue(uri.toString().charAt(uri.toString().length() - 1));
                    try (var stream = inputStreamFromResources("cristinUnits/units%d.json".formatted(lastDigit))) {
                        var result = Optional.ofNullable(attempt(() -> streamToString(stream)).orElseThrow());
                        stream.close();
                        return result;
                    }
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
        var result = new CristinUnitsUtil(uriRetriever, apiUri).getTopLevel(
            URI.create(inputUri));
        Assertions.assertEquals(expectedUri != null ? URI.create(expectedUri) : null, result);
    }
}

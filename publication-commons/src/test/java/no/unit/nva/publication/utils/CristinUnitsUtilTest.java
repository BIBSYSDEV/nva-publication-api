package no.unit.nva.publication.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class CristinUnitsUtilTest {

    public static final String CRISTIN_API_URI = "https://mock-api.cristin.no/v2/units";// prod: https://api.cristin.no/v2/units
    private S3Client s3Client;

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        this.s3Client = mock(S3Client.class);
        when(s3Client.utilities()).thenReturn(S3Client.create().utilities());
        when(s3Client.getObjectAsBytes(GetObjectRequest.builder()
                                           .bucket("something")
                                           .key("object.json")
                                           .build())).thenAnswer(
            (Answer<ResponseBytes<GetObjectResponse>>) invocationOnMock -> getUnitsResponseBytes());
    }

    private static ResponseBytes getUnitsResponseBytes() {
        var result = IoUtils.stringFromResources(Path.of("cristinUnits/units-norway.json"));
        var httpResponse = mock(ResponseBytes.class);
        when(httpResponse.asUtf8String()).thenReturn(result);
        return httpResponse;
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
        var result = new CristinUnitsUtil(s3Client, "s3://something/object.json").getTopLevel(
            URI.create(inputUri));
        Assertions.assertEquals(expectedUri != null ? URI.create(expectedUri) : null, result);
    }
}

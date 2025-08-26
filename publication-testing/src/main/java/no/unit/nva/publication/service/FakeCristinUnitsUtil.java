package no.unit.nva.publication.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.nio.file.Path;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import no.unit.nva.publication.utils.CristinUnitsUtilImpl;
import nva.commons.core.ioutils.IoUtils;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class FakeCristinUnitsUtil implements CristinUnitsUtil {

    private final CristinUnitsUtilImpl cristinUnitsUtil;

    @SuppressWarnings("PMD.CloseResource") // Mock object doesn't need to be closed
    public FakeCristinUnitsUtil() {
        var s3Client = mock(S3Client.class);
        try (var realS3Client = S3Client.create()) {
            lenient().when(s3Client.utilities()).thenReturn(realS3Client.utilities());
        }
        lenient().when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenAnswer(
            (Answer<ResponseBytes<GetObjectResponse>>) invocationOnMock -> getUnitsResponseBytes());
        this.cristinUnitsUtil = new CristinUnitsUtilImpl(s3Client, "s3://some-bucket/some-key");
    }

    private static ResponseBytes<GetObjectResponse> getUnitsResponseBytes() {
        var result = IoUtils.stringFromResources(Path.of("cristinUnits/units-norway.json"));
        @SuppressWarnings("unchecked")
        var httpResponse = (ResponseBytes<GetObjectResponse>) mock(ResponseBytes.class);
        when(httpResponse.asUtf8String()).thenReturn(result);
        return httpResponse;
    }

    @Override
    public URI getTopLevel(URI unitId) {
        return cristinUnitsUtil.getTopLevel(unitId);
    }
}

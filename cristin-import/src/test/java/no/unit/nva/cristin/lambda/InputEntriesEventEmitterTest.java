package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public class InputEntriesEventEmitterTest {

    public static final String UNEXPECTED_DETAIL_TYPE = "unexpected detail type";
    public static final String FILE_01 = "file01";
    public static final String FILE_02 = "file02";
    public static final Map<String, InputStream> MOCK_S3_CONTENTS = Map.of(FILE_01, InputStream.nullInputStream(),
                                                                           FILE_02, InputStream.nullInputStream());
    public static final Context CONTEXT = mock(Context.class);

    private final ImportRequest eventDetail = new ImportRequest("s3://some/location/goes/here");
    private S3Client s3Client;
    private EventBridgeClient eventBridgeClient;
    private InputEntriesEventEmitter handler;
    private ByteArrayOutputStream outpuStream;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client(MOCK_S3_CONTENTS);
        eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME);
        handler = new InputEntriesEventEmitter(s3Client, eventBridgeClient);
        outpuStream = new ByteArrayOutputStream();
    }

    @Test
    public void handlerThrowsExceptionWhenInputDoesNotHaveTheExpectedDetailType() {
        AwsEventBridgeEvent<ImportRequest> request = new AwsEventBridgeEvent<>();
        request.setDetailType(UNEXPECTED_DETAIL_TYPE);
        request.setDetail(eventDetail);
        InputStream input = toInputStream(request);
        Executable action = () -> handler.handleRequest(input, outpuStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(request.getDetailType()));
    }

    private InputStream toInputStream(AwsEventBridgeEvent<ImportRequest> request) {
        return attempt(() -> JsonUtils.objectMapperNoEmpty.writeValueAsString(request))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }
}
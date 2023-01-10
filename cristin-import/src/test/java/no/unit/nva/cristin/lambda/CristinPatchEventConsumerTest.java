package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.SUBTOPIC;
import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.TOPIC;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CristinPatchEventConsumerTest extends AbstractCristinImportTest {

    public static final Context CONTEXT = mock(Context.class);

    private FakeS3Client s3Client;
    private S3Driver s3Driver;

    private CristinPatchEventConsumer handler;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        super.init();
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignored");
        handler = new CristinPatchEventConsumer();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void dummyTest() throws IOException {
        var fileUri = s3Driver.insertFile(randomPath(), randomString());
        var input = toInputStream(createInputEventForFile(fileUri));
        handler.handleRequest(input, outputStream, CONTEXT);
    }

    private UnixPath randomPath() {
        return UnixPath.of(randomString(), randomString());
    }

    private InputStream toInputStream(AwsEventBridgeEvent<EventReference> request) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(request))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }

    private AwsEventBridgeEvent<EventReference> createInputEventForFile(URI fileUri) {
        var eventReference = new EventReference(TOPIC,
                                                SUBTOPIC,
                                                fileUri,
                                                Instant.now());
        var request = new AwsEventBridgeEvent<EventReference>();

        request.setDetail(eventReference);
        return request;
    }
}

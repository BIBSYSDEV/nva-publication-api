package no.unit.nva.cristin.lambda;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.cristin.FakeAmazonSQS;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

class CristinRerunEventEmitterTest {

    public static final FakeContext CONTEXT = new FakeContext();
    public static final String CRISTIN_ENTRY_LOCATION = "s3://cristin-import-884807050265/cristinEntries/2024-04"
                                                        + "-19T12:08:24.244309343Z/19843994-c259-4ca8-9143"
                                                        + "-786739318a97.gz";
    private FakeAmazonSQS sqsClient;
    private CristinRerunErrorsEventEmitter handler;
    private S3Driver s3Driver;
    private ByteArrayOutputStream output;

    @BeforeEach
    void init() {
        S3Client s3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(s3Client, "ignored");
        this.sqsClient = new FakeAmazonSQS();
        this.output = new ByteArrayOutputStream();
        this.handler = new CristinRerunErrorsEventEmitter(s3Client, sqsClient);
    }

    @Test
    void shouldReadErrorReportFromCristinErrorLocationAndEmitNewSqsEventForFailedCristinEntry() throws IOException {
        var errorsLocation = UnixPath.of(new Environment().readEnv("CRISTIN_IMPORT_BUCKET"))
                                 .addChild("errors")
                                 .addChild("nullpointer");
        s3Driver.insertFile(errorsLocation.addChild("12345"), getFailure(CRISTIN_ENTRY_LOCATION));
        var input = IoUtils.stringToStream(new RerunFailedEntriesEvent(URI.create(errorsLocation.toString())).toJsonString());
        handler.handleRequest(input, output, CONTEXT);
        var expectedMessageBody = new EventReference("PublicationService.DataImport.DataEntry",
                                                     "PublicationService.CristinData.DataEntry",
                                                     URI.create(CRISTIN_ENTRY_LOCATION));
        var deliveredSqsMessage = JsonUtils.dtoObjectMapper.readValue(sqsClient.getMessageBodies().getFirst(),
                                                                      EventReference.class);
        assertThat(deliveredSqsMessage.getUri(), is(equalTo(expectedMessageBody.getUri())));
    }

    private String getFailure(String cristinEntryLocation) {
        return """
                {
              "input": {
                "topic": "PublicationService.DataImport.DataEntry",
                "subtopic": "PublicationService.CristinData.DataEntry",
                "fileUri": "CRISTIN_ENTRY_LOCATION",
                "timestamp": "2024-04-19T12:08:24.244309343Z",
                "contents": {}
              }
            }
            """.replace("CRISTIN_ENTRY_LOCATION", cristinEntryLocation);
    }
}
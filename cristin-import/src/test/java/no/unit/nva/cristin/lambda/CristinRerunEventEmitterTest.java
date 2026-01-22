package no.unit.nva.cristin.lambda;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.cristin.utils.FakeSqsClient;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

class CristinRerunEventEmitterTest {

    public static final FakeContext CONTEXT = new FakeContext();
    public static final String CRISTIN_ENTRY_LOCATION = "s3://cristin-import-884807050265/cristinEntries/2024-04"
                                                        + "-19T12:08:24.244309343Z/19843994-c259-4ca8-9143"
                                                        + "-786739318a97.gz";
    private FakeSqsClient sqsClient;
    private FakeS3Client s3Client;
    private CristinRerunErrorsEventEmitter handler;
    private S3Driver s3Driver;
    private ByteArrayOutputStream output;

    @BeforeEach
    void init() {
        this.s3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(s3Client, "ignored");
        this.sqsClient = new FakeSqsClient();
        this.output = new ByteArrayOutputStream();
        this.handler = new CristinRerunErrorsEventEmitter(s3Client, sqsClient);
    }

    @Test
    void shouldReadErrorReportFromCristinErrorLocationAndEmitNewSqsEventForFailedCristinEntry() throws IOException {
        var errorsLocation = UriWrapper.fromHost(new Environment().readEnv("CRISTIN_IMPORT_BUCKET"))
            .addChild("errors")
            .addChild("nullpointer");

        s3Driver.insertFile(errorsLocation.addChild("12345").toS3bucketPath(), getFailure(CRISTIN_ENTRY_LOCATION));
        var errorLocation = URI.create(errorsLocation.toString());
        var input = IoUtils.stringToStream(new RerunFailedEntriesEvent(errorLocation).toJsonString());
        handler.handleRequest(input, output, CONTEXT);
        var expectedMessageBody = new EventReference("PublicationService.DataImport.DataEntry",
                                                     "PublicationService.CristinData.DataEntry",
                                                     URI.create(CRISTIN_ENTRY_LOCATION));
        var deliveredSqsMessage = JsonUtils.dtoObjectMapper.readValue(sqsClient.getMessageBodies().getFirst(),
                                                                      EventReference.class);
        assertThat(deliveredSqsMessage.getUri(), is(equalTo(expectedMessageBody.getUri())));
    }

    @Test
    void shouldReadErrorReportFromCristinErrorLocationAndDoNotEmitEventWhenNoErrors() {
        var errorsLocation = UnixPath.of(new Environment().readEnv("CRISTIN_IMPORT_BUCKET"))
                                 .addChild("errors")
                                 .addChild("nullpointer");
        var errorLocation = URI.create(errorsLocation.toString());
        var input = IoUtils.stringToStream(new RerunFailedEntriesEvent(errorLocation).toJsonString());
        handler.handleRequest(input, output, CONTEXT);

        assertTrue(sqsClient.getMessageBodies().isEmpty());
    }

    @Test
    void shouldDeleteErrorReportAfterRelatedResourceHasBeenSentToQueue() throws IOException {
        var errorsLocation = UriWrapper.fromHost(new Environment().readEnv("CRISTIN_IMPORT_BUCKET"))
                                 .addChild("errors")
                                 .addChild("nullpointer");

        var errorReport = errorsLocation.addChild("12345").toS3bucketPath();
        s3Driver.insertFile(errorReport, getFailure(CRISTIN_ENTRY_LOCATION));
        var errorLocation = URI.create(errorsLocation.toString());
        var input = IoUtils.stringToStream(new RerunFailedEntriesEvent(errorLocation).toJsonString());
        handler.handleRequest(input, output, CONTEXT);

        assertThrows(NoSuchKeyException.class, () -> s3Driver.getFile(errorReport));
    }

    @Test
    void shouldNotDeleteErrorReportWhenSqsMessageForCristinEntryHasNotBeenSent() throws IOException {
        var errorsLocation = UriWrapper.fromHost(new Environment().readEnv("CRISTIN_IMPORT_BUCKET"))
                                 .addChild("errors")
                                 .addChild("nullpointer");
        var errorReport = errorsLocation.addChild("12345").toS3bucketPath();
        var failure = getFailure(CRISTIN_ENTRY_LOCATION);
        sqsClient = sqsClientThatFailsToSendMessages();
        s3Driver.insertFile(errorReport, failure);
        var errorLocation = URI.create(errorsLocation.toString());
        var input = IoUtils.stringToStream(new RerunFailedEntriesEvent(errorLocation).toJsonString());
        new CristinRerunErrorsEventEmitter(s3Client, sqsClient).handleRequest(input, output, CONTEXT);
        var notDeleteReport = s3Driver.getFile(errorReport);

        assertThat(notDeleteReport, is(equalTo(failure)));
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

    private FakeSqsClient sqsClientThatFailsToSendMessages() {
        return new FakeSqsClient() {
            @Override
            public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
                return SendMessageBatchResponse.builder()
                           .failed(sendMessageBatchRequest.entries().stream()
                                       .map(this::createFailedResult)
                                       .toList())
                           .successful(List.of())
                           .build();
            }

            private BatchResultErrorEntry createFailedResult(SendMessageBatchRequestEntry entry) {
                return BatchResultErrorEntry.builder()
                           .id(entry.id())
                           .message("Failed miserably")
                           .build();
            }
        };
    }
}

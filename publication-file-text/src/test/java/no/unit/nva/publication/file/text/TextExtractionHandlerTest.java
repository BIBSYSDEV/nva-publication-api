package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextExtractionHandlerTest {

  private static final String SOURCE_BUCKET = "source-bucket";
  private static final String TEXT_BUCKET = "text-bucket";
  private static final String SOME_KEY = "publications/2024/document.pdf";
  private static final String SOME_ETAG = "abc123";
  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String EXTRACTED_TEXT = "The quick brown fox";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String TXT_EXTENSION = ".txt";

  private FakeS3Client fakeS3Client;
  private TextExtractionConfig config;
  private FakeContext context;

  @BeforeEach
  void setUp() {
    fakeS3Client = new FakeS3Client();
    config = new TextExtractionConfig(SOURCE_BUCKET, TEXT_BUCKET);
    context = new FakeContext();
  }

  @Test
  void shouldThrowWhenMessageBodyIsUnparseable() {
    var handler =
        new TextExtractionHandler(fakeS3Client, config, List.of(new FallbackTextExtractor()));
    var event = buildSqsEvent("not-valid-json");

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldNotStoreTextWhenExtractionIsFlagged() throws IOException {
    var handler =
        new TextExtractionHandler(fakeS3Client, config, List.of(new FallbackTextExtractor()));
    var event =
        buildSqsEventFromRequest(
            new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY, SOME_ETAG, PDF_CONTENT_TYPE));

    handler.handleRequest(event, context);

    var textDriver = new S3Driver(fakeS3Client, TEXT_BUCKET);
    assertThat(textDriver.listAllFiles(UnixPath.ROOT_PATH)).isEmpty();
  }

  @Test
  void shouldStoreExtractedTextInTextBucketUnderSameKeyWithTxtSuffix() throws IOException {
    var successExtractor = extractorThatReturns(EXTRACTED_TEXT);
    var handler = new TextExtractionHandler(fakeS3Client, config, List.of(successExtractor));
    var event =
        buildSqsEventFromRequest(
            new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY, SOME_ETAG, PDF_CONTENT_TYPE));

    handler.handleRequest(event, context);

    var textDriver = new S3Driver(fakeS3Client, TEXT_BUCKET);
    var storedText = textDriver.getFile(UnixPath.of(SOME_KEY + TXT_EXTENSION));
    assertThat(storedText).isEqualTo(EXTRACTED_TEXT);
  }

  @Test
  void shouldReadBucketNamesFromEnvironment() {
    var config = TextExtractionConfig.fromEnvironment();

    assertThat(config.sourceBucketName()).isEqualTo(SOURCE_BUCKET);
    assertThat(config.textBucketName()).isEqualTo(TEXT_BUCKET);
  }

  private TextExtractor extractorThatReturns(String text) {
    return new TextExtractor() {
      @Override
      public boolean supports(String contentType) {
        return true;
      }

      @Override
      public ExtractionResult extract(ExtractionInput input) {
        return new ExtractionResult.Extracted(input, text);
      }
    };
  }

  private SQSEvent buildSqsEventFromRequest(TextExtractionRequest request)
      throws JsonProcessingException {
    return buildSqsEvent(OBJECT_MAPPER.writeValueAsString(request));
  }

  private SQSEvent buildSqsEvent(String body) {
    var message = new SQSMessage();
    message.setBody(body);
    var event = new SQSEvent();
    event.setRecords(List.of(message));
    return event;
  }
}

package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class TextExtractionHandlerTest {

  private static final String SOURCE_BUCKET = "source-bucket";
  private static final String TEXT_BUCKET = "text-bucket";
  private static final String SOME_KEY = "publications/2024/document.pdf";
  private static final String SOME_ETAG = "\"abc123\"";
  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String EXTRACTED_TEXT = "The quick brown fox";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectMetadataSource FIXED_METADATA =
      (bucket, key) -> new ObjectMetadata(SOME_ETAG, PDF_CONTENT_TYPE);

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
        new TextExtractionHandler(
            fakeS3Client, FIXED_METADATA, config, List.of(new FallbackTextExtractor()));

    assertThatThrownBy(() -> handler.handleRequest(buildSqsEvent("not-valid-json"), context))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldNotThrowWhenContentTypeIsUnsupported() throws JsonProcessingException {
    var handler =
        new TextExtractionHandler(
            fakeS3Client, FIXED_METADATA, config, List.of(new FallbackTextExtractor()));

    assertThatCode(
            () ->
                handler.handleRequest(
                    buildSqsEventFromRequest(new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY)),
                    context))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldThrowWhenExtractionFails() throws JsonProcessingException {
    var handler =
        new TextExtractionHandler(
            fakeS3Client,
            FIXED_METADATA,
            config,
            List.of(extractorThatFlags(ExtractionFailureReason.EXTRACTION_ERROR)));

    assertThatThrownBy(
            () ->
                handler.handleRequest(
                    buildSqsEventFromRequest(new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY)),
                    context))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldNotStoreTextWhenExtractionIsFlaggedWithNonFatalReason() throws IOException {
    var handler =
        new TextExtractionHandler(
            fakeS3Client,
            FIXED_METADATA,
            config,
            List.of(extractorThatFlags(ExtractionFailureReason.PASSWORD_PROTECTED)));

    assertThatCode(
            () ->
                handler.handleRequest(
                    buildSqsEventFromRequest(new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY)),
                    context))
        .doesNotThrowAnyException();

    assertThat(new S3Driver(fakeS3Client, TEXT_BUCKET).listAllFiles(UnixPath.ROOT_PATH)).isEmpty();
  }

  @Test
  void shouldNotStoreTextWhenExtractedTextIsBlank() throws IOException {
    var handler =
        new TextExtractionHandler(
            fakeS3Client, FIXED_METADATA, config, List.of(extractorThatReturns("   ")));

    handler.handleRequest(
        buildSqsEventFromRequest(new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY)), context);

    assertThat(new S3Driver(fakeS3Client, TEXT_BUCKET).listAllFiles(UnixPath.ROOT_PATH)).isEmpty();
  }

  @Test
  void shouldNotThrowWhenSourceObjectNoLongerExists() throws JsonProcessingException {
    ObjectMetadataSource sourceNotFound =
        (bucket, key) -> {
          throw NoSuchKeyException.builder().build();
        };
    var handler =
        new TextExtractionHandler(
            fakeS3Client, sourceNotFound, config, List.of(new FallbackTextExtractor()));

    assertThatCode(
            () ->
                handler.handleRequest(
                    buildSqsEventFromRequest(new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY)),
                    context))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldStoreExtractedTextInTextBucketUnderSameKeyWithTxtSuffix() throws IOException {
    var handler =
        new TextExtractionHandler(
            fakeS3Client, FIXED_METADATA, config, List.of(extractorThatReturns(EXTRACTED_TEXT)));

    handler.handleRequest(
        buildSqsEventFromRequest(new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY)), context);

    var storedText =
        new S3Driver(fakeS3Client, TEXT_BUCKET).getFile(UnixPath.of(SOME_KEY + ".txt"));
    assertThat(storedText).isEqualTo(EXTRACTED_TEXT);
  }

  @Test
  void shouldReadBucketNamesFromEnvironment() {
    var config = TextExtractionConfig.fromEnvironment();

    assertThat(config.sourceBucketName()).isEqualTo(SOURCE_BUCKET);
    assertThat(config.textBucketName()).isEqualTo(TEXT_BUCKET);
  }

  private static TextExtractor extractorThatReturns(String text) {
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

  private static TextExtractor extractorThatFlags(ExtractionFailureReason reason) {
    return new TextExtractor() {
      @Override
      public boolean supports(String contentType) {
        return true;
      }

      @Override
      public ExtractionResult extract(ExtractionInput input) {
        return new ExtractionResult.Flagged(input, reason, reason.name());
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

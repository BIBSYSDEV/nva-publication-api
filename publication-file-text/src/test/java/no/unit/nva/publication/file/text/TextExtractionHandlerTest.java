package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
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
  private static final String PARAMETERIZED_MIXED_CASE_PDF = "Application/PDF; charset=UTF-8";
  private static final String EXTRACTED_TEXT = "The quick brown fox";
  private static final String BLANK_TEXT = "   ";
  private static final String MESSAGE_ID = "test-message-id";
  private static final String TEXT_KEY = SOME_KEY + ".txt";
  private static final String FLAG_KEY = "flags/" + SOME_KEY + ".json";
  private static final String TEXT_KEY_SUFFIX = ".txt";
  private static final ObjectMetadataSource FIXED_METADATA =
      (bucket, key) -> new ObjectMetadata(SOME_ETAG, PDF_CONTENT_TYPE);

  private FakeS3Client fakeS3Client;
  private TextExtractionConfig config;
  private FakeContext context;

  @BeforeEach
  void setUp() {
    fakeS3Client = new FakeS3Client();
    config = new TextExtractionConfig(TEXT_BUCKET);
    context = new FakeContext();
  }

  @Test
  void shouldReturnBatchItemFailureWhenMessageBodyIsUnparseable() {
    var handler = handlerWith(FIXED_METADATA, List.of());

    var result = handler.handleRequest(buildSqsEvent("not-valid-json"), context);

    assertThat(result.getBatchItemFailures()).hasSize(1);
    assertThat(result.getBatchItemFailures().getFirst().getItemIdentifier()).isEqualTo(MESSAGE_ID);
  }

  @Test
  void shouldNotReturnBatchItemFailureWhenNoExtractorSupportsContentType() {
    var handler = handlerWith(FIXED_METADATA, List.of());

    var result = handler.handleRequest(extractionRequestEvent(), context);

    assertThat(result.getBatchItemFailures()).isEmpty();
  }

  @Test
  void shouldStoreFlagMarkerWhenNoExtractorSupportsContentType() throws IOException {
    var handler = handlerWith(FIXED_METADATA, List.of());

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(storedFlagMarker())
        .contains(ExtractionFailureReason.UNSUPPORTED_FORMAT.name())
        .contains(SOME_KEY)
        .contains(PDF_CONTENT_TYPE);
    assertThatNoTextIsStored();
  }

  @Test
  void shouldReturnBatchItemFailureWithoutFlagMarkerWhenExtractionFails() throws IOException {
    var handler =
        handlerWith(
            FIXED_METADATA, List.of(extractorThatFlags(ExtractionFailureReason.EXTRACTION_ERROR)));

    var result = handler.handleRequest(extractionRequestEvent(), context);

    assertThat(result.getBatchItemFailures()).hasSize(1);
    assertThat(textBucketDriver().listAllFiles(UnixPath.ROOT_PATH)).isEmpty();
  }

  @Test
  void shouldStoreFlagMarkerInsteadOfTextWhenExtractionIsFlaggedWithNonFatalReason()
      throws IOException {
    var handler =
        handlerWith(
            FIXED_METADATA,
            List.of(extractorThatFlags(ExtractionFailureReason.PASSWORD_PROTECTED)));

    assertThatCode(() -> handler.handleRequest(extractionRequestEvent(), context))
        .doesNotThrowAnyException();

    assertThat(storedFlagMarker())
        .contains(ExtractionFailureReason.PASSWORD_PROTECTED.name())
        .contains("abc123");
    assertThatNoTextIsStored();
  }

  @Test
  void shouldStoreFlagMarkerInsteadOfTextWhenExtractedTextIsBlank() throws IOException {
    var handler = handlerWith(FIXED_METADATA, List.of(extractorThatReturns(BLANK_TEXT)));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(storedFlagMarker()).contains(ExtractionFailureReason.BLANK_CONTENT.name());
    assertThatNoTextIsStored();
  }

  @Test
  void shouldNotReturnBatchItemFailureWhenSourceObjectNoLongerExists() {
    ObjectMetadataSource sourceNotFound =
        (bucket, key) -> {
          throw NoSuchKeyException.builder().build();
        };
    var handler = handlerWith(sourceNotFound, List.of());

    var result = handler.handleRequest(extractionRequestEvent(), context);

    assertThat(result.getBatchItemFailures()).isEmpty();
  }

  @Test
  void shouldStoreExtractedTextInTextBucketUnderSameKeyWithTxtSuffix() throws IOException {
    var handler = handlerWith(FIXED_METADATA, List.of(extractorThatReturns(EXTRACTED_TEXT)));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(textBucketDriver().getFile(UnixPath.of(TEXT_KEY))).isEqualTo(EXTRACTED_TEXT);
  }

  @Test
  void shouldExtractWhenStoredContentTypeHasParametersAndMixedCase() throws IOException {
    ObjectMetadataSource parameterizedMetadata =
        (bucket, key) -> new ObjectMetadata(SOME_ETAG, PARAMETERIZED_MIXED_CASE_PDF);
    var handler =
        handlerWith(
            parameterizedMetadata, List.of(extractorSupporting(PDF_CONTENT_TYPE, EXTRACTED_TEXT)));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(textBucketDriver().getFile(UnixPath.of(TEXT_KEY))).isEqualTo(EXTRACTED_TEXT);
  }

  @Test
  void shouldReadTextBucketNameFromEnvironment() {
    var environmentConfig = TextExtractionConfig.fromEnvironment();

    assertThat(environmentConfig.textBucketName()).isEqualTo(TEXT_BUCKET);
  }

  private TextExtractionHandler handlerWith(
      ObjectMetadataSource metadataSource, List<TextExtractor> extractors) {
    return new TextExtractionHandler(fakeS3Client, metadataSource, config, extractors);
  }

  private S3Driver textBucketDriver() {
    return new S3Driver(fakeS3Client, TEXT_BUCKET);
  }

  private String storedFlagMarker() {
    return textBucketDriver().getFile(UnixPath.of(FLAG_KEY));
  }

  private void assertThatNoTextIsStored() throws IOException {
    assertThat(textBucketDriver().listAllFiles(UnixPath.ROOT_PATH))
        .noneMatch(path -> path.toString().endsWith(TEXT_KEY_SUFFIX));
  }

  private static TextExtractor extractorSupporting(String supportedContentType, String text) {
    return new TextExtractor() {
      @Override
      public boolean supports(String contentType) {
        return supportedContentType.equals(contentType);
      }

      @Override
      public ExtractionResult extract(ExtractionInput input) {
        return new ExtractionResult.Extracted(input, text);
      }
    };
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

  private SQSEvent extractionRequestEvent() {
    return buildSqsEvent(new TextExtractionRequest(SOURCE_BUCKET, SOME_KEY).toJsonString());
  }

  private SQSEvent buildSqsEvent(String body) {
    var message = new SQSMessage();
    message.setMessageId(MESSAGE_ID);
    message.setBody(body);
    var event = new SQSEvent();
    event.setRecords(List.of(message));
    return event;
  }
}

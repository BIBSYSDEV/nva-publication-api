package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
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
  private static final String BLANK_TEXT = "   ";
  private static final String MESSAGE_ID = "test-message-id";
  private static final String TEXT_KEY = SOME_KEY + ".txt";
  private static final String FLAG_KEY = "flags/" + SOME_KEY + ".json";
  private static final String TEXT_KEY_SUFFIX = ".txt";
  private static final String FLAG_KEY_PREFIX = "flags/";
  private static final Path UNUSED_FILE = Path.of("/unused");
  private static final long OVERSIZED_OBJECT_BYTES = 600_000_000L;
  private static final long SIZE_LIMIT_BYTES = 400_000_000L;
  private static final FileDownloadSource FIXED_DOWNLOAD =
      (bucket, key) -> new DownloadedObject(UNUSED_FILE, SOME_ETAG);
  private static final FileDownloadSource OVERSIZED_SOURCE =
      (bucket, key) -> {
        throw new FileTooLargeException(OVERSIZED_OBJECT_BYTES, SIZE_LIMIT_BYTES, SOME_ETAG);
      };
  private static final ContentTypeDetector FIXED_DETECTOR = (file, filename) -> PDF_CONTENT_TYPE;

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
    var handler = handlerWith(FIXED_DOWNLOAD, List.of());

    var result = handler.handleRequest(buildSqsEvent("not-valid-json"), context);

    assertThat(result.getBatchItemFailures()).hasSize(1);
    assertThat(result.getBatchItemFailures().getFirst().getItemIdentifier()).isEqualTo(MESSAGE_ID);
  }

  @Test
  void shouldReturnBatchItemFailureWhenDownloadFails() {
    FileDownloadSource failingDownload =
        (bucket, key) -> {
          throw new IllegalStateException("S3 unavailable");
        };
    var handler = handlerWith(failingDownload, List.of(extractor(this::extracted)));

    var result = handler.handleRequest(extractionRequestEvent(), context);

    assertThat(result.getBatchItemFailures()).hasSize(1);
  }

  @Test
  void shouldNotReturnBatchItemFailureWhenSourceObjectNoLongerExists() {
    FileDownloadSource sourceGone =
        (bucket, key) -> {
          throw NoSuchKeyException.builder().build();
        };
    var handler = handlerWith(sourceGone, List.of());

    var result = handler.handleRequest(extractionRequestEvent(), context);

    assertThat(result.getBatchItemFailures()).isEmpty();
  }

  @Test
  void shouldStoreFlagMarkerWithoutBatchItemFailureWhenSourceObjectExceedsSizeLimit()
      throws IOException {
    var handler = handlerWith(OVERSIZED_SOURCE, List.of());

    var result = handler.handleRequest(extractionRequestEvent(), context);

    assertThat(result.getBatchItemFailures()).isEmpty();
    assertThat(storedFlagMarker())
        .contains(ExtractionFailureReason.FILE_TOO_LARGE.name())
        .contains(String.valueOf(OVERSIZED_OBJECT_BYTES))
        .contains(String.valueOf(SIZE_LIMIT_BYTES))
        .contains("abc123");
    assertThatNoTextIsStored();
  }

  @Test
  void shouldDeleteStaleTextWhenSourceObjectExceedsSizeLimit() throws IOException {
    textBucketDriver().insertFile(UnixPath.of(TEXT_KEY), "stale text");
    var handler = handlerWith(OVERSIZED_SOURCE, List.of());

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(storedFlagMarker()).contains(ExtractionFailureReason.FILE_TOO_LARGE.name());
    assertThatNoTextIsStored();
  }

  @Test
  void shouldStoreFlagMarkerWhenNoExtractorSupportsContentType() throws IOException {
    var handler = handlerWith(FIXED_DOWNLOAD, List.of());

    var result = handler.handleRequest(extractionRequestEvent(), context);

    assertThat(result.getBatchItemFailures()).isEmpty();
    assertThat(storedFlagMarker())
        .contains(ExtractionFailureReason.UNSUPPORTED_FORMAT.name())
        .contains(SOME_KEY)
        .contains(PDF_CONTENT_TYPE);
    assertThatNoTextIsStored();
  }

  @Test
  void shouldStoreFlagMarkerWithoutBatchItemFailureWhenParsingFailsDeterministically()
      throws IOException {
    var handler =
        handlerWith(
            FIXED_DOWNLOAD,
            List.of(extractor(input -> flagged(input, ExtractionFailureReason.PARSE_ERROR))));

    var result = handler.handleRequest(extractionRequestEvent(), context);

    assertThat(result.getBatchItemFailures()).isEmpty();
    assertThat(storedFlagMarker()).contains(ExtractionFailureReason.PARSE_ERROR.name());
    assertThatNoTextIsStored();
  }

  @Test
  void shouldStoreFlagMarkerInsteadOfTextWhenDocumentIsPasswordProtected() throws IOException {
    var handler =
        handlerWith(
            FIXED_DOWNLOAD,
            List.of(
                extractor(input -> flagged(input, ExtractionFailureReason.PASSWORD_PROTECTED))));

    assertThatCode(() -> handler.handleRequest(extractionRequestEvent(), context))
        .doesNotThrowAnyException();

    assertThat(storedFlagMarker())
        .contains(ExtractionFailureReason.PASSWORD_PROTECTED.name())
        .contains("abc123");
    assertThatNoTextIsStored();
  }

  @Test
  void shouldStoreFlagMarkerInsteadOfTextWhenExtractedTextIsBlank() throws IOException {
    var handler =
        handlerWith(FIXED_DOWNLOAD, List.of(extractor(input -> extracted(input, BLANK_TEXT))));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(storedFlagMarker()).contains(ExtractionFailureReason.BLANK_CONTENT.name());
    assertThatNoTextIsStored();
  }

  @Test
  void shouldStoreExtractedTextInTextBucketUnderSameKeyWithTxtSuffix() throws IOException {
    var handler = handlerWith(FIXED_DOWNLOAD, List.of(extractor(this::extracted)));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(textBucketDriver().getFile(UnixPath.of(TEXT_KEY))).isEqualTo(EXTRACTED_TEXT);
  }

  @Test
  void shouldStoreTruncationFlagMarkerAlongsideTruncatedText() throws IOException {
    var handler = handlerWith(FIXED_DOWNLOAD, List.of(extractor(this::truncatedExtracted)));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(textBucketDriver().getFile(UnixPath.of(TEXT_KEY))).isEqualTo(EXTRACTED_TEXT);
    assertThat(storedFlagMarker()).contains(ExtractionFailureReason.TRUNCATED_CONTENT.name());
  }

  @Test
  void shouldDeleteStaleFlagMarkerWhenExtractionSucceedsCompletely() throws IOException {
    textBucketDriver().insertFile(UnixPath.of(FLAG_KEY), "stale flag");
    var handler = handlerWith(FIXED_DOWNLOAD, List.of(extractor(this::extracted)));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(textBucketDriver().getFile(UnixPath.of(TEXT_KEY))).isEqualTo(EXTRACTED_TEXT);
    assertThatNoFlagMarkerIsStored();
  }

  @Test
  void shouldDeleteStaleTextWhenExtractionIsFlagged() throws IOException {
    textBucketDriver().insertFile(UnixPath.of(TEXT_KEY), "stale text");
    var handler =
        handlerWith(
            FIXED_DOWNLOAD,
            List.of(
                extractor(input -> flagged(input, ExtractionFailureReason.PASSWORD_PROTECTED))));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(storedFlagMarker()).contains(ExtractionFailureReason.PASSWORD_PROTECTED.name());
    assertThatNoTextIsStored();
  }

  @Test
  void shouldDeleteDownloadedTempFileWhenExtractionSucceeds() throws IOException {
    var tempFile = Files.createTempFile("handler-test-", ".bin");
    var handler =
        handlerWith(
            (bucket, key) -> new DownloadedObject(tempFile, SOME_ETAG),
            List.of(extractor(this::extracted)));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(tempFile).doesNotExist();
  }

  @Test
  void shouldDeleteDownloadedTempFileWhenExtractionIsFlagged() throws IOException {
    var tempFile = Files.createTempFile("handler-test-", ".bin");
    var handler =
        handlerWith(
            (bucket, key) -> new DownloadedObject(tempFile, SOME_ETAG),
            List.of(extractor(input -> flagged(input, ExtractionFailureReason.PARSE_ERROR))));

    handler.handleRequest(extractionRequestEvent(), context);

    assertThat(tempFile).doesNotExist();
  }

  @Test
  void shouldReadTextBucketNameFromEnvironment() {
    var environmentConfig = TextExtractionConfig.fromEnvironment();

    assertThat(environmentConfig.textBucketName()).isEqualTo(TEXT_BUCKET);
  }

  private TextExtractionHandler handlerWith(
      FileDownloadSource downloadSource, List<TextExtractor> extractors) {
    return new TextExtractionHandler(
        fakeS3Client, downloadSource, FIXED_DETECTOR, config, extractors);
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

  private void assertThatNoFlagMarkerIsStored() throws IOException {
    assertThat(textBucketDriver().listAllFiles(UnixPath.ROOT_PATH))
        .noneMatch(path -> path.toString().startsWith(FLAG_KEY_PREFIX));
  }

  private ExtractionResult extracted(ExtractionInput input) {
    return extracted(input, EXTRACTED_TEXT);
  }

  private ExtractionResult extracted(ExtractionInput input, String text) {
    return new ExtractionResult.Extracted(input, text, false);
  }

  private ExtractionResult truncatedExtracted(ExtractionInput input) {
    return new ExtractionResult.Extracted(input, EXTRACTED_TEXT, true);
  }

  private static ExtractionResult flagged(ExtractionInput input, ExtractionFailureReason reason) {
    return new ExtractionResult.Flagged(input, reason, reason.name());
  }

  private static TextExtractor extractor(Function<ExtractionInput, ExtractionResult> result) {
    return new TextExtractor() {
      @Override
      public boolean supports(String contentType) {
        return true;
      }

      @Override
      public ExtractionResult extract(ExtractionInput input, Path file) {
        return result.apply(input);
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

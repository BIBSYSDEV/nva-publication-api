package no.unit.nva.publication.file.text;

import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public final class TextExtractionHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TextExtractionHandler.class);
  private static final String TEXT_KEY_SUFFIX = ".txt";
  private static final String FLAG_KEY_PREFIX = "flags/";
  private static final String FLAG_KEY_SUFFIX = ".json";
  private static final String UNPARSEABLE_MESSAGE_BODY = "Unparseable SQS message body: ";
  private static final String BLANK_CONTENT_DETAIL = "Extracted text was blank";

  private final ObjectMetadataSource metadataSource;
  private final List<TextExtractor> extractors;
  private final S3Driver textStorageDriver;

  @JacocoGenerated
  public TextExtractionHandler() {
    this(S3Driver.defaultS3Client().build(), TextExtractionConfig.fromEnvironment());
  }

  @JacocoGenerated
  TextExtractionHandler(S3Client s3Client, TextExtractionConfig config) {
    this(s3Client, new S3ObjectMetadataSource(s3Client), config, createDefaultExtractors(s3Client));
  }

  public TextExtractionHandler(
      S3Client s3Client,
      ObjectMetadataSource metadataSource,
      TextExtractionConfig config,
      List<TextExtractor> extractors) {
    this.metadataSource = metadataSource;
    this.extractors = extractors;
    this.textStorageDriver = new S3Driver(s3Client, config.textBucketName());
  }

  @JacocoGenerated
  private static List<TextExtractor> createDefaultExtractors(S3Client s3Client) {
    var downloadSource = new S3FileDownloadSource(s3Client);
    return List.of(
        new PdfTextExtractor(downloadSource),
        new WordTextExtractor(downloadSource),
        new LatexTextExtractor(downloadSource));
  }

  @Override
  public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
    var batchItemFailures = new ArrayList<SQSBatchResponse.BatchItemFailure>();
    for (var message : event.getRecords()) {
      try {
        processMessage(message);
      } catch (RuntimeException exception) {
        LOGGER.error(
            "Failed to process message: id={}",
            LogSanitizer.sanitize(message.getMessageId()),
            exception);
        batchItemFailures.add(
            SQSBatchResponse.BatchItemFailure.builder()
                .withItemIdentifier(message.getMessageId())
                .build());
      }
    }
    return SQSBatchResponse.builder().withBatchItemFailures(batchItemFailures).build();
  }

  private void processMessage(SQSMessage message) {
    var request = parseRequest(message.getBody());
    ObjectMetadata metadata;
    try {
      metadata = metadataSource.fetchMetadata(request.bucket(), request.key());
    } catch (NoSuchKeyException exception) {
      LOGGER.warn(
          "Source object no longer exists, skipping: bucket={} key={}",
          LogSanitizer.sanitize(request.bucket()),
          LogSanitizer.sanitize(request.key()));
      return;
    }
    var input =
        new ExtractionInput(
            request.bucket(),
            request.key(),
            metadata.etag(),
            ContentTypeNormalizer.normalize(metadata.contentType()));
    handleResult(dispatch(input));
  }

  private TextExtractionRequest parseRequest(String body) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(body, TextExtractionRequest.class))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    UNPARSEABLE_MESSAGE_BODY
                        + LogSanitizer.sanitize(failure.getException().getMessage())));
  }

  private ExtractionResult dispatch(ExtractionInput input) {
    return extractors.stream()
        .filter(extractor -> extractor.supports(input.contentType()))
        .findFirst()
        .map(extractor -> extractor.extract(input))
        .orElseGet(
            () ->
                new ExtractionResult.Flagged(
                    input, ExtractionFailureReason.UNSUPPORTED_FORMAT, input.contentType()));
  }

  private void handleResult(ExtractionResult result) {
    switch (result) {
      case ExtractionResult.Extracted extracted -> storeText(extracted);
      case ExtractionResult.Flagged flagged -> handleFlag(flagged);
    }
  }

  private void handleFlag(ExtractionResult.Flagged flagged) {
    logFlag(flagged);
    if (flagged.reason() == ExtractionFailureReason.EXTRACTION_ERROR) {
      throw new IllegalStateException(
          "Extraction failed: " + LogSanitizer.sanitize(flagged.detail()));
    }
    storeFlag(flagged.source(), flagged.reason(), flagged.detail());
  }

  private void storeText(ExtractionResult.Extracted extracted) {
    if (extracted.text().isBlank()) {
      LOGGER.warn(
          "Extracted blank text, flagging instead of storing: key={}",
          LogSanitizer.sanitize(extracted.source().sourceKey()));
      storeFlag(extracted.source(), ExtractionFailureReason.BLANK_CONTENT, BLANK_CONTENT_DETAIL);
      return;
    }
    var textKey = extracted.source().sourceKey() + TEXT_KEY_SUFFIX;
    insertFile(textKey, extracted.text());
    LOGGER.info("Stored extracted text: key={}", LogSanitizer.sanitize(textKey));
  }

  private void storeFlag(ExtractionInput source, ExtractionFailureReason reason, String detail) {
    var flag =
        new ExtractionFlag(
            source.sourceBucket(), source.sourceKey(), source.sourceEtag(), reason, detail);
    var flagKey = FLAG_KEY_PREFIX + source.sourceKey() + FLAG_KEY_SUFFIX;
    insertFile(flagKey, flag.toJsonString());
    LOGGER.info("Stored extraction flag: key={}", LogSanitizer.sanitize(flagKey));
  }

  private void insertFile(String key, String content) {
    try {
      textStorageDriver.insertFile(UnixPath.of(key), content);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private void logFlag(ExtractionResult.Flagged flagged) {
    LOGGER.warn(
        "Extraction flagged: bucket={} key={} etag={} reason={} detail={}",
        LogSanitizer.sanitize(flagged.source().sourceBucket()),
        LogSanitizer.sanitize(flagged.source().sourceKey()),
        LogSanitizer.sanitize(flagged.source().sourceEtag()),
        flagged.reason(),
        LogSanitizer.sanitize(flagged.detail()));
  }
}

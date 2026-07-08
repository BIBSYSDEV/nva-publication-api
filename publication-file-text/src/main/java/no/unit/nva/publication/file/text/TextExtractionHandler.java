package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
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
  private static final String PARSE_LOCATION_TEMPLATE = " at line %d, column %d";
  private static final String BLANK_CONTENT_DETAIL = "Extracted text was blank";
  private static final String TRUNCATED_CONTENT_DETAIL =
      "Truncated at " + TikaSupport.MAX_EXTRACTED_CHARACTERS + " characters";

  private final FileDownloadSource downloadSource;
  private final ContentTypeDetector contentTypeDetector;
  private final List<TextExtractor> extractors;
  private final S3Driver textStorageDriver;

  @JacocoGenerated
  public TextExtractionHandler() {
    this(S3Driver.defaultS3Client().build(), TextExtractionConfig.fromEnvironment());
  }

  @JacocoGenerated
  TextExtractionHandler(S3Client s3Client, TextExtractionConfig config) {
    this(
        s3Client,
        new S3FileDownloadSource(s3Client),
        TikaSupport::detectContentType,
        config,
        TextExtractors.defaultExtractors());
  }

  public TextExtractionHandler(
      S3Client s3Client,
      FileDownloadSource downloadSource,
      ContentTypeDetector contentTypeDetector,
      TextExtractionConfig config,
      List<TextExtractor> extractors) {
    this.downloadSource = downloadSource;
    this.contentTypeDetector = contentTypeDetector;
    this.extractors = extractors;
    this.textStorageDriver = new S3Driver(s3Client, config.textBucketName());
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
    DownloadedObject downloadedObject;
    try {
      downloadedObject = downloadSource.downloadToFile(request.bucket(), request.key());
    } catch (NoSuchKeyException exception) {
      LOGGER.warn(
          "Source object no longer exists, skipping: bucket={} key={}",
          LogSanitizer.sanitize(request.bucket()),
          LogSanitizer.sanitize(request.key()));
      return;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    try {
      var contentType =
          contentTypeDetector.detectContentType(downloadedObject.path(), filenameOf(request.key()));
      var input =
          new ExtractionInput(
              request.bucket(), request.key(), downloadedObject.etag(), contentType);
      handleResult(dispatch(input, downloadedObject.path()));
    } finally {
      TempFileSupport.deleteTempFile(downloadedObject.path());
    }
  }

  private static String filenameOf(String key) {
    return UnixPath.of(key).getLastPathElement();
  }

  private TextExtractionRequest parseRequest(String body) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(body, TextExtractionRequest.class))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    UNPARSEABLE_MESSAGE_BODY + describeParseFailure(failure.getException())));
  }

  private static String describeParseFailure(Exception exception) {
    if (exception instanceof JsonProcessingException jsonException) {
      return LogSanitizer.sanitize(jsonException.getOriginalMessage())
          + parseLocationOf(jsonException);
    }
    return LogSanitizer.sanitize(exception.getMessage());
  }

  private static String parseLocationOf(JsonProcessingException jsonException) {
    var location = jsonException.getLocation();
    if (isNull(location) || JsonLocation.NA.equals(location)) {
      return "";
    }
    return PARSE_LOCATION_TEMPLATE.formatted(location.getLineNr(), location.getColumnNr());
  }

  private ExtractionResult dispatch(ExtractionInput input, Path file) {
    return extractors.stream()
        .filter(extractor -> extractor.supports(input.contentType()))
        .findFirst()
        .map(extractor -> extractor.extract(input, file))
        .orElseGet(
            () ->
                new ExtractionResult.Flagged(
                    input, ExtractionFailureReason.UNSUPPORTED_FORMAT, input.contentType()));
  }

  private void handleResult(ExtractionResult result) {
    switch (result) {
      case ExtractionResult.Extracted extracted -> storeText(extracted);
      case ExtractionResult.Flagged flagged -> storeFlaggedResult(flagged);
    }
  }

  private void storeText(ExtractionResult.Extracted extracted) {
    var source = extracted.source();
    if (extracted.text().isBlank()) {
      LOGGER.warn(
          "Extracted blank text, flagging instead of storing: key={}",
          LogSanitizer.sanitize(source.sourceKey()));
      storeFlagAndRemoveStaleText(
          source, ExtractionFailureReason.BLANK_CONTENT, BLANK_CONTENT_DETAIL);
      return;
    }
    var textKey = source.sourceKey() + TEXT_KEY_SUFFIX;
    insertFile(textKey, extracted.text());
    LOGGER.info("Stored extracted text: key={}", LogSanitizer.sanitize(textKey));
    if (extracted.truncated()) {
      LOGGER.warn(
          "Stored text is truncated: key={} limit={}",
          LogSanitizer.sanitize(source.sourceKey()),
          TikaSupport.MAX_EXTRACTED_CHARACTERS);
      storeFlag(source, ExtractionFailureReason.TRUNCATED_CONTENT, TRUNCATED_CONTENT_DETAIL);
    } else {
      textStorageDriver.deleteFile(UnixPath.of(flagKeyFor(source.sourceKey())));
    }
  }

  private void storeFlaggedResult(ExtractionResult.Flagged flagged) {
    logFlag(flagged);
    storeFlagAndRemoveStaleText(flagged.source(), flagged.reason(), flagged.detail());
  }

  private void storeFlagAndRemoveStaleText(
      ExtractionInput source, ExtractionFailureReason reason, String detail) {
    storeFlag(source, reason, detail);
    textStorageDriver.deleteFile(UnixPath.of(source.sourceKey() + TEXT_KEY_SUFFIX));
  }

  private void storeFlag(ExtractionInput source, ExtractionFailureReason reason, String detail) {
    var flag = ExtractionFlag.from(source, reason, detail);
    var flagKey = flagKeyFor(source.sourceKey());
    insertFile(flagKey, flag.toJsonString());
    LOGGER.info("Stored extraction flag: key={}", LogSanitizer.sanitize(flagKey));
  }

  private static String flagKeyFor(String sourceKey) {
    return FLAG_KEY_PREFIX + sourceKey + FLAG_KEY_SUFFIX;
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

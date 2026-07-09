package no.unit.nva.publication.file.text;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Streams a CSV file of S3 object keys uploaded to S3 and enqueues a {@link TextExtractionRequest}
 * for each key in batches of {@value BATCH_SIZE}. Used for bulk population of the text bucket.
 * Memory use is bounded by the batch size regardless of CSV size. Lines are stripped of a UTF-8
 * byte order mark and surrounding whitespace before enqueueing; lines without any letter or digit
 * are skipped. Keys that fail to enqueue — individually or as a whole batch — are each logged, and
 * a run with any failures fails after all batches have been attempted, so the asynchronous retry
 * and on-failure destination see it while every failed key remains identifiable in the logs.
 */
public final class SeedTextExtractionHandler implements RequestHandler<S3Event, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SeedTextExtractionHandler.class);
  private static final int BATCH_SIZE = 10;
  private static final String AWS_REGION_ENV = "AWS_REGION";
  private static final String UTF8_BYTE_ORDER_MARK = "\uFEFF";
  private static final String BATCH_FAILURES_MESSAGE_TEMPLATE =
      "SQS batch enqueue had %d failed entries out of %d";
  private static final String EMPTY_STRING = "";

  private final S3Client s3Client;
  private final SqsClient sqsClient;
  private final SeedTextExtractionConfig config;

  @JacocoGenerated
  public SeedTextExtractionHandler() {
    this(
        S3Driver.defaultS3Client().build(),
        defaultSqsClient(),
        SeedTextExtractionConfig.fromEnvironment());
  }

  public SeedTextExtractionHandler(
      S3Client s3Client, SqsClient sqsClient, SeedTextExtractionConfig config) {
    this.s3Client = s3Client;
    this.sqsClient = sqsClient;
    this.config = config;
  }

  @JacocoGenerated
  private static SqsClient defaultSqsClient() {
    return SqsClient.builder()
        .region(
            new Environment().readEnvOpt(AWS_REGION_ENV).map(Region::of).orElse(Region.EU_WEST_1))
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();
  }

  @Override
  public Void handleRequest(S3Event event, Context context) {
    event.getRecords().forEach(this::processRecord);
    return null;
  }

  private void processRecord(S3EventNotificationRecord record) {
    var csvBucket = record.getS3().getBucket().getName();
    var csvKey = record.getS3().getObject().getUrlDecodedKey();
    var outcome = enqueueFromCsv(csvBucket, csvKey);
    LOGGER.info(
        "Seeded {} keys from s3://{}/{} ({} failed)",
        outcome.attempted(),
        LogSanitizer.sanitize(csvBucket),
        LogSanitizer.sanitize(csvKey),
        outcome.failed());
    if (outcome.failed() > 0) {
      throw new IllegalStateException(
          BATCH_FAILURES_MESSAGE_TEMPLATE.formatted(outcome.failed(), outcome.attempted()));
    }
  }

  private BatchOutcome enqueueFromCsv(String bucket, String key) {
    var objectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();
    try (var reader =
        new BufferedReader(
            new InputStreamReader(s3Client.getObject(objectRequest), StandardCharsets.UTF_8))) {
      return reader
          .lines()
          .map(SeedTextExtractionHandler::cleanKey)
          .filter(SeedTextExtractionHandler::containsAlphanumeric)
          .gather(Gatherers.windowFixed(BATCH_SIZE))
          .map(this::sendBatch)
          .reduce(BatchOutcome.NONE, BatchOutcome::plus);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static String cleanKey(String line) {
    return line.replace(UTF8_BYTE_ORDER_MARK, EMPTY_STRING).strip();
  }

  private static boolean containsAlphanumeric(String line) {
    return line.chars().anyMatch(Character::isLetterOrDigit);
  }

  private BatchOutcome sendBatch(List<String> keys) {
    var entries =
        IntStream.range(0, keys.size())
            .mapToObj(keyIndex -> batchEntry(keyIndex, keys.get(keyIndex)))
            .toList();
    SendMessageBatchResponse response;
    try {
      response =
          sqsClient.sendMessageBatch(
              SendMessageBatchRequest.builder()
                  .queueUrl(config.queueUrl())
                  .entries(entries)
                  .build());
    } catch (SdkException exception) {
      logWholeBatchFailure(keys, exception);
      return new BatchOutcome(0, keys.size());
    }
    var failedCount = logFailedEntries(response, keys);
    return new BatchOutcome(keys.size() - failedCount, failedCount);
  }

  private SendMessageBatchRequestEntry batchEntry(int entryIndex, String key) {
    return SendMessageBatchRequestEntry.builder()
        .id(String.valueOf(entryIndex))
        .messageBody(new TextExtractionRequest(config.sourceBucketName(), key).toJsonString())
        .build();
  }

  private static void logWholeBatchFailure(List<String> keys, SdkException exception) {
    keys.forEach(key -> LOGGER.warn("Failed to enqueue key: {}", LogSanitizer.sanitize(key)));
    LOGGER.error("SQS batch send failed for {} keys", keys.size(), exception);
  }

  private static int logFailedEntries(SendMessageBatchResponse response, List<String> keys) {
    response
        .failed()
        .forEach(
            failedEntry ->
                LOGGER.warn(
                    "Failed to enqueue key: {} code={}",
                    LogSanitizer.sanitize(keys.get(Integer.parseInt(failedEntry.id()))),
                    LogSanitizer.sanitize(failedEntry.code())));
    return response.failed().size();
  }

  private record BatchOutcome(int succeeded, int failed) {

    private static final BatchOutcome NONE = new BatchOutcome(0, 0);

    private BatchOutcome plus(BatchOutcome other) {
      return new BatchOutcome(succeeded + other.succeeded, failed + other.failed);
    }

    private int attempted() {
      return succeeded + failed;
    }
  }
}

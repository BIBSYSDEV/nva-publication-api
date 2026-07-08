package no.unit.nva.publication.file.text;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import java.util.List;
import java.util.stream.IntStream;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Reads a CSV file of S3 object keys uploaded to S3 and enqueues a {@link TextExtractionRequest}
 * for each key in batches of {@value BATCH_SIZE}. Used for bulk population of the text bucket.
 * Lines are stripped of a UTF-8 byte order mark and surrounding whitespace before enqueueing; lines
 * without any letter or digit are skipped.
 */
public final class SeedTextExtractionHandler implements RequestHandler<S3Event, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SeedTextExtractionHandler.class);
  private static final int BATCH_SIZE = 10;
  private static final String AWS_REGION_ENV = "AWS_REGION";
  private static final String UTF8_BYTE_ORDER_MARK = "\uFEFF";

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
    var keysEnqueued = enqueueFromCsv(csvBucket, csvKey);
    LOGGER.info(
        "Seeded {} keys from s3://{}/{}",
        keysEnqueued,
        LogSanitizer.sanitize(csvBucket),
        LogSanitizer.sanitize(csvKey));
  }

  private int enqueueFromCsv(String bucket, String key) {
    var keys = readEnqueueableKeys(bucket, key);
    var totalFailures = 0;
    for (var batchStart = 0; batchStart < keys.size(); batchStart += BATCH_SIZE) {
      var batchEnd = Math.min(batchStart + BATCH_SIZE, keys.size());
      totalFailures += sendBatch(keys.subList(batchStart, batchEnd));
    }
    if (totalFailures > 0) {
      throw new IllegalStateException(
          "SQS batch enqueue had " + totalFailures + " failed entries out of " + keys.size());
    }
    return keys.size();
  }

  private List<String> readEnqueueableKeys(String bucket, String key) {
    return new S3Driver(s3Client, bucket)
        .getFile(UnixPath.of(key))
        .lines()
        .map(SeedTextExtractionHandler::cleanKey)
        .filter(SeedTextExtractionHandler::containsAlphanumeric)
        .toList();
  }

  private static String cleanKey(String line) {
    return line.replace(UTF8_BYTE_ORDER_MARK, "").strip();
  }

  private static boolean containsAlphanumeric(String line) {
    return line.chars().anyMatch(Character::isLetterOrDigit);
  }

  private int sendBatch(List<String> keys) {
    var entries =
        IntStream.range(0, keys.size())
            .mapToObj(keyIndex -> batchEntry(keyIndex, keys.get(keyIndex)))
            .toList();
    var response =
        sqsClient.sendMessageBatch(
            SendMessageBatchRequest.builder().queueUrl(config.queueUrl()).entries(entries).build());
    return logFailedEntries(response, keys);
  }

  private SendMessageBatchRequestEntry batchEntry(int entryIndex, String key) {
    return SendMessageBatchRequestEntry.builder()
        .id(String.valueOf(entryIndex))
        .messageBody(new TextExtractionRequest(config.sourceBucketName(), key).toJsonString())
        .build();
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
}

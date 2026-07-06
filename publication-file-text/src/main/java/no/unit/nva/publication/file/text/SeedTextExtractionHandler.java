package no.unit.nva.publication.file.text;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Reads a CSV file of S3 object keys uploaded to S3 and enqueues a {@link TextExtractionRequest}
 * for each key in batches of {@value BATCH_SIZE}. Used for bulk population of the text bucket.
 */
public final class SeedTextExtractionHandler implements RequestHandler<S3Event, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SeedTextExtractionHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int BATCH_SIZE = 10;

  private final S3Client s3Client;
  private final SqsClient sqsClient;
  private final SeedTextExtractionConfig config;

  @JacocoGenerated
  public SeedTextExtractionHandler() {
    this(
        S3Driver.defaultS3Client().build(),
        SqsClient.builder().region(Region.of(System.getenv("AWS_REGION"))).build(),
        SeedTextExtractionConfig.fromEnvironment());
  }

  public SeedTextExtractionHandler(
      S3Client s3Client, SqsClient sqsClient, SeedTextExtractionConfig config) {
    this.s3Client = s3Client;
    this.sqsClient = sqsClient;
    this.config = config;
  }

  @Override
  public Void handleRequest(S3Event event, Context context) {
    event.getRecords().forEach(this::processRecord);
    return null;
  }

  private void processRecord(S3EventNotificationRecord record) {
    var csvBucket = record.getS3().getBucket().getName();
    var csvKey = record.getS3().getObject().getKey();
    var keys = readKeys(csvBucket, csvKey);
    enqueueInBatches(keys);
    LOGGER.info(
        "Seeded {} keys from s3://{}/{}",
        keys.size(),
        LogSanitizer.sanitize(csvBucket),
        LogSanitizer.sanitize(csvKey));
  }

  private List<String> readKeys(String bucket, String key) {
    return new S3Driver(s3Client, bucket)
        .getFile(UnixPath.of(key))
        .lines()
        .filter(Predicate.not(String::isBlank))
        .filter(SeedTextExtractionHandler::containsAlphanumeric)
        .toList();
  }

  private static boolean containsAlphanumeric(String line) {
    return line.chars().anyMatch(Character::isLetterOrDigit);
  }

  private void enqueueInBatches(List<String> keys) {
    IntStream.iterate(0, start -> start < keys.size(), start -> start + BATCH_SIZE)
        .mapToObj(start -> createBatch(keys, start))
        .forEach(this::sendBatch);
  }

  private static List<String> createBatch(List<String> keys, int start) {
    return keys.subList(start, Math.min(start + BATCH_SIZE, keys.size()));
  }

  private void sendBatch(List<String> keys) {
    var entries =
        IntStream.range(0, keys.size())
            .mapToObj(keyIndex -> batchEntry(keyIndex, createTextExtractionRequest(keys, keyIndex)))
            .toList();
    var response =
        sqsClient.sendMessageBatch(
            SendMessageBatchRequest.builder()
                .queueUrl(config.queueUrl())
                .entries(entries)
                .build());
    logBatchFailures(response, entries.size());
  }

  private static void logBatchFailures(SendMessageBatchResponse response, int batchSize) {
    if (!response.failed().isEmpty()) {
      LOGGER.warn(
          "SQS batch had {} failed entries out of {}", response.failed().size(), batchSize);
      throw new RuntimeException(
          "SQS batch failed for " + response.failed().size() + " of " + batchSize + " entries");
    }
  }

  private TextExtractionRequest createTextExtractionRequest(List<String> keys, int keyIndex) {
    return new TextExtractionRequest(config.sourceBucketName(), keys.get(keyIndex));
  }

  private static SendMessageBatchRequestEntry batchEntry(
      int entryIndex, TextExtractionRequest request) {
    try {
      return SendMessageBatchRequestEntry.builder()
          .id(String.valueOf(entryIndex))
          .messageBody(OBJECT_MAPPER.writeValueAsString(request))
          .build();
    } catch (JsonProcessingException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}

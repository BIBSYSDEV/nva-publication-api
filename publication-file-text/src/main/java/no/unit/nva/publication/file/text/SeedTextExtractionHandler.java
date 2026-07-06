package no.unit.nva.publication.file.text;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
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
  private static final int BATCH_SIZE = 10;
  private static final String AWS_REGION_ENV = "AWS_REGION";

  private final S3Client s3Client;
  private final SqsClient sqsClient;
  private final SeedTextExtractionConfig config;

  @JacocoGenerated
  public SeedTextExtractionHandler() {
    this(
        S3Driver.defaultS3Client().build(),
        SqsClient.builder().region(Region.of(new Environment().readEnv(AWS_REGION_ENV))).build(),
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
    var csvKey = record.getS3().getObject().getUrlDecodedKey();
    var keysEnqueued = enqueueFromCsv(csvBucket, csvKey);
    LOGGER.info(
        "Seeded {} keys from s3://{}/{}",
        keysEnqueued,
        LogSanitizer.sanitize(csvBucket),
        LogSanitizer.sanitize(csvKey));
  }

  private int enqueueFromCsv(String bucket, String key) {
    var objectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();
    var batch = new ArrayList<String>(BATCH_SIZE);
    var keysEnqueued = 0;
    var totalFailures = 0;
    try {
      try (var responseStream =
              s3Client.getObject(objectRequest, ResponseTransformer.toInputStream());
          var reader =
              new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.isBlank() && containsAlphanumeric(line)) {
            batch.add(line);
            keysEnqueued++;
            if (batch.size() == BATCH_SIZE) {
              totalFailures += sendBatch(batch);
              batch.clear();
            }
          }
        }
        if (!batch.isEmpty()) {
          totalFailures += sendBatch(batch);
        }
      }
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    if (totalFailures > 0) {
      throw new IllegalStateException(
          "SQS batch enqueue had " + totalFailures + " failed entries out of " + keysEnqueued);
    }
    return keysEnqueued;
  }

  private static boolean containsAlphanumeric(String line) {
    return line.chars().anyMatch(Character::isLetterOrDigit);
  }

  private int sendBatch(List<String> keys) {
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
    return countBatchFailures(response, entries.size());
  }

  private static int countBatchFailures(SendMessageBatchResponse response, int batchSize) {
    if (!response.failed().isEmpty()) {
      LOGGER.warn(
          "SQS batch had {} failed entries out of {}", response.failed().size(), batchSize);
    }
    return response.failed().size();
  }

  private TextExtractionRequest createTextExtractionRequest(List<String> keys, int keyIndex) {
    return new TextExtractionRequest(config.sourceBucketName(), keys.get(keyIndex));
  }

  private static SendMessageBatchRequestEntry batchEntry(
      int entryIndex, TextExtractionRequest request) {
    try {
      return SendMessageBatchRequestEntry.builder()
          .id(String.valueOf(entryIndex))
          .messageBody(JsonUtils.dtoObjectMapper.writeValueAsString(request))
          .build();
    } catch (JsonProcessingException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}

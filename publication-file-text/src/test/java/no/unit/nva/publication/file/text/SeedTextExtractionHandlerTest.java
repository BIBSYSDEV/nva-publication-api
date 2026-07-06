package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

class SeedTextExtractionHandlerTest {

  private static final String CSV_BUCKET = "csv-bucket";
  private static final String CSV_KEY = "seed.csv";
  private static final String SOURCE_BUCKET = "source-bucket";
  private static final String QUEUE_URL =
      "https://sqs.eu-west-1.amazonaws.com/someAccount/someQueue";

  private FakeS3Client fakeS3Client;
  private SqsClient sqsClient;
  private SeedTextExtractionConfig config;

  @BeforeEach
  void setUp() {
    fakeS3Client = new FakeS3Client();
    sqsClient = mock(SqsClient.class);
    config = new SeedTextExtractionConfig(SOURCE_BUCKET, QUEUE_URL);
    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(SendMessageBatchResponse.builder().build());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"\n\n", "  ", "\r", "\r\n", "\t", ";", ","})
  void shouldSendNoMessagesWhenCsvHasNoContent(String value) throws IOException {
    insertCsv(value);

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    verifyNoInteractions(sqsClient);
  }

  @Test
  void shouldSendSingleBatchForTenOrFewerKeys() throws IOException {
    insertCsv(csvWithKeys(10));

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    verify(sqsClient, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
  }

  @Test
  void shouldPartitionKeysIntoBatchesOfTen() throws IOException {
    insertCsv(csvWithKeys(25));

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(sqsClient, times(3)).sendMessageBatch(captor.capture());
    assertThat(captor.getAllValues())
        .allSatisfy(batch -> assertThat(batch.entries()).hasSizeLessThanOrEqualTo(10));
    var totalEnqueued =
        captor.getAllValues().stream().mapToInt(batch -> batch.entries().size()).sum();
    assertThat(totalEnqueued).isEqualTo(25);
  }

  @Test
  void shouldIncludeSourceBucketAndKeyInMessageBody() throws IOException {
    var key = "publications/2024/paper.pdf";
    insertCsv(key);

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(sqsClient).sendMessageBatch(captor.capture());
    var body = captor.getValue().entries().getFirst().messageBody();
    assertThat(body).contains(SOURCE_BUCKET).contains(key);
  }

  @Test
  void shouldSendToConfiguredQueueUrl() throws IOException {
    insertCsv("key.pdf");

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(sqsClient).sendMessageBatch(captor.capture());
    assertThat(captor.getValue().queueUrl()).isEqualTo(QUEUE_URL);
  }

  @Test
  void shouldReadConfigFromEnvironment() {
    var envConfig = SeedTextExtractionConfig.fromEnvironment();

    assertThat(envConfig.sourceBucketName()).isEqualTo(SOURCE_BUCKET);
    assertThat(envConfig.queueUrl()).isEqualTo(QUEUE_URL);
  }

  private SeedTextExtractionHandler handler() {
    return new SeedTextExtractionHandler(fakeS3Client, sqsClient, config);
  }

  private void insertCsv(String content) throws IOException {
    new S3Driver(fakeS3Client, CSV_BUCKET)
        .insertFile(UnixPath.of(CSV_KEY), Objects.requireNonNullElse(content, ""));
  }

  private static String csvWithKeys(int count) {
    return IntStream.rangeClosed(1, count)
        .mapToObj(i -> "publications/doc" + i + ".pdf")
        .collect(Collectors.joining("\n"));
  }

  private static S3Event s3Event(String bucket, String key) {
    var bucketEntity = new S3BucketEntity(bucket, null, null);
    var objectEntity = new S3ObjectEntity(key, null, null, null, null);
    var s3Entity = new S3Entity(null, bucketEntity, objectEntity, null);
    var record =
        new S3EventNotificationRecord(null, null, null, null, null, null, null, s3Entity, null);
    return new S3Event(List.of(record));
  }
}

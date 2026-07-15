package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import no.unit.nva.publication.queue.QueueMessageSender;
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
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

class SeedTextExtractionHandlerTest {

  private static final String CSV_BUCKET = "csv-bucket";
  private static final String CSV_KEY = "seed.csv";
  private static final String SOURCE_BUCKET = "source-bucket";
  private static final String QUEUE_URL =
      "https://sqs.eu-west-1.amazonaws.com/someAccount/someQueue";
  private static final int MAX_ENTRIES_PER_BATCH = 10;
  private static final int KEYS_SPANNING_THREE_BATCHES = 25;
  private static final int EXPECTED_BATCH_COUNT = 3;

  private FakeS3Client fakeS3Client;
  private QueueMessageSender queueMessageSender;
  private SeedTextExtractionConfig config;

  @BeforeEach
  void setUp() {
    fakeS3Client = new FakeS3Client();
    queueMessageSender = mock(QueueMessageSender.class);
    config = new SeedTextExtractionConfig(SOURCE_BUCKET, QUEUE_URL);
    when(queueMessageSender.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(SendMessageBatchResponse.builder().build());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"\n\n", "  ", "\r", "\r\n", "\t", ";", ","})
  void shouldSendNoMessagesWhenCsvHasNoContent(String value) throws IOException {
    insertCsv(value);

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    verifyNoInteractions(queueMessageSender);
  }

  @Test
  void shouldSendSingleBatchForTenOrFewerKeys() throws IOException {
    insertCsv(csvWithKeys(MAX_ENTRIES_PER_BATCH));

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    verify(queueMessageSender, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
  }

  @Test
  void shouldPartitionKeysIntoBatchesOfTen() throws IOException {
    insertCsv(csvWithKeys(KEYS_SPANNING_THREE_BATCHES));

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(queueMessageSender, times(EXPECTED_BATCH_COUNT)).sendMessageBatch(captor.capture());
    assertThat(captor.getAllValues())
        .allSatisfy(
            batch -> assertThat(batch.entries()).hasSizeLessThanOrEqualTo(MAX_ENTRIES_PER_BATCH));
    var totalEnqueued =
        captor.getAllValues().stream().mapToInt(batch -> batch.entries().size()).sum();
    assertThat(totalEnqueued).isEqualTo(KEYS_SPANNING_THREE_BATCHES);
  }

  @Test
  void shouldIncludeSourceBucketAndKeyInMessageBody() throws IOException {
    var key = "publications/2024/paper.pdf";
    insertCsv(key);

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(queueMessageSender).sendMessageBatch(captor.capture());
    var body = captor.getValue().entries().getFirst().messageBody();
    assertThat(body).contains(SOURCE_BUCKET).contains(key);
  }

  @Test
  void shouldSendToConfiguredQueueUrl() throws IOException {
    insertCsv("key.pdf");

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(queueMessageSender).sendMessageBatch(captor.capture());
    assertThat(captor.getValue().queueUrl()).isEqualTo(QUEUE_URL);
  }

  @Test
  void shouldThrowWhenBatchContainsFailedEntries() throws IOException {
    var failedEntry =
        BatchResultErrorEntry.builder()
            .id("0")
            .code("ThrottlingException")
            .message("Rate exceeded")
            .senderFault(false)
            .build();
    when(queueMessageSender.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(SendMessageBatchResponse.builder().failed(failedEntry).build());
    insertCsv("publications/doc1.pdf");

    assertThatThrownBy(
            () -> handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldSendAllBatchesEvenWhenOneBatchHasSqsFailures() throws IOException {
    var failedEntry =
        BatchResultErrorEntry.builder()
            .id("0")
            .code("ThrottlingException")
            .message("Rate exceeded")
            .senderFault(false)
            .build();
    when(queueMessageSender.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(SendMessageBatchResponse.builder().failed(failedEntry).build())
        .thenReturn(SendMessageBatchResponse.builder().build())
        .thenReturn(SendMessageBatchResponse.builder().build());
    insertCsv(csvWithKeys(KEYS_SPANNING_THREE_BATCHES));

    assertThatThrownBy(
            () -> handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext()))
        .isInstanceOf(IllegalStateException.class);

    verify(queueMessageSender, times(EXPECTED_BATCH_COUNT))
        .sendMessageBatch(any(SendMessageBatchRequest.class));
  }

  @Test
  void shouldContinueRemainingBatchesAndThrowWhenWholeBatchSendFails() throws IOException {
    when(queueMessageSender.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenThrow(SqsException.builder().message("SQS unavailable").build())
        .thenReturn(SendMessageBatchResponse.builder().build())
        .thenReturn(SendMessageBatchResponse.builder().build());
    insertCsv(csvWithKeys(KEYS_SPANNING_THREE_BATCHES));

    assertThatThrownBy(
            () -> handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext()))
        .isInstanceOf(IllegalStateException.class);

    verify(queueMessageSender, times(EXPECTED_BATCH_COUNT))
        .sendMessageBatch(any(SendMessageBatchRequest.class));
  }

  @Test
  void shouldStripByteOrderMarkAndSurroundingWhitespaceFromCsvLines() throws IOException {
    insertCsv("\uFEFF" + "publications/doc1.pdf  \n\tpublications/doc2.pdf \r\n");

    handler().handleRequest(s3Event(CSV_BUCKET, CSV_KEY), new FakeContext());

    var captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
    verify(queueMessageSender).sendMessageBatch(captor.capture());
    var bodies =
        captor.getValue().entries().stream()
            .map(SendMessageBatchRequestEntry::messageBody)
            .toList();
    assertThat(bodies).hasSize(2);
    assertThat(bodies.getFirst()).contains("\"publications/doc1.pdf\"");
    assertThat(bodies.getLast()).contains("\"publications/doc2.pdf\"");
  }

  @Test
  void shouldHandleUrlEncodedCsvKey() throws IOException {
    var decodedKey = "seed file.csv";
    new S3Driver(fakeS3Client, CSV_BUCKET)
        .insertFile(UnixPath.of(decodedKey), "publications/doc1.pdf");

    handler().handleRequest(s3Event(CSV_BUCKET, "seed+file.csv"), new FakeContext());

    verify(queueMessageSender, times(1)).sendMessageBatch(any(SendMessageBatchRequest.class));
  }

  @Test
  void shouldReadConfigFromEnvironment() {
    var envConfig = SeedTextExtractionConfig.fromEnvironment();

    assertThat(envConfig.sourceBucketName()).isEqualTo(SOURCE_BUCKET);
    assertThat(envConfig.queueUrl()).isEqualTo(QUEUE_URL);
  }

  private SeedTextExtractionHandler handler() {
    return new SeedTextExtractionHandler(fakeS3Client, queueMessageSender, config);
  }

  private void insertCsv(String content) throws IOException {
    new S3Driver(fakeS3Client, CSV_BUCKET)
        .insertFile(UnixPath.of(CSV_KEY), Objects.requireNonNullElse(content, ""));
  }

  private static String csvWithKeys(int count) {
    return IntStream.rangeClosed(1, count)
        .mapToObj(keyNumber -> "publications/doc" + keyNumber + ".pdf")
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

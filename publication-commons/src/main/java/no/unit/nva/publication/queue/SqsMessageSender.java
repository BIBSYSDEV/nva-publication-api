package no.unit.nva.publication.queue;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Send-only {@link QueueMessageSender} backed by an {@link SqsClient}. It holds no queue URL: the
 * target queue comes from each batch request, so a single instance can publish to any queue.
 */
@JacocoGenerated
public final class SqsMessageSender implements QueueMessageSender {

  private static final String AWS_REGION = "AWS_REGION";
  private final SqsClient sqsClient;

  private SqsMessageSender(SqsClient sqsClient) {
    this.sqsClient = sqsClient;
  }

  public static SqsMessageSender defaultSqsMessageSender() {
    return new SqsMessageSender(defaultClient());
  }

  @Override
  public SendMessageBatchResponse sendMessageBatch(
      SendMessageBatchRequest sendMessageBatchRequest) {
    return sqsClient.sendMessageBatch(sendMessageBatchRequest);
  }

  private static SqsClient defaultClient() {
    return SqsClient.builder().region(getRegion()).httpClient(ApacheHttpClient.create()).build();
  }

  private static Region getRegion() {
    return new Environment().readEnvOpt(AWS_REGION).map(Region::of).orElse(Region.EU_WEST_1);
  }
}

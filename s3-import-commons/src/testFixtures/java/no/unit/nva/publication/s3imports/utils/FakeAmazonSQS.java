package no.unit.nva.publication.s3imports.utils;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;

public class FakeAmazonSQS implements SqsClient {

  private final List<String> messageBodies = new ArrayList<>();

  public List<String> getMessageBodies() {
    return messageBodies;
  }

  @Override
  public String serviceName() {
    return SERVICE_NAME;
  }

  @Override
  public void close() {
    // No resources to release in the fake.
  }

  @Override
  public SendMessageBatchResponse sendMessageBatch(
      SendMessageBatchRequest sendMessageBatchRequest) {
    sendMessageBatchRequest.entries().stream()
        .map(SendMessageBatchRequestEntry::messageBody)
        .forEach(messageBodies::add);
    var messageBatchResultEntries =
        sendMessageBatchRequest.entries().stream().map(this::convertToResult).toList();
    return SendMessageBatchResponse.builder().successful(messageBatchResultEntries).build();
  }

  private SendMessageBatchResultEntry convertToResult(
      SendMessageBatchRequestEntry sendMessageBatchRequestEntry) {
    return SendMessageBatchResultEntry.builder()
        .messageId(sendMessageBatchRequestEntry.id())
        .build();
  }
}

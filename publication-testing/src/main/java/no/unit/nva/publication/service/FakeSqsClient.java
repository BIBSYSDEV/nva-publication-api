package no.unit.nva.publication.service;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.QueueMessageSender;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class FakeSqsClient implements QueueClient, QueueMessageSender {

  private final List<Message> deliveredMessages = new ArrayList<>();

  public List<Message> getDeliveredMessages() {
    return deliveredMessages;
  }

  public List<String> getMessageBodies() {
    return deliveredMessages.stream().map(Message::body).toList();
  }

  @Override
  public void sendMessage(SendMessageRequest sendMessageRequest) {
    var message =
        Message.builder()
            .messageAttributes(sendMessageRequest.messageAttributes())
            .body(sendMessageRequest.messageBody())
            .messageId(randomString())
            .build();
    deliveredMessages.add(message);
  }

  @Override
  public SendMessageBatchResponse sendMessageBatch(
      SendMessageBatchRequest sendMessageBatchRequest) {
    sendMessageBatchRequest
        .entries()
        .forEach(
            entry ->
                deliveredMessages.add(
                    Message.builder().body(entry.messageBody()).messageId(randomString()).build()));
    var resultEntries =
        sendMessageBatchRequest.entries().stream()
            .map(SendMessageBatchRequestEntry::id)
            .map(id -> SendMessageBatchResultEntry.builder().id(id).build())
            .toList();
    return SendMessageBatchResponse.builder().successful(resultEntries).build();
  }

  @Override
  public List<Message> readMessages(int maximumNumberOfMessages) {
    var max = maximumNumberOfMessages > 10 ? 10 : maximumNumberOfMessages;
    int toIndex = Math.min(deliveredMessages.size(), max);
    return new ArrayList<>(deliveredMessages.subList(0, toIndex));
  }

  @Override
  public void deleteMessages(List<Message> messages) {
    var messagesToRemove =
        messages.stream()
            .map(this::findMatchingMessageInDeliveredMessages)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    deliveredMessages.removeAll(messagesToRemove);
  }

  private Optional<Message> findMatchingMessageInDeliveredMessages(Message message) {
    return deliveredMessages.stream()
        .filter(item -> item.messageId().equals(message.messageId()))
        .findFirst();
  }
}

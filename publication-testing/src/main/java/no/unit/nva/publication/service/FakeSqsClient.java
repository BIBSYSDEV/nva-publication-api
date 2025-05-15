package no.unit.nva.publication.service;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.unit.nva.publication.queue.QueueClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class FakeSqsClient implements QueueClient {

    private final List<Message> deliveredMessages = new ArrayList<>();

    public List<Message> getDeliveredMessages() {
        return deliveredMessages;
    }

    @Override
    public void sendMessage(SendMessageRequest sendMessageRequest) {
        var message = Message.builder()
                          .messageAttributes(sendMessageRequest.messageAttributes())
                          .body(sendMessageRequest.messageBody())
                          .messageId(randomString())
                          .build();
        deliveredMessages.add(message);
    }

    @Override
    public List<Message> readMessages(int maximumNumberOfMessages) {
        var max = maximumNumberOfMessages > 10 ? 10 : maximumNumberOfMessages;
        int toIndex = Math.min(deliveredMessages.size(), max);
        return new ArrayList<>(deliveredMessages.subList(0, toIndex));
    }

    @Override
    public void deleteMessages(List<Message> messages) {
        var messagesToRemove = messages.stream()
                                             .map(this::findMatchingMessageInDeliveredMessages)
                                             .filter(Optional::isPresent)
                                             .map(Optional::get)
                                             .toList();

        deliveredMessages.removeAll(messagesToRemove);
    }

    private Optional<Message> findMatchingMessageInDeliveredMessages(Message message) {
        return deliveredMessages.stream().filter(item -> item.messageId().equals(message.messageId())).findFirst();
    }
}

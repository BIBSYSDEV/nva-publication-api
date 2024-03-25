package no.unit.nva.publication.queue;

import java.util.List;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public interface QueueClient {

    void sendMessage(SendMessageRequest sendMessageRequest);

    List<Message> readMessages();

    void deleteMessages(List<Message> messages);
}

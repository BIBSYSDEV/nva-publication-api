package no.unit.nva.publication.queue;

import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public interface QueueClient {

    void sendMessage(SendMessageRequest sendMessageRequest);

}

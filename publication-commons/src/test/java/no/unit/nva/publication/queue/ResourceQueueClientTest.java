package no.unit.nva.publication.queue;

import no.unit.nva.publication.service.FakeSqsClient;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class ResourceQueueClientTest {

    @Test
    void shouldSendMessage() {
        new FakeSqsClient().sendMessage(SendMessageRequest.builder().build());
    }
}

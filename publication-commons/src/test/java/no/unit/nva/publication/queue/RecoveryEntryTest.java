package no.unit.nva.publication.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.FakeSqsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecoveryEntryTest {

    private QueueClient queueClient;

    @BeforeEach
    void setUp() {
        this.queueClient = new FakeSqsClient();
    }

    @Test
    void shouldPersistRecoveryEntry() {
        var type = RecoveryEntry.RESOURCE;
        var identifier = SortableIdentifier.next();

        RecoveryEntry.create(type, identifier)
            .withException(new Exception())
            .persist(queueClient);

        var message = queueClient.readMessages(1).getFirst();

        assertEquals(type, message.messageAttributes().get("type").stringValue());
        assertEquals(identifier.toString(), message.messageAttributes().get("id").stringValue());
    }
}
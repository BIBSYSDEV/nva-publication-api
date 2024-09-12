package no.unit.nva.publication.events.bodies;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;
import org.junit.jupiter.api.Test;

class DoiMetadataUpdateEventTest {


    @Test
    void shouldCreateEventWithPublicationId() {
        var publication = randomPublication();
        var host = "host.no";
        var event = DoiMetadataUpdateEvent.createUpdateDoiEvent(publication, host);

        var expectedId = URI.create("https://host.no/publication/" + publication.getIdentifier());

        assertEquals(event.getPublicationId(), expectedId);
    }
}
package no.unit.nva.publication.events.handlers.bodies;

import static no.unit.nva.publication.events.bodies.DynamoEntryUpdateEvent.PUBLICATION_UPDATE_TYPE;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.bodies.DynamoEntryUpdateEvent;
import no.unit.nva.publication.storage.model.Resource;
import org.junit.jupiter.api.Test;

public class DynamoEntryUpdateEventTest {

    public static final Resource NO_VALUE = null;
    public static final String PUBLISHER_ID = "http://example.org/publisher/123";
    public static final String OWNER = "owner";

    @Test
    public void writePublicationUpdateEventToJsonAndReadBackAsObject() throws JsonProcessingException {
        var event = new DynamoEntryUpdateEvent(
            PUBLICATION_UPDATE_TYPE,
            createPublication(),
            NO_VALUE);
        var json = objectMapper.writeValueAsString(event);
        var mappedEvent = objectMapper.readValue(json, DynamoEntryUpdateEvent.class);

        assertThat(event, equalTo(mappedEvent));
    }

    private Resource createPublication() {
        Publication publication = new Publication.Builder()
                .withIdentifier(SortableIdentifier.next())
                .withModifiedDate(Instant.now())
                .withOwner(OWNER)
                .withPublisher(new Organization.Builder()
                        .withId(URI.create(PUBLISHER_ID))
                        .build()
                )
                .build();
        return Resource.fromPublication(publication);
    }

}

package no.unit.nva.publication.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;

import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static no.unit.nva.publication.events.PublicationUpdateEvent.PUBLICATION_UPDATE_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PublicationUpdateEventTest {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    public static final Publication NO_VALUE = null;
    public static final String INSERT_UPDATE_TYPE = "INSERT";
    public static final String PUBLISHER_ID = "http://example.org/publisher/123";
    public static final String OWNER = "owner";

    @Test
    public void writePublicationUpdateEventToJsonAndReadBackAsObject() throws JsonProcessingException {
        var event = new PublicationUpdateEvent(
                PUBLICATION_UPDATE_TYPE,
                INSERT_UPDATE_TYPE,
                createPublication(),
                NO_VALUE);
        var json = objectMapper.writeValueAsString(event);
        var mappedEvent = objectMapper.readValue(json, PublicationUpdateEvent.class);

        assertThat(event, equalTo(mappedEvent));
    }

    private Publication createPublication() {
        return new Publication.Builder()
                .withIdentifier(UUID.randomUUID())
                .withModifiedDate(Instant.now())
                .withOwner(OWNER)
                .withPublisher(new Organization.Builder()
                        .withId(URI.create(PUBLISHER_ID))
                        .build()
                )
                .build();
    }

}

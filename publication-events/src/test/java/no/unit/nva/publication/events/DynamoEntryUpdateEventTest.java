package no.unit.nva.publication.events;

import static no.unit.nva.publication.events.DynamoEntryUpdateEvent.PUBLICATION_UPDATE_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class DynamoEntryUpdateEventTest {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    public static final Publication NO_VALUE = null;
    public static final String INSERT_UPDATE_TYPE = "INSERT";
    public static final String PUBLISHER_ID = "http://example.org/publisher/123";
    public static final String OWNER = "owner";

    @Test
    public void writePublicationUpdateEventToJsonAndReadBackAsObject() throws JsonProcessingException {
        var event = new DynamoEntryUpdateEvent(
                PUBLICATION_UPDATE_TYPE,
                INSERT_UPDATE_TYPE,
                createPublication(),
                NO_VALUE);
        var json = objectMapper.writeValueAsString(event);
        var mappedEvent = objectMapper.readValue(json, DynamoEntryUpdateEvent.class);

        assertThat(event, equalTo(mappedEvent));
    }

    private Publication createPublication() {
        return new Publication.Builder()
                .withIdentifier(SortableIdentifier.next())
                .withModifiedDate(Instant.now())
                .withOwner(OWNER)
                .withPublisher(new Organization.Builder()
                        .withId(URI.create(PUBLISHER_ID))
                        .build()
                )
                .build();
    }

}

package no.unit.nva.publication.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.PublicationStatus;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class DeletePublicationEventTest {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    public static final String SOME_DOI = "http://example.org/doi/123";

    @Test
    public void writeDeletePublicationEventToJsonAndReadBackAsObject() throws JsonProcessingException {
        var event = new DeletePublicationEvent(
                DeletePublicationEvent.DELETE_PUBLICATION,
                UUID.randomUUID(),
                PublicationStatus.DRAFT_FOR_DELETION.getValue(),
                URI.create(SOME_DOI));

        var json = objectMapper.writeValueAsString(event);
        var mappedEvent = objectMapper.readValue(json, DeletePublicationEvent.class);

        assertThat(event, equalTo(mappedEvent));
    }

}

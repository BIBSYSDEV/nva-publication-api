package no.unit.nva.publication.events;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationStatus;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class DeletePublicationEventTest {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    public static final String SOME_DOI = "http://example.org/doi/123";
    public static final String SOME_CUSTOMER = "http://example.org/customer/123";

    @Test
    public void writeDeletePublicationEventToJsonAndReadBackAsObject() throws JsonProcessingException {
        var event = new DeletePublicationEvent(
                DeletePublicationEvent.DELETE_PUBLICATION,
               SortableIdentifier.next(),
                PublicationStatus.DRAFT_FOR_DELETION.getValue(),
                URI.create(SOME_DOI),
                URI.create(SOME_CUSTOMER));

        var json = objectMapper.writeValueAsString(event);
        var mappedEvent = objectMapper.readValue(json, DeletePublicationEvent.class);

        assertThat(event, equalTo(mappedEvent));
    }

}

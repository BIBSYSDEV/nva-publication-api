package no.unit.nva.publication.ticket;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.Test;

public class TicketRequestTest {

    @Test
    @Deprecated
    public void shouldSerializeRequestBodyWithoutTypeAsUpdateTicketRequest() throws JsonProcessingException {
        var json = """
            {
            }
            """;
        var request = JsonUtils.dtoObjectMapper.readValue(json, TicketRequest.class);

        assertInstanceOf(UpdateTicketRequest.class, request);
    }
}
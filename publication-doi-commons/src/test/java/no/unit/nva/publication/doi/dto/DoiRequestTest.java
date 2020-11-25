package no.unit.nva.publication.doi.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import no.unit.nva.publication.doi.dto.DoiRequest.Builder;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

public class DoiRequestTest {

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private static final String INVALID_STATUS = "invalid_status";

    @Test
    public void canWriteToAndReadFromJson() throws JsonProcessingException {
        DoiRequest doiRequest = new Builder()
            .withStatus(DoiRequestStatus.APPROVED)
            .withModifiedDate(Instant.now())
            .build();

        assertNotNull(doiRequest.getStatus());
        assertNotNull(doiRequest.getModifiedDate());

        String json = objectMapper.writeValueAsString(doiRequest);

        DoiRequest parsedDoiRequest = objectMapper.readValue(json, DoiRequest.class);

        assertEquals(doiRequest, parsedDoiRequest);
    }

    @Test
    public void lookupInvalidDoiRequestStatusThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DoiRequestStatus.lookup(INVALID_STATUS));
    }

}

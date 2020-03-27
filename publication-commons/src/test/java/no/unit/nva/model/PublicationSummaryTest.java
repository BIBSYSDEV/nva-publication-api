package no.unit.nva.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.PublicationHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicationSummaryTest {

    private ObjectMapper objectMapper = PublicationHandler.createObjectMapper();

    @Test
    @DisplayName("objectMapper Can Write And Read PublicationSummary")
    public void objectMapperCanWriteAndReadPublicationSummary() throws JsonProcessingException {
        PublicationSummary publicationSummary = publicationSummary();
        PublicationSummary processedPublicationSummary = objectMapper.readValue(
                objectMapper.writeValueAsString(publicationSummary), PublicationSummary.class);

        assertEquals(publicationSummary.getMainTitle(), processedPublicationSummary.getMainTitle());
    }

    private PublicationSummary publicationSummary() {
        Instant now = Instant.now();
        return new PublicationSummary.Builder()
                .withCreatedDate(now)
                .withModifiedDate(now)
                .withIdentifier(UUID.randomUUID())
                .withMainTitle("Main Title")
                .withOwner("Owner")
                .withStatus(PublicationStatus.DRAFT)
                .build();
    }

}

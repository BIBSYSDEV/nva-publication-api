package no.unit.nva.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.PublicationHandler;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.time.Instant;
import java.util.UUID;

public class PublicationSummaryTest {

    private ObjectMapper objectMapper = PublicationHandler.createObjectMapper();

    @Test
    public void objectMapping() throws JsonProcessingException {
        PublicationSummary publicationSummary = getPublicationSummary();
        PublicationSummary processedPublicationSummary = objectMapper.readValue(
                objectMapper.writeValueAsString(publicationSummary), PublicationSummary.class);

        Assertions.assertEquals(publicationSummary.getMainTitle(), processedPublicationSummary.getMainTitle());
    }

    private PublicationSummary getPublicationSummary() {
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

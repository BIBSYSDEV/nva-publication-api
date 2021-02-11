package no.unit.nva.publication.model;

import static nva.commons.core.JsonUtils.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PublicationSummaryTest {

    @Test
    @DisplayName("objectMapper Can Write And Read PublicationSummary")
    public void objectMapperCanWriteAndReadPublicationSummary() throws JsonProcessingException {
        PublicationSummary publicationSummary = publicationSummary();
        PublicationSummary processedPublicationSummary = objectMapper.readValue(
            objectMapper.writeValueAsString(publicationSummary), PublicationSummary.class);

        assertEquals(publicationSummary, processedPublicationSummary);
    }

    private PublicationSummary publicationSummary() {
        Instant now = Instant.now();
        return new PublicationSummary.Builder()
            .withCreatedDate(now)
            .withModifiedDate(now)
            .withIdentifier(SortableIdentifier.next())
            .withMainTitle("Main Title")
            .withOwner("Owner")
            .withStatus(PublicationStatus.DRAFT)
            .build();
    }
}

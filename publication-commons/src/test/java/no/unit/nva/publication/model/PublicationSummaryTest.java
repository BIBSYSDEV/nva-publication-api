package no.unit.nva.publication.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PublicationSummaryTest {
    
    @Test
    @DisplayName("objectMapper Can Write And Read PublicationSummary")
    public void objectMapperCanWriteAndReadPublicationSummary() throws JsonProcessingException {
        PublicationSummary publicationSummary = publicationSummary();
        String content = objectMapper.writeValueAsString(publicationSummary);
        PublicationSummary processedPublicationSummary =
            objectMapper.readValue(content, PublicationSummary.class);
        
        assertEquals(publicationSummary, processedPublicationSummary);
    }
    
    @Test
    public void fromPublicationReturnsPublicationSummaryWithoutEmptyFields() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        PublicationSummary summary = PublicationSummary.fromPublication(publication);
        assertThat(summary, doesNotHaveEmptyValues());
        assertThat(summary.getIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(summary.getMainTitle(), is(equalTo(publication.getEntityDescription().getMainTitle())));
        assertThat(summary.getOwner(), is(equalTo(publication.getOwner())));
        assertThat(summary.getStatus(), is(equalTo(publication.getStatus())));
        assertThat(summary.getCreatedDate(), is(equalTo(publication.getCreatedDate())));
        assertThat(summary.getModifiedDate(), is(equalTo(publication.getModifiedDate())));
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

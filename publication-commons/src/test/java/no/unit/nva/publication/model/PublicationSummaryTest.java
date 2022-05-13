package no.unit.nva.publication.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.DoiRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicationSummaryTest {
    
    @Test
    @DisplayName("objectMapper Can Write And Read PublicationSummary")
    void objectMapperCanWriteAndReadPublicationSummary() throws JsonProcessingException {
        PublicationSummary publicationSummary = publicationSummary();
        String content = dtoObjectMapper.writeValueAsString(publicationSummary);
        PublicationSummary processedPublicationSummary =
            dtoObjectMapper.readValue(content, PublicationSummary.class);
        
        assertEquals(publicationSummary, processedPublicationSummary);
    }
    
    @Test
    void fromPublicationReturnsPublicationSummaryWithoutEmptyFields() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        PublicationSummary summary = PublicationSummary.create(publication);
        assertThat(summary, doesNotHaveEmptyValues());
        assertThat(summary.getPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(summary.getTitle(), is(equalTo(publication.getEntityDescription().getMainTitle())));
        assertThat(summary.getOwner(), is(equalTo(publication.getOwner())));
        assertThat(summary.getStatus(), is(equalTo(publication.getStatus())));
        assertThat(summary.getCreatedDate(), is(equalTo(publication.getCreatedDate())));
        assertThat(summary.getModifiedDate(), is(equalTo(publication.getModifiedDate())));
    }



    @Test
    void shouldCreatePublicationSummaryFromDoiRequest() {
        DoiRequest doiRequest =
            DoiRequest.fromPublication(PublicationGenerator.randomPublication(), SortableIdentifier.next());
        PublicationSummary publicationSummary = PublicationSummary.create(doiRequest);
        assertThat(publicationSummary.getPublicationIdentifier(), is(equalTo(doiRequest.getResourceIdentifier())));
    }

    private PublicationSummary publicationSummary() {
        return PublicationSummary.create(PublicationGenerator.randomPublication());
    }
}

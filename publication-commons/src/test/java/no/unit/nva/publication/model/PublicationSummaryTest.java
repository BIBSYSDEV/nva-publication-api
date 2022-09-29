package no.unit.nva.publication.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicationSummaryTest extends ResourcesLocalTest {
    
    @BeforeEach
    public void setup() {
        super.init();
    }
    
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
        assertThat(summary.extractPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(summary.getTitle(), is(equalTo(publication.getEntityDescription().getMainTitle())));
        assertThat(summary.getOwner(), is(equalTo(new User(publication.getResourceOwner().getOwner()))));
        assertThat(summary.getCreatedDate(), is(equalTo(publication.getCreatedDate())));
        assertThat(summary.getModifiedDate(), is(equalTo(publication.getModifiedDate())));
    }
    
    @Test
    void fromPublicationDetailsReturnsPublicationSummaryWithoutEmptyFields() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        PublicationSummary summary = PublicationSummary.create(PublicationDetails.create(publication));
        assertThat(summary, doesNotHaveEmptyValues());
        assertThat(summary.extractPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(summary.getTitle(), is(equalTo(publication.getEntityDescription().getMainTitle())));
        assertThat(summary.getOwner(), is(equalTo(new User(publication.getResourceOwner().getOwner()))));
        assertThat(summary.getCreatedDate(), is(equalTo(publication.getCreatedDate())));
        assertThat(summary.getModifiedDate(), is(equalTo(publication.getModifiedDate())));
    }
    
    @Test
    void shouldAllowCreationOfMinimumPossibleInformation() {
        var publicationId = randomPublicationId();
        var publicationTitle = randomString();
        var summary = PublicationSummary.create(publicationId, publicationTitle);
        assertThat(summary.getPublicationId(), is(equalTo(publicationId)));
        assertThat(summary.getTitle(), is(equalTo(publicationTitle)));
    }
    
    private URI randomPublicationId() {
        return UriWrapper.fromUri(randomUri()).addChild(SortableIdentifier.next().toString()).getUri();
    }
    
    private PublicationSummary publicationSummary() {
        return PublicationSummary.create(randomPublication());
    }
}

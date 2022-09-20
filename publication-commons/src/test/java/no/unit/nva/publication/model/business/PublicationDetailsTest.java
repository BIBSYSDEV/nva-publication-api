package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import no.unit.nva.publication.model.PublicationSummary;
import org.junit.jupiter.api.Test;

class PublicationDetailsTest {
    
    @Test
    void shouldBeCreatedFromPublicationSummary() {
        var publication = randomPublication();
        var fromPublication = PublicationDetails.create(publication);
        var publicationSummary = PublicationSummary.create(publication);
        var fromSummary = PublicationDetails.create(publicationSummary);
        assertThat(fromSummary, is(equalTo(fromPublication)));
    }
}
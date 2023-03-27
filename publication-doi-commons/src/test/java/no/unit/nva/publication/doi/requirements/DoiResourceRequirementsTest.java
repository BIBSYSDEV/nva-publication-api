package no.unit.nva.publication.doi.requirements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.testing.PublicationGenerator;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class DoiResourceRequirementsTest {

    @Test
    void shouldReturnFalseForPublicaitonMissingYear(){
        var publication = createSamplePublication();
        publication.getEntityDescription().getDate().setYear(null);
        var expectedResult = false;
        assertThat(DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication),
                   is(equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForPublicationMissingTitle() {
        var publication = createSamplePublication();
        publication.getEntityDescription().setMainTitle(null);
        var expectedResult = false;
        assertThat(DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication),
                   is(equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForPublicationMissingPublisherId() {
        var publication = createSamplePublication();
        publication.getPublisher().setId(null);
        var expectedResult = false;
        assertThat(DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication),
                   is(equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForDraftPublicaiton() {
        var publication = createSamplePublication();
        publication.setStatus(PublicationStatus.DRAFT);
        var expectedResult = false;
        assertThat(DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication),
                   is(equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForPublicationMissingPublicationIdentifier() {
        var publication = createSamplePublication();
        publication.setIdentifier(null);
        var expectedResult = false;
        assertThat(DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication),
                   is(equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForPublicationMissingModifiedDate() {
        var publication = createSamplePublication();
        publication.setModifiedDate(null);
        var expectedResult = false;
        assertThat(DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication),
                   is(equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForPublicationMissingInstanceType() {
        var publication = createSamplePublication();
        publication.getEntityDescription().getReference().setPublicationInstance(null);
        var expectedResult = false;
        assertThat(DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication),
                   is(equalTo(expectedResult)));
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class,  names = {"PUBLISHED_METADATA", "PUBLISHED"})
    void shouldReturnTrueIfMandatoryFieldsArePresent(PublicationStatus validPublicationStatus) {
        var publication = createSamplePublication();
        publication.setStatus(validPublicationStatus);
        publication.setPublisher( new Organization.Builder().withId(UriWrapper.fromUri("wwww.example.com").getUri()).build());
        publication.getEntityDescription().getDate().setYear("2014");
        publication.getEntityDescription().setMainTitle("some title");
        publication.setIdentifier(SortableIdentifier.next());
        publication.setModifiedDate(Instant.now());
        var expectedResult = true;
        assertThat(DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication),
                   is(equalTo(expectedResult)));

    }



    private Publication createSamplePublication() {
        return PublicationGenerator.publicationWithIdentifier();
    }

}

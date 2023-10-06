package no.unit.nva.publication.model.business.importcandidate;

import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

public class ImportCandidateTest {

    public static Stream<Arguments> importStatuses() {
        return Stream.of(Arguments.of( ImportStatusFactory.createImported(randomPerson(), randomUri())),
                Arguments.of( ImportStatusFactory.createNotApplicable(randomPerson(), randomString())));
    }

    private static Username randomPerson() {
        return new Username(randomString());
    }

    @Test
    void shouldCreatePublicationFromImportCandidate() {
        var randomImportCandidate = randomImportCandidate();
        var expectedPublication = createExpectedPublication(randomImportCandidate);
        var actualImportedPublication = randomImportCandidate.toPublication();
        assertThat(randomImportCandidate.getImportStatus().candidateStatus(), is(equalTo(CandidateStatus.NOT_IMPORTED)));
        assertThat(actualImportedPublication, is(equalTo(expectedPublication)));
    }

    @Test
    void builderShouldAcceptPublication() {
        var randomPublication = createPublicationWithoutStatus();
        var importCandidate =
            new ImportCandidate.Builder().withPublication(randomPublication.copy().build())
                    .withImportStatus( ImportStatusFactory.createNotImported())
                .build();

        var importCandidateCastedToPublication = Resource.fromPublication(importCandidate).toPublication();
        assertThat(importCandidate.getImportStatus().candidateStatus(), is(equalTo(CandidateStatus.NOT_IMPORTED)));
        assertThat(importCandidateCastedToPublication, is(equalTo(randomPublication)));
    }

    @Test
    void shouldDoRoundTripWithoutLosingData() throws JsonProcessingException {
        var randomImportCandidate = randomImportCandidate();
        var json = randomImportCandidate.toString();
        var regeneratedImportCandidate = JsonUtils.dtoObjectMapper.readValue(json, ImportCandidate.class);
        assertThat(regeneratedImportCandidate, is(equalTo(regeneratedImportCandidate)));
    }

    @ParameterizedTest
    @DisplayName("should be possible to swap imported status to other status")
    @MethodSource("importStatuses")
    void shouldBePossibleToTransitionLegalImportStatus(ImportStatus importStatus) {
        var randomImportCandidate = randomImportCandidate();
        randomImportCandidate.setImportStatus(importStatus);
        assertThat(randomImportCandidate.getImportStatus(), is(equalTo(importStatus)));
    }

    @Test
    void shouldCopyImportCandidate() {
        var randomImportCandidate = randomImportCandidate();
        var copy = randomImportCandidate.copyImportCandidate().withDoi(null).build();
        assertThat(randomImportCandidate, is(not(equalTo(copy))));
    }

    private static Funding randomFunding() {
        return new FundingBuilder().withId(randomUri()).build();
    }

    private static Publication createPublicationWithoutStatus() {
        var randomPublication = PublicationGenerator.randomPublication();
        randomPublication.setStatus(null);
        return randomPublication;
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                .withImportStatus( ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription())
                   .withLink(randomUri())
                   .withDoi(randomDoi())
                   .withIndexedDate(Instant.now())
                   .withPublishedDate(Instant.now())
                   .withHandle(randomUri())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withSubjects(List.of(randomUri()))
                   .withIdentifier(SortableIdentifier.next())
                   .withRightsHolder(randomString())
                   .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))
                   .withFundings(List.of(randomFunding()))
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    private EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder()
                   .withPublicationDate(new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }

    private Publication createExpectedPublication(ImportCandidate randomImportCandidate) {
        return new Publication.Builder()
                   .withAssociatedArtifacts(randomImportCandidate.getAssociatedArtifacts())
                   .withEntityDescription(randomImportCandidate.getEntityDescription())
                   .withAdditionalIdentifiers(randomImportCandidate.getAdditionalIdentifiers())
                   .withCreatedDate(randomImportCandidate.getCreatedDate())
                   .withDoi(randomImportCandidate.getDoi())
                   .withFundings(randomImportCandidate.getFundings())
                   .withSubjects(randomImportCandidate.getSubjects())
                   .withIdentifier(randomImportCandidate.getIdentifier())
                   .withLink(randomImportCandidate.getLink())
                   .withModifiedDate(randomImportCandidate.getModifiedDate())
                   .withProjects(randomImportCandidate.getProjects())
                   .withPublisher(randomImportCandidate.getPublisher())
                   .withResourceOwner(randomImportCandidate.getResourceOwner())
                   .withRightsHolder(randomImportCandidate.getRightsHolder())
                   .withHandle(randomImportCandidate.getHandle())
                   .withIndexedDate(randomImportCandidate.getIndexedDate())
                   .withPublishedDate(randomImportCandidate.getPublishedDate())
                   .withStatus(PublicationStatus.PUBLISHED)
                   .build();
    }
}

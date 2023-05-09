package no.unit.nva.publication.model.business;

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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ImportCandidateTest {

    @Test
    void shouldCreatePublicationFromImportCandidate() {
        var randomImportCandidate = randomImportCandidate();
        var expectedPublication = createExpectedPublication(randomImportCandidate);
        var actualImportedPublication = randomImportCandidate.toPublication();
        assertThat(randomImportCandidate.getImportStatus(), is(equalTo(ImportStatus.NOT_IMPORTED)));
        assertThat(actualImportedPublication, is(equalTo(expectedPublication)));

    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                .withStatus(PublicationStatus.PUBLISHED)
                .withImportStatus(ImportStatus.NOT_IMPORTED)
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

    private static Funding randomFunding() {
        return new FundingBuilder().withId(randomUri()).build();
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
                .withStatus(PublicationStatus.PUBLISHED)
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
                .build();
    }
}

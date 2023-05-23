package no.unit.nva.publication.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicationSummaryTest extends ResourcesLocalTest {

    private static final int MAX_SIZE_CONTRIBUTOR_LIST = 5;

    @BeforeEach
    public void setup() {
        super.init();
    }

    @Test
    @DisplayName("objectMapper Can Write And Read PublicationSummary")
    void objectMapperCanWriteAndReadPublicationSummary() throws JsonProcessingException {
        var publicationSummary = publicationSummary();
        var content = dtoObjectMapper.writeValueAsString(publicationSummary);
        var processedPublicationSummary = dtoObjectMapper.readValue(content, PublicationSummary.class);

        assertEquals(publicationSummary, processedPublicationSummary);
    }

    @Test
    void fromPublicationReturnsPublicationSummaryWithoutEmptyFields() {
        var publication = PublicationGenerator.publicationWithIdentifier();
        var summary = PublicationSummary.create(publication);
        assertThat(summary, doesNotHaveEmptyValues());
        assertThat(summary.extractPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(summary.getTitle(), is(equalTo(publication.getEntityDescription().getMainTitle())));
        assertThat(summary.getOwner(), is(equalTo(new User(publication.getResourceOwner().getOwner().getValue()))));
        assertThat(summary.getPublicationInstance(),
                   is(equalTo(publication.getEntityDescription().getReference().getPublicationInstance())));
        assertThat(summary.getPublishedDate(), is(equalTo(publication.getPublishedDate())));
        assertThat(summary.getContributors(),
                   containsInAnyOrder(publication.getEntityDescription().getContributors().toArray()));
    }

    @Test
    void shouldReturnsPublicationSummaryWithMaxSizeOfContributors() {
        var publication = PublicationGenerator.publicationWithIdentifier();
        var entityDescription = publication.getEntityDescription();
        entityDescription.setContributors(getNumberOfContributors(getRandomNumberOfContributorsLargerThanMaxSize()));
        PublicationSummary summary = PublicationSummary.create(publication);
        assertThat(summary.getContributors().size(), is(equalTo(MAX_SIZE_CONTRIBUTOR_LIST)));
    }

    @Test
    void shouldReturnsPublicationSummaryWithMaxSizeOfContributorsWithLowestSequenceNumbers() {
        var publication = PublicationGenerator.publicationWithIdentifier();
        var entityDescription = publication.getEntityDescription();
        entityDescription.setContributors(getNumberOfContributors(getRandomNumberOfContributorsLargerThanMaxSize()));
        PublicationSummary summary = PublicationSummary.create(publication);
        assertThat(summary.getContributors(), containsInAnyOrder(entityDescription.getContributors()
                                                                     .stream()
                                                                     .sorted(
                                                                         Comparator.comparing(Contributor::getSequence))
                                                                     .limit(MAX_SIZE_CONTRIBUTOR_LIST)
                                                                     .toArray()));
    }

    @Test
    void shouldAllowCreationOfMinimumPossibleInformation() {
        var publicationId = randomPublicationId();
        var publicationTitle = randomString();
        var summary = PublicationSummary.create(publicationId, publicationTitle);
        assertThat(summary.getPublicationId(), is(equalTo(publicationId)));
        assertThat(summary.getTitle(), is(equalTo(publicationTitle)));
    }

    private int getRandomNumberOfContributorsLargerThanMaxSize() {
        return MAX_SIZE_CONTRIBUTOR_LIST
               + new Random().nextInt(10 - MAX_SIZE_CONTRIBUTOR_LIST + 1)
               + MAX_SIZE_CONTRIBUTOR_LIST;
    }

    private List<Contributor> getNumberOfContributors(int number) {
        var contributors = new ArrayList<Contributor>(Collections.emptyList());
        for (int i = 0; i < number; i++) {
            contributors.add(getRandomContributor(i));
        }
        return contributors;
    }

    private Contributor getRandomContributor(int sequenceNumber) {
        return new Contributor.Builder()
                   .withIdentity(getRandomIdentity())
                   .withAffiliations(getListOfRandomOrganizations())
                   .withRole(new RoleType(Role.OTHER))
                   .withSequence(sequenceNumber)
                   .withCorrespondingAuthor(randomBoolean())
                   .build();
    }

    private List<Organization> getListOfRandomOrganizations() {
        return List.of(new Organization.Builder().withId(randomUri()).build());
    }

    private Identity getRandomIdentity() {
        return new Identity.Builder().withName(randomString()).withOrcId(randomString()).withId(randomUri()).build();
    }

    private URI randomPublicationId() {
        return UriWrapper.fromUri(randomUri()).addChild(SortableIdentifier.next().toString()).getUri();
    }

    private PublicationSummary publicationSummary() {
        return PublicationSummary.create(randomPublication());
    }
}

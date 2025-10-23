package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithAffiliation;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithId;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.events.handlers.batch.ManualUpdateType.CONTRIBUTOR_AFFILIATION;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManuallyUpdatePublicationUtilTest extends ResourcesLocalTest {

    private static final URI NEW_AFFILIATION_ID = randomUri();
    private static final URI OLD_AFFILIATION_ID = randomUri();

    private ManuallyUpdatePublicationUtil publicationUtil;
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        super.init();
        resourceService = getResourceService(client);
        publicationUtil = ManuallyUpdatePublicationUtil.create(resourceService, new Environment());
    }

    @Test
    void updateWithContributorAffiliationShouldUpdateAllMatchingAffiliations() {
        var resources = createResourcesWithContributor(randomContributorWithAffiliation(OLD_AFFILIATION_ID));
        var updateRequest = createAffiliationUpdateRequest();

        publicationUtil.update(resources, updateRequest);

        resources.forEach(this::assertAffiliationWasUpdated);
    }

    @Test
    void updateWithNonMatchingAffiliationShouldNotModifyContributor() {
        var contributorWithRandomAffiliation = randomContributorWithAffiliation(randomUri());
        var resources = createResourcesWithContributor(contributorWithRandomAffiliation);
        var updateRequest = createAffiliationUpdateRequest();

        publicationUtil.update(resources, updateRequest);

        resources.forEach(this::assertResourceIsUnchanged);
    }

    @Test
    void updateWithMultipleAffiliationsShouldUpdateOnlyAffiliationProvidedInRequest() {
        var contributor = randomContributorWithAffiliation(OLD_AFFILIATION_ID);
        var originalAffiliations = copyAffiliations(contributor);
        var resources = createResourcesWithContributor(contributor);
        var updateRequest = createAffiliationUpdateRequest();

        publicationUtil.update(resources, updateRequest);

        resources.forEach(resource -> {
            var updatedContributor = findContributor(resource, contributor);
            assertContainsUpdatedAffiliation(updatedContributor);
            assertOtherAffiliationsUnchanged(updatedContributor, originalAffiliations);
        });
    }

    private void assertResourceIsUnchanged(Resource resource) {
        assertEquals(resource.toPublication(), resource.fetch(resourceService).orElseThrow().toPublication());
    }

    private List<Resource> createResourcesWithContributor(Contributor contributor) {
        return IntStream.range(0, 3)
                   .mapToObj(i -> createPublicationWithContributor(contributor))
                   .map(Resource::fromPublication)
                   .toList();
    }

    private Publication createPublicationWithContributor(Contributor contributor) {
        var publication = randomPublication();
        publication.getEntityDescription()
            .setContributors(
                List.of(contributor, randomContributorWithId(randomUri()), new Contributor.Builder().build()));
        return savePublication(publication);
    }

    private ManuallyUpdatePublicationsRequest createAffiliationUpdateRequest() {
        return new ManuallyUpdatePublicationsRequest(CONTRIBUTOR_AFFILIATION,
                                                     OLD_AFFILIATION_ID.toString(),
                                                     NEW_AFFILIATION_ID.toString(), Map.of(),
                                                     null);
    }

    private Publication savePublication(Publication publication) {
        return attempt(() -> resourceService.createPublication(UserInstance.fromPublication(publication),
                                                               publication)).orElseThrow();
    }

    private void assertAffiliationWasUpdated(Resource resource) {
        var updatedResource = resource.fetch(resourceService).orElseThrow();
        var updatedContributor = findContributorWithAffiliation(updatedResource);
        var updatedAffiliation = getAffiliationsWithId(updatedContributor).getFirst();
        var expectedAffiliation = Organization.fromUri(NEW_AFFILIATION_ID);
        assertEquals(expectedAffiliation, updatedAffiliation);
    }

    private void assertContainsUpdatedAffiliation(Contributor contributor) {
        var expectedAffiliation = Organization.fromUri(NEW_AFFILIATION_ID);
        assertThat(contributor.getAffiliations(), hasItem(expectedAffiliation));
    }

    private void assertOtherAffiliationsUnchanged(Contributor updatedContributor,
                                                  List<Corporation> originalAffiliations) {
        var unchangedOriginalAffiliations = originalAffiliations.stream()
                                                .filter(affiliation -> !hasAffiliationId(affiliation, OLD_AFFILIATION_ID))
                                                .toList();

        unchangedOriginalAffiliations.forEach(
            originalAffiliation -> assertThat(updatedContributor.getAffiliations(), hasItem(originalAffiliation)));
    }

    private Contributor findContributor(Resource resource, Contributor contributor) {
        var contributorId = extractContributorId(contributor);
        return resource.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(contr -> hasContributorId(contr, contributorId))
                   .findFirst()
                   .orElseThrow();
    }

    private Contributor findContributorWithAffiliation(Resource resource) {
        return resource.getEntityDescription().getContributors().stream()
                   .filter(contributor -> !getAffiliationsWithId(contributor).isEmpty())
                   .findFirst()
                   .orElseThrow();
    }

    private List<Corporation> getAffiliationsWithId(Contributor contributor) {
        return contributor.getAffiliations()
                   .stream()
                   .filter(affiliation -> hasAffiliationId(affiliation, NEW_AFFILIATION_ID))
                   .toList();
    }

    private String extractContributorId(Contributor contributor) {
        return UriWrapper.fromUri(contributor.getIdentity().getId()).getLastPathElement();
    }

    private boolean hasContributorId(Contributor contributor, String contributorId) {
        return Optional.ofNullable(contributor)
                   .map(Contributor::getIdentity)
                   .map(Identity::getId)
                   .map(UriWrapper::fromUri)
                   .map(UriWrapper::getLastPathElement)
                   .map(id -> id.equals(contributorId))
                   .orElse(false);
    }

    private boolean hasAffiliationId(Corporation corporation, URI organizationId) {
        return corporation instanceof Organization organization && organization.getId().equals(organizationId);
    }

    private List<Corporation> copyAffiliations(Contributor contributor) {
        return new ArrayList<>(contributor.getAffiliations());
    }
}
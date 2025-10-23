package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithAffiliation;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithId;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.events.handlers.batch.ManualUpdateType.CONTRIBUTOR_AFFILIATION;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
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

    private static final String CRISTIN = "cristin";
    private static final String ORGANIZATION = "organization";
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private static final String NEW_AFFILIATION_ID = randomString();
    private static final String OLD_AFFILIATION_ID = randomString();

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
        var resources = createResourcesWithContributor(createContributorWithAffiliation(OLD_AFFILIATION_ID));
        var updateRequest = createAffiliationUpdateRequest();

        publicationUtil.update(resources, updateRequest);

        resources.forEach(this::assertAffiliationWasUpdated);
    }

    @Test
    void updateWithNonMatchingAffiliationShouldNotModifyContributor() {
        var contributorWithRandomAffiliation = createContributorWithAffiliation(randomString());
        var resources = createResourcesWithContributor(contributorWithRandomAffiliation);
        var updateRequest = createAffiliationUpdateRequest();

        publicationUtil.update(resources, updateRequest);

        resources.forEach(this::assertResourceIsUnchanged);
    }

    @Test
    void updateWithMultipleAffiliationsShouldUpdateOnlyAffiliationProvidedInRequest() {
        var contributor = randomContributorWithAffiliation(buildAffiliationUri(OLD_AFFILIATION_ID));
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

    private Contributor createContributorWithAffiliation(String affiliationId) {
        return randomContributorWithAffiliation(buildAffiliationUri(affiliationId));
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
                                                     OLD_AFFILIATION_ID,
                                                     NEW_AFFILIATION_ID, Map.of(),
                                                     null);
    }

    private URI buildAffiliationUri(String affiliationId) {
        return UriWrapper.fromHost(API_HOST).addChild(CRISTIN).addChild(ORGANIZATION).addChild(affiliationId).getUri();
    }

    private Publication savePublication(Publication publication) {
        return attempt(() -> resourceService.createPublication(UserInstance.fromPublication(publication),
                                                               publication)).orElseThrow();
    }

    private void assertAffiliationWasUpdated(Resource resource) {
        var updatedResource = resource.fetch(resourceService).orElseThrow();
        var updatedContributor = findContributorWithAffiliation(updatedResource);
        var updatedAffiliation = getAffiliationsWithIdentifier(updatedContributor).getFirst();
        var expectedAffiliation = Organization.fromUri(buildAffiliationUri(NEW_AFFILIATION_ID));
        assertEquals(expectedAffiliation, updatedAffiliation);
    }

    private void assertContainsUpdatedAffiliation(Contributor contributor) {
        var expectedAffiliation = Organization.fromUri(buildAffiliationUri(NEW_AFFILIATION_ID));
        assertThat(contributor.getAffiliations(), hasItem(expectedAffiliation));
    }

    private void assertOtherAffiliationsUnchanged(Contributor updatedContributor,
                                                  List<Corporation> originalAffiliations) {
        var unchangedOriginalAffiliations = originalAffiliations.stream()
                                                .filter(
                                                    affiliation -> !hasAffiliationId(affiliation, OLD_AFFILIATION_ID))
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
                   .filter(contributor -> !getAffiliationsWithIdentifier(contributor).isEmpty())
                   .findFirst()
                   .orElseThrow();
    }

    private List<Corporation> getAffiliationsWithIdentifier(Contributor contributor) {
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

    private boolean hasAffiliationId(Corporation corporation, String affiliationId) {
        if (!(corporation instanceof Organization organization)) {
            return false;
        }
        return UriWrapper.fromUri(organization.getId()).getLastPathElement().equals(affiliationId);
    }

    private List<Corporation> copyAffiliations(Contributor contributor) {
        return new ArrayList<>(contributor.getAffiliations());
    }
}
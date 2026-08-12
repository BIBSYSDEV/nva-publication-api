package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.model.testing.PublicationGenerator.randomApprovals;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithAffiliation;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithId;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.events.handlers.batch.ManualUpdateType.CONTRIBUTOR_AFFILIATION;
import static no.unit.nva.publication.events.handlers.batch.ManualUpdateType.PROJECT;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import no.unit.nva.model.Approval;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResearchProject;
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
  private static final String OLD_PROJECT_IDENTIFIER = randomInteger().toString();
  private static final String NEW_PROJECT_IDENTIFIER = randomInteger().toString();
  private static final String OTHER_PROJECT_IDENTIFIER = randomInteger().toString();
  private static final String OLD_PROJECT_NAME = randomString();
  private static final String NEW_PROJECT_NAME = randomString();
  private static final String OTHER_PROJECT_NAME = randomString();
  private static final List<Approval> OLD_PROJECT_APPROVALS = randomApprovals();
  private static final List<Approval> NEW_PROJECT_APPROVALS = randomApprovals();
  private static final List<Approval> OTHER_PROJECT_APPROVALS = randomApprovals();
  private static final String API_HOST = new Environment().readEnv("API_HOST");
  private static final String CRISTIN_PATH = "cristin";
  private static final String PROJECT_PATH = "project";
  private static final String NON_CRISTIN_PROJECT_PATH = "external-project";

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
    var resources =
        createResourcesWithContributor(randomContributorWithAffiliation(OLD_AFFILIATION_ID));
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

    resources.forEach(
        resource -> {
          var updatedContributor = findContributor(resource, contributor);
          assertContainsUpdatedAffiliation(updatedContributor);
          assertOtherAffiliationsUnchanged(updatedContributor, originalAffiliations);
        });
  }

  @Test
  void updateWithProjectShouldReplaceMatchingProjectAndKeepItsMetadata() {
    var resources = createResourcesWithProjects(List.of(this::oldProject));

    publicationUtil.update(resources, createProjectUpdateRequest());

    resources.forEach(
        resource -> assertThat(fetchProjects(resource), contains(oldProjectWithNewIdentifier())));
  }

  @Test
  void updateWithNonMatchingProjectShouldNotModifyResource() {
    var resources = createResourcesWithProjects(List.of(this::otherProject));

    publicationUtil.update(resources, createProjectUpdateRequest());

    resources.forEach(resource -> assertThat(fetchProjects(resource), contains(otherProject())));
  }

  @Test
  void updateWithMultipleProjectsShouldUpdateOnlyProjectProvidedInRequest() {
    var resources = createResourcesWithProjects(List.of(this::oldProject, this::otherProject));

    publicationUtil.update(resources, createProjectUpdateRequest());

    resources.forEach(
        resource ->
            assertThat(
                fetchProjects(resource), contains(oldProjectWithNewIdentifier(), otherProject())));
  }

  @Test
  void updateWithProjectShouldKeepExistingProjectUntouchedWhenResourceAlreadyHasNewProject() {
    var resources = createResourcesWithProjects(List.of(this::oldProject, this::newProject));

    publicationUtil.update(resources, createProjectUpdateRequest());

    resources.forEach(resource -> assertThat(fetchProjects(resource), contains(newProject())));
  }

  @Test
  void updateWithIdenticalOldAndNewProjectShouldNotModifyResource() {
    var resources = createResourcesWithProjects(List.of(this::oldProject));

    publicationUtil.update(
        resources, createProjectUpdateRequest(OLD_PROJECT_IDENTIFIER, OLD_PROJECT_IDENTIFIER));

    resources.forEach(resource -> assertThat(fetchProjects(resource), contains(oldProject())));
  }

  @Test
  void updateWithOldProjectListedTwiceShouldResultInSingleNewProject() {
    var resources = createResourcesWithProjects(List.of(this::oldProject, this::oldProject));

    publicationUtil.update(resources, createProjectUpdateRequest());

    resources.forEach(
        resource -> assertThat(fetchProjects(resource), contains(oldProjectWithNewIdentifier())));
  }

  @Test
  void updateWithProjectShouldNotModifyProjectOutsideCristinProjectPath() {
    var resources = createResourcesWithProjects(List.of(this::projectOutsideCristinProjectPath));

    publicationUtil.update(resources, createProjectUpdateRequest());

    resources.forEach(
        resource ->
            assertThat(fetchProjects(resource), contains(projectOutsideCristinProjectPath())));
  }

  private List<Resource> createResourcesWithProjects(
      Collection<Supplier<ResearchProject>> projectSuppliers) {
    return IntStream.range(0, 3)
        .mapToObj(_ -> createPublicationWithProjects(projectSuppliers))
        .map(Resource::fromPublication)
        .toList();
  }

  private Publication createPublicationWithProjects(
      Collection<Supplier<ResearchProject>> projectSuppliers) {
    var publication = randomPublication();
    publication.setProjects(projectSuppliers.stream().map(Supplier::get).toList());
    return savePublication(publication);
  }

  private ManuallyUpdatePublicationsRequest createProjectUpdateRequest() {
    return createProjectUpdateRequest(OLD_PROJECT_IDENTIFIER, NEW_PROJECT_IDENTIFIER);
  }

  private ManuallyUpdatePublicationsRequest createProjectUpdateRequest(
      String oldIdentifier, String newIdentifier) {
    return new ManuallyUpdatePublicationsRequest(
        PROJECT, oldIdentifier, newIdentifier, Map.of(), null);
  }

  private ResearchProject projectOutsideCristinProjectPath() {
    var projectId =
        UriWrapper.fromHost(API_HOST)
            .addChild(NON_CRISTIN_PROJECT_PATH)
            .addChild(OLD_PROJECT_IDENTIFIER)
            .getUri();
    return new ResearchProject.Builder()
        .withId(projectId)
        .withName(OLD_PROJECT_NAME)
        .withApprovals(OLD_PROJECT_APPROVALS)
        .build();
  }

  private ResearchProject oldProject() {
    return project(OLD_PROJECT_IDENTIFIER, OLD_PROJECT_NAME, OLD_PROJECT_APPROVALS);
  }

  private ResearchProject newProject() {
    return project(NEW_PROJECT_IDENTIFIER, NEW_PROJECT_NAME, NEW_PROJECT_APPROVALS);
  }

  private ResearchProject otherProject() {
    return project(OTHER_PROJECT_IDENTIFIER, OTHER_PROJECT_NAME, OTHER_PROJECT_APPROVALS);
  }

  private ResearchProject oldProjectWithNewIdentifier() {
    return project(NEW_PROJECT_IDENTIFIER, OLD_PROJECT_NAME, OLD_PROJECT_APPROVALS);
  }

  private ResearchProject project(String identifier, String name, List<Approval> approvals) {
    return new ResearchProject.Builder()
        .withId(projectUri(identifier))
        .withName(name)
        .withApprovals(approvals)
        .build();
  }

  private URI projectUri(String identifier) {
    return UriWrapper.fromHost(API_HOST)
        .addChild(CRISTIN_PATH)
        .addChild(PROJECT_PATH)
        .addChild(identifier)
        .getUri();
  }

  private List<ResearchProject> fetchProjects(Resource resource) {
    return resource.fetch(resourceService).orElseThrow().getProjects();
  }

  private void assertResourceIsUnchanged(Resource resource) {
    assertEquals(
        resource.toPublication(), resource.fetch(resourceService).orElseThrow().toPublication());
  }

  private List<Resource> createResourcesWithContributor(Contributor contributor) {
    return IntStream.range(0, 3)
        .mapToObj(i -> createPublicationWithContributor(contributor))
        .map(Resource::fromPublication)
        .toList();
  }

  private Publication createPublicationWithContributor(Contributor contributor) {
    var publication = randomPublication();
    publication
        .getEntityDescription()
        .setContributors(
            List.of(
                contributor,
                randomContributorWithId(randomUri()),
                new Contributor.Builder().build()));
    return savePublication(publication);
  }

  private ManuallyUpdatePublicationsRequest createAffiliationUpdateRequest() {
    return new ManuallyUpdatePublicationsRequest(
        CONTRIBUTOR_AFFILIATION,
        OLD_AFFILIATION_ID.toString(),
        NEW_AFFILIATION_ID.toString(),
        Map.of(),
        null);
  }

  private Publication savePublication(Publication publication) {
    return attempt(
            () ->
                resourceService.createPublication(
                    UserInstance.fromPublication(publication), publication))
        .orElseThrow();
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
    assertThat(contributor.affiliations(), hasItem(expectedAffiliation));
  }

  private void assertOtherAffiliationsUnchanged(
      Contributor updatedContributor, List<Corporation> originalAffiliations) {
    var unchangedOriginalAffiliations =
        originalAffiliations.stream()
            .filter(affiliation -> !hasAffiliationId(affiliation, OLD_AFFILIATION_ID))
            .toList();

    unchangedOriginalAffiliations.forEach(
        originalAffiliation ->
            assertThat(updatedContributor.affiliations(), hasItem(originalAffiliation)));
  }

  private Contributor findContributor(Resource resource, Contributor contributor) {
    var contributorId = extractContributorId(contributor);
    return resource.getEntityDescription().getContributors().stream()
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
    return contributor.affiliations().stream()
        .filter(affiliation -> hasAffiliationId(affiliation, NEW_AFFILIATION_ID))
        .toList();
  }

  private String extractContributorId(Contributor contributor) {
    return UriWrapper.fromUri(contributor.identity().getId()).getLastPathElement();
  }

  private boolean hasContributorId(Contributor contributor, String contributorId) {
    return Optional.ofNullable(contributor)
        .map(Contributor::identity)
        .map(Identity::getId)
        .map(UriWrapper::fromUri)
        .map(UriWrapper::getLastPathElement)
        .map(id -> id.equals(contributorId))
        .orElse(false);
  }

  private boolean hasAffiliationId(Corporation corporation, URI organizationId) {
    return corporation instanceof Organization organization
        && organization.getId().equals(organizationId);
  }

  private List<Corporation> copyAffiliations(Contributor contributor) {
    return new ArrayList<>(contributor.affiliations());
  }
}

package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeBase;
import no.unit.nva.model.instancetypes.degree.DegreeLicentiate;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NonDegreePermissionStrategyTest extends PublicationPermissionStrategyTest {

    private static Stream<Arguments> argumentsForDenyingCuratorFromPerformingOperationsOnProtectedDegreeResources() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.DELETE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.TERMINATE);

        final var instanceClasses = Set.of(DegreeLicentiate.class,
                                           DegreeBachelor.class,
                                           DegreeMaster.class,
                                           DegreePhd.class);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations, instanceClasses);
    }

    private static Stream<Arguments> argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.UNPUBLISH);

        final var instanceClasses = Set.of(DegreeLicentiate.class,
                                           DegreeBachelor.class,
                                           DegreeMaster.class,
                                           DegreePhd.class);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations, instanceClasses);
    }

    private static Stream<Arguments> generateAllCombinationsOfOperationsAndInstanceClasses(
        final Set<PublicationOperation> operations,
        final Set<Class<? extends DegreeBase>> instanceClasses) {
        return operations.stream()
                   .flatMap(operation -> instanceClasses.stream()
                                             .map(instanceClass -> Arguments.of(operation, instanceClass)))
                   .toList()
                   .stream();
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on instance type {1} belonging to the institution")
    @MethodSource("argumentsForDenyingCuratorFromPerformingOperationsOnProtectedDegreeResources")
    void shouldDenyCuratorOnDegree(PublicationOperation operation, Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();
        var topLevelCristinOrgId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForCurator(), cristinId,
                                                topLevelCristinOrgId);
        var publication = createPublication(degreeInstanceClass, resourceOwner, institution, topLevelCristinOrgId);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(
        name = "Should allow Thesis curator {0} operation on instance type {1} belonging to the institution"
    )
    @MethodSource("argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources")
    void shouldAllowThesisCuratorOnDegree(PublicationOperation operation, Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createPublication(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();
        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(
        name = "Should allow Thesis curator {0} operation on instance type {1} belonging to the institution"
    )
    @MethodSource("argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources")
    void shouldAllowThesisCuratorFromCuratingInstitutionOnDegree(PublicationOperation operation,
                                                                 Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinOrganizationId = randomUri();

        var publication = createPublication(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();
        var creator = createContributor(Role.CREATOR, randomUri(), cristinOrganizationId);
        publication.getEntityDescription().setContributors(List.of(creator));

        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution,
                                                                           Set.of(creator.getIdentity().getId()))));
        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinOrganizationId, curatingInstitution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @Test
    void shouldDenyNonEmbargoThesisCuratorOnDegreeWithEmbargo()
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution, randomUri()).copy()
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(
                                  List.of(randomFileWithEmbargo(), AssociatedArtifactsGenerator.randomOpenFile()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(PublicationOperation.UPDATE));
    }

    @Test
    void shouldAllowEmbargoThesisCuratorOnDegreeWithEmbargoWhenCuratorForRegistrator()
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution, randomUri()).copy()
                              .withStatus(PUBLISHED)
                              .withAssociatedArtifacts(
                                  List.of(randomFileWithEmbargo(), AssociatedArtifactsGenerator.randomOpenFile()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForEmbargoThesisCurator(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(PublicationOperation.UPDATE));
    }

    @ParameterizedTest(
        name = "Should not allow Thesis curator {0} operation on instance type {1} belonging to the institution"
    )
    @MethodSource("argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources")
    void shouldNotAllowThesisCuratorFromCuratingInstitutionOnDegreeWhenCuratorIsCuratingSupervisorOnly(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var publication = createPublication(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var cristinOrganizationId = randomUri();
        var cristinPersonId = randomUri();
        var contributor = createContributor(Role.SUPERVISOR, cristinPersonId, cristinOrganizationId);
        publication.getEntityDescription().setContributors(List.of(contributor));

        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(curatingInstitution, Set.of(cristinPersonId))));

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinOrganizationId, curatingInstitution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(
        name = "Should handle {0} operation on instance type {1} when contributor is missing ID"
    )
    @MethodSource("argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources")
    void shouldHandleDegreeContributorsWithoutId(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var publication = createPublication(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var cristinOrganizationId = randomUri();
        var cristinPersonId = randomUri();
        var contributor = createContributorWithoutId(Role.CREATOR, cristinOrganizationId);
        publication.getEntityDescription().setContributors(List.of(contributor));

        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(curatingInstitution, Set.of(cristinPersonId))));

        var requestInfo = createUserRequestInfo(curatorUsername, institution, Collections.emptyList(),
                                                cristinOrganizationId, curatingInstitution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(
        name = "Should allow Thesis curator {0} operation on instance type {1} belonging to the institution"
    )
    @MethodSource("argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources")
    void shouldAllowThesisCuratorFromCuratingInstitutionOnDegreeWhenCuratorIsCuratingNotOnlySupervisor(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var publication = createPublication(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var cristinOrganizationId = randomUri();
        var supervisor = createContributor(Role.SUPERVISOR, randomUri(), cristinOrganizationId);
        var creator = createContributor(Role.CREATOR, randomUri(), cristinOrganizationId);
        publication.getEntityDescription().setContributors(List.of(supervisor, creator));

        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(
            curatingInstitution, Set.of(supervisor.getIdentity().getId(), creator.getIdentity().getId()))));

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinOrganizationId, curatingInstitution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(
        name = "Should not allow Thesis curator {0} operation on instance type {1} belonging to the institution"
    )
    @MethodSource("argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources")
    void shouldNotAllowThesisCuratorFromCuratingInstitutionOnDegreeWhenCuratingInstitutionIsMissingContributors(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var publication = createPublication(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var cristinOrganizationId = randomUri();
        var cristinPersonId = randomUri();
        var contributor = createContributor(Role.SUPERVISOR, cristinPersonId, cristinOrganizationId);
        publication.getEntityDescription().setContributors(List.of(contributor));

        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of(contributor.getIdentity().getId()))));

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinOrganizationId, curatingInstitution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(
        name = "Should allow Thesis curator {0} operation on instance type {1} belonging to the institution when "
               + "missing contributor IDs"
    )
    @MethodSource("argumentsForAllowingThesisCuratorPerformingOperationsOnProtectedDegreeResources")
    void shouldAllowThesisCuratorFromCuratingInstitutionOnDegreeWhenCuratorIsCuratingContributorsWithoutId(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var publication = createPublication(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var cristinOrganizationId = randomUri();

        publication.getEntityDescription().setContributors(List.of());

        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of())));

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinOrganizationId, curatingInstitution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    private static Contributor createContributor(Role role, URI cristinPersonId, URI cristinOrganizationId) {
        return new Contributor.Builder()
                   .withAffiliations(List.of(Organization.fromUri(cristinOrganizationId)))
                   .withIdentity(new Identity.Builder().withId(cristinPersonId).build())
                   .withRole(new RoleType(role))
                   .build();
    }

    private static Contributor createContributorWithoutId(Role role, URI cristinOrganizationId) {
        return new Contributor.Builder()
                   .withAffiliations(List.of(Organization.fromUri(cristinOrganizationId)))
                   .withIdentity(new Identity.Builder().build())
                   .withRole(new RoleType(role))
                   .build();
    }

    public static OpenFile randomFileWithEmbargo() {
        return new OpenFile(UUID.randomUUID(), RandomDataGenerator.randomString(),
                                 RandomDataGenerator.randomString(), RandomDataGenerator.randomInteger().longValue(),
                                 RandomDataGenerator.randomUri(), false, PublisherVersion.PUBLISHED_VERSION,
                                 Instant.now().plusSeconds(60 * 60 * 24),
                                 RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                                 RandomDataGenerator.randomString(), RandomDataGenerator.randomInstant(),
                                 new UserUploadDetails(new Username(RandomDataGenerator.randomString()),
                                                       RandomDataGenerator.randomInstant()));
    }
}

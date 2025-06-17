package no.unit.nva.publication.permissions.publication;

import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.PermissionsTestUtils.InstitutionSuite;
import no.unit.nva.publication.permissions.PermissionsTestUtils.User;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DegreeDenyStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should allow Registrator {0} operation on instance type {1} when degree has no finalized "
                              + "files")
    @MethodSource("argumentsForRegistrator")
    void shouldAllowRegistratorOperationsOnDegreeWithoutFinalizedFiles(PublicationOperation operation,
                                                                      Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var registrator = User.random();
        var publication = createPublicationWithoutFinalizedFiles(degreeInstanceClass,
                                                                 registrator.name(),
                                                                 registrator.customer(),
                                                                 registrator.topLevelCristinId());

        var publicationWithStatus = publication.copy()
                                        .withStatus(operation == PublicationOperation.DELETE ? DRAFT : PUBLISHED)
                                        .build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publicationWithStatus), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Registrator {0} operation on instance type {1} when degree has finalized "
                              + "files")
    @MethodSource("argumentsForRegistratorExcludingUploadFileAndPartialUpdate")
    void shouldDenyRegistratorOperationsOnDegreeWithFinalizedFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var registrator = User.random();
        var publication = createPublicationWithFinalizedFiles(degreeInstanceClass,
                                                              registrator.name(),
                                                              registrator.customer(),
                                                              registrator.topLevelCristinId());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Registrator {0} operation on instance type {1} when degree has finalized "
                              + "files")
    @MethodSource("argumentsEveryoneShouldBeAllowedAfterFinalizedFiles")
    void shouldAllowRegistratorOperationsOnDegreeFinalizedFiles(PublicationOperation operation,
                                                                      Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var registrator = User.random();
        var publication = createPublicationWithoutFinalizedFiles(degreeInstanceClass,
                                                                 registrator.name(),
                                                                 registrator.customer(),
                                                                 registrator.topLevelCristinId());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should not throw NPE when contributor is missing ID")
    @MethodSource("degreesProvider")
    void shouldNotThrowNPEWhenContributorIsMissingId(Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublication(degreeInstanceTypeClass,
                                            owningInstitution.registrator().name(),
                                            owningInstitution.registrator().customer(),
                                            owningInstitution.registrator().cristinId());

        var contributor = createContributorWithoutId(Role.CREATOR,
                                                     curatingInstitution.contributor().topLevelCristinId());
        publication.getEntityDescription().setContributors(List.of(contributor));

        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(curatingInstitution.contributor().topLevelCristinId(),
                                           Set.of(curatingInstitution.contributor().cristinId()))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should allow Thesis curator from curating institution {0} operation on instance type "
                              + "{1} when curating institution is missing contributor IDs")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromCuratingInstitutionOnDegreeWithoutApprovedFilesWhenCuratorIsCuratingContributorsWithoutId(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithoutFinalizedFiles(degreeInstanceTypeClass,
                                                                 owningInstitution.registrator().name(),
                                                                 owningInstitution.registrator().customer(),
                                                                 owningInstitution.registrator().cristinId());

        publication.getEntityDescription().setContributors(List.of());
        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(curatingInstitution.thesisCurator().topLevelCristinId(),
                                           Set.of())));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    private static Stream<Arguments> argumentsForRegistrator() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.DELETE,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForRegistratorExcludingUploadFileAndPartialUpdate() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.DELETE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsEveryoneShouldBeAllowedAfterFinalizedFiles() {
        final var operations = Set.of(PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForThesisCurator() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> degreesProvider() {
        return Arrays.stream(PROTECTED_DEGREE_INSTANCE_TYPES).toList().stream()
            .map(Arguments::of);
    }

    private static Stream<Arguments> generateAllCombinationsOfOperationsAndInstanceClasses(
        final Set<PublicationOperation> operations) {
        return operations.stream()
                   .flatMap(operation -> Arrays.stream(PROTECTED_DEGREE_INSTANCE_TYPES).toList().stream()
                                             .map(instanceClass -> Arguments.of(operation, instanceClass)));
    }

    private static Contributor createContributorWithoutId(Role role, URI cristinOrganizationId) {
        return new Contributor.Builder()
                   .withAffiliations(List.of(Organization.fromUri(cristinOrganizationId)))
                   .withIdentity(new Identity.Builder().build())
                   .withRole(new RoleType(role))
                   .build();
    }

    private RequestInfo toRequestInfo(User user) throws JsonProcessingException {
        return createUserRequestInfo(user.name(), user.customer(), user.accessRights(), user.cristinId(),
                                     user.topLevelCristinId());
    }
}

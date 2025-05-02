package no.unit.nva.publication.permissions.publication;

import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
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
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DegreeDenyStrategyTest extends PublicationPermissionStrategyTest {

    // AnonBeforeOpenFile ?
    // AnonAfterOpenFile ?

    @ParameterizedTest(name = "Should allow Registrator {0} operation on instance type {1} when degree has no open "
                              + "files")
    @MethodSource("argumentsForRegistrator")
    void shouldAllowRegistratorOperationsOnDegreeWithoutOpenFiles(PublicationOperation operation,
                                                             Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var registrator = User.random();
        var publication = operation == PublicationOperation.UNPUBLISH
                              ? createPublicationWithoutOpenOrInternalFiles(degreeInstanceClass,
                                                                          registrator.name,
                                                                          registrator.customer,
                                                                          registrator.topLevelCristinId)
                              : createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                                  registrator.name,
                                                                  registrator.customer,
                                                                  registrator.topLevelCristinId);

        var publicationWithStatus = publication.copy()
                                        .withStatus(operation == PublicationOperation.DELETE ? DRAFT : PUBLISHED)
                                        .build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publicationWithStatus, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Registrator {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsForRegistrator")
    void shouldDenyRegistratorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                            Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var registrator = User.random();
        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator.name,
                                                        registrator.customer,
                                                        registrator.topLevelCristinId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Contributor {0} operation on instance type {1} when degree has no open "
                              + "files")
    @MethodSource("argumentsForContributor")
    void shouldAllowContributorOperationsOnDegreeWithoutOpenFiles(PublicationOperation operation,
                                                                    Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var publication = operation == PublicationOperation.UNPUBLISH
                              ? createPublicationWithoutOpenOrInternalFiles(degreeInstanceClass,
                                                                            registrator.name,
                                                                            registrator.customer,
                                                                            registrator.topLevelCristinId)
                              : createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                                  registrator.name,
                                                                  registrator.customer,
                                                                  registrator.topLevelCristinId);

        var contributor = createContributor(Role.CREATOR, institution.contributor.cristinId,
                                            institution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(institution.contributor.topLevelCristinId,
                                                                           Set.of(institution.contributor.cristinId))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(institution.contributor),
                                                                      identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Contributor {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsForContributor")
    void shouldDenyContributorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator.name,
                                                        registrator.customer,
                                                        registrator.topLevelCristinId);

        var contributor = createContributor(Role.CREATOR, institution.contributor.cristinId,
                                            institution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(institution.contributor.topLevelCristinId,
                                                                           Set.of(institution.contributor.cristinId))));


        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(institution.contributor),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on instance type {1} when degree has no open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorOperationsOnDegreeWithoutOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var curator = institution.curator;

        var publication = createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                            registrator.name,
                                                            registrator.customer,
                                                            registrator.topLevelCristinId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsForCurator")
    void shouldDenyCuratorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var curator = institution.curator;

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator.name,
                                                        registrator.customer,
                                                        registrator.topLevelCristinId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator from curating institution {0} operation on instance type {1} "
                              + "when degree has no open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorOnCuratingInstitutionOperationsOnDegreeWithoutOpenFiles(PublicationOperation operation,
                                                                                   Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication = createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                            owningInstitution.registrator.name,
                                                            owningInstitution.registrator.customer,
                                                            owningInstitution.registrator.topLevelCristinId);

        var contributor = createContributor(Role.CREATOR, curatingInstitution.contributor.cristinId,
                                            curatingInstitution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution.contributor.topLevelCristinId,
                                                                           Set.of(curatingInstitution.contributor.cristinId))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator from curating institution {0} operation on instance type {1} when"
                              + " degree has open files")
    @MethodSource("argumentsForCurator")
    void shouldDenyCuratorOnCuratingInstitutionOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                          Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        owningInstitution.registrator.name,
                                                        owningInstitution.registrator.customer,
                                                        owningInstitution.registrator.topLevelCristinId);

        var contributor = createContributor(Role.CREATOR, curatingInstitution.contributor.cristinId,
                                            curatingInstitution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution.contributor.topLevelCristinId,
                                                                           Set.of(curatingInstitution.contributor.cristinId))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator from Registrators institution {0} operation on instance "
                              + "type {1} when degree has no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromRegistratorsInstitutionOperationsOnDegreeWithoutOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var thesisCurator = institution.thesisCurator;

        var publication = createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                            registrator.name,
                                                            registrator.customer,
                                                            registrator.topLevelCristinId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(thesisCurator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator from Registrators institution {0} operation on instance "
                              + "type {1} when degree has open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromRegistratorsInstitutionOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                          Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var thesisCurator = institution.thesisCurator;

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator.name,
                                                        registrator.customer,
                                                        registrator.topLevelCristinId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(thesisCurator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator from curating institution {0} operation on instance "
                              + "type {1} when degree has no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromCuratingInstitutionOperationsOnDegreeWithoutOpenFiles(PublicationOperation operation,
                                                                                               Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication = createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                            owningInstitution.registrator.name,
                                                            owningInstitution.registrator.customer,
                                                            owningInstitution.registrator.topLevelCristinId);

        var contributor = createContributor(Role.CREATOR, curatingInstitution.contributor.cristinId,
                                            curatingInstitution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution.contributor.topLevelCristinId,
                                                                           Set.of(curatingInstitution.contributor.cristinId))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from curating institution {0} operation on instance "
                              + "type {1} when degree has open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldDenyThesisCuratorFromCuratingInstitutionOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                                                            Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        owningInstitution.registrator.name,
                                                        owningInstitution.registrator.customer,
                                                        owningInstitution.registrator.topLevelCristinId);

        var contributor = createContributor(Role.CREATOR, curatingInstitution.contributor.cristinId,
                                            curatingInstitution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution.contributor.topLevelCristinId,
                                                                           Set.of(curatingInstitution.contributor.cristinId))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }


    @ParameterizedTest(name = "Should deny Thesis Curator from Registrators institution {0} operation on instance "
                              + "type {1} when degree has embargo but no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldDenyThesisCuratorOperationsOnEmbargoDegreeWithoutOpenFiles(PublicationOperation operation,
                                                                                               Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var thesisCurator = institution.thesisCurator;

        var publicationWithPendingFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator.name, registrator.customer, registrator.topLevelCristinId).copy()
                .withAssociatedArtifacts(List.of(randomPendingOpenFileWithEmbargo()))
                .build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(thesisCurator), identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publicationWithPendingFileWithEmbargo, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from Registrators institution {0} operation on instance "
                              + "type {1} when degree has open file with embargo")
    @MethodSource("argumentsForThesisCurator")
    void shouldDenyThesisCuratorOperationsOnEmbargoDegreeWithOpenFiles(PublicationOperation operation,
                                                                                            Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var thesisCurator = institution.thesisCurator;

        var publicationWithOpenFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator.name, registrator.customer, registrator.topLevelCristinId).copy()
                .withAssociatedArtifacts(List.of(randomOpenFileWithEmbargo())).build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(thesisCurator), identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publicationWithOpenFileWithEmbargo, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Embargo Thesis Curator from Registrators institution {0} operation on "
                              + "instance type {1} when degree has embargo but no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowEmbargoThesisCuratorOperationsOnEmbargoDegreeWithoutOpenFiles(PublicationOperation operation,
                                                                          Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var embargoThesisCurator = institution.embargoThesisCurator;

        var publicationWithPendingFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator.name, registrator.customer,
                              registrator.topLevelCristinId).copy()
                .withAssociatedArtifacts(List.of(randomPendingOpenFileWithEmbargo())).build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(embargoThesisCurator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publicationWithPendingFileWithEmbargo, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Embargo Thesis Curator from Registrators institution {0} operation on "
                              + "instance type {1} when degree has open file with embargo")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowEmbargoThesisCuratorOperationsOnEmbargoDegreeWithOpenFiles(PublicationOperation operation,
                                                                       Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var institution = Institution.random();
        var registrator = institution.registrator;
        var embargoThesisCurator = institution.embargoThesisCurator;

        var publicationWithOpenFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator.name, registrator.customer,
                              registrator.topLevelCristinId).copy()
                .withAssociatedArtifacts(List.of(randomOpenFileWithEmbargo())).build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(embargoThesisCurator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publicationWithOpenFileWithEmbargo, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from curating institution {0} operation on instance type "
                              + "{1} when curating institution has supervisor only and publication has no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldDenyThesisCuratorFromCuratingInstitutionOnDegreeWithoutOpenFilesWhenCuratorIsCuratingSupervisorOnly(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication =
            createPublicationWithoutOpenFiles(degreeInstanceTypeClass,
                                              owningInstitution.registrator.name,
                                              owningInstitution.registrator.customer,
                                              owningInstitution.registrator.cristinId)
                .copy()
                .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                .build();

        var contributor = createContributor(Role.SUPERVISOR, curatingInstitution.contributor.cristinId,
                                            curatingInstitution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));

        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(
            curatingInstitution.contributor.topLevelCristinId, Set.of(curatingInstitution.contributor.cristinId))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from curating institution {0} operation on instance type "
                              + "{1} when curating institution has supervisor only and publication has open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldDenyThesisCuratorFromCuratingInstitutionOnDegreeWithOpenFilesWhenCuratorIsCuratingSupervisorOnly(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication = createPublicationWithOpenFile(degreeInstanceTypeClass,
                                                        owningInstitution.registrator.name,
                                                        owningInstitution.registrator.customer,
                                                        owningInstitution.registrator.cristinId)
                              .copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var contributor = createContributor(Role.SUPERVISOR,
                                            curatingInstitution.contributor.cristinId,
                                            curatingInstitution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));

        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(curatingInstitution.contributor.topLevelCristinId,
                                           Set.of(curatingInstitution.contributor.cristinId))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should not throw NPE when contributor is missing ID")
    @MethodSource("argumentsForCurator")
    void shouldNotThrowNPEWhenContributorIsMissingId(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication = createPublication(degreeInstanceTypeClass,
                                            owningInstitution.registrator.name,
                                            owningInstitution.registrator.customer,
                                            owningInstitution.registrator.cristinId)
                              .copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var contributor = createContributorWithoutId(Role.CREATOR, curatingInstitution.contributor.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(contributor));

        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(curatingInstitution.contributor.topLevelCristinId,
                                           Set.of(curatingInstitution.contributor.cristinId))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator from curating institution {0} operation on instance type "
                              + "{1} when curating institution has both creator and supervisor")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromCuratingInstitutionOnDegreeWithNoOpenFilesWhenCuratorIsCuratingNotOnlySupervisor(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication =
            createPublicationWithoutOpenFiles(degreeInstanceTypeClass,
                                              owningInstitution.registrator.name,
                                              owningInstitution.registrator.customer,
                                              owningInstitution.registrator.cristinId)
                .copy()
                .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                .build();

        var supervisor = createContributor(Role.SUPERVISOR, curatingInstitution.contributor.cristinId,
                                           curatingInstitution.contributor.topLevelCristinId);
        var creator = createContributor(Role.CREATOR, curatingInstitution.registrator.cristinId,
                                        curatingInstitution.registrator.topLevelCristinId);
        publication.getEntityDescription().setContributors(List.of(supervisor, creator));

        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(
            curatingInstitution.contributor.topLevelCristinId, Set.of(supervisor.getIdentity().getId(),
                                                                       creator.getIdentity().getId()))));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Thesis curator from curating institution {0} operation on instance type "
                              + "{1} when curating institution is missing contributor IDs")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromCuratingInstitutionOnDegreeWithoutOpenFilesWhenCuratorIsCuratingContributorsWithoutId(
        PublicationOperation operation,
        Class<?> degreeInstanceTypeClass)
        throws JsonProcessingException, UnauthorizedException {

        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution;
        var curatingInstitution = suite.curatingInstitution;

        var publication = createPublicationWithoutOpenFiles(degreeInstanceTypeClass,
                                                            owningInstitution.registrator.name,
                                                            owningInstitution.registrator.customer,
                                                            owningInstitution.registrator.cristinId)
                              .copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        publication.getEntityDescription().setContributors(List.of());
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution.thesisCurator.topLevelCristinId,
                                                                           Set.of())));

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    private static Stream<Arguments> argumentsForRegistrator() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.DELETE,
                                      PublicationOperation.UPLOAD_FILE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    // TODO: Should we test that this should be allowed after open file?
    private static Stream<Arguments> argumentsEveryoneShouldBeAllowedAfterOpenFiles() {
        final var operations = Set.of(PublicationOperation.UPLOAD_FILE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForContributor() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForCurator() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.UPDATE_FILES,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForThesisCurator() {
        final var operations = Set.of(PublicationOperation.UPDATE,
                                      PublicationOperation.UPDATE_FILES,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> generateAllCombinationsOfOperationsAndInstanceClasses(
        final Set<PublicationOperation> operations) {
        return operations.stream()
                   .flatMap(operation -> Arrays.stream(PROTECTED_DEGREE_INSTANCE_TYPES).toList().stream()
                                             .map(instanceClass -> Arguments.of(operation, instanceClass)))
                   .toList()
                   .stream();
    }

    private static Contributor createContributor(Role role, URI cristinPersonId, URI cristinOrganizationId) {
        return new Contributor.Builder()
                   .withAffiliations(List.of(Organization.fromUri(cristinOrganizationId)))
                   .withIdentity(new Identity.Builder().withId(cristinPersonId).withName(randomString()).build())
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

    public static OpenFile randomOpenFileWithEmbargo() {
        return new OpenFile(UUID.randomUUID(), RandomDataGenerator.randomString(),
                            RandomDataGenerator.randomString(), RandomDataGenerator.randomInteger().longValue(),
                            RandomDataGenerator.randomUri(), PublisherVersion.PUBLISHED_VERSION,
                            Instant.now().plusSeconds(60 * 60 * 24),
                            RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                            RandomDataGenerator.randomString(), RandomDataGenerator.randomInstant(),
                            new UserUploadDetails(new Username(RandomDataGenerator.randomString()),
                                                  RandomDataGenerator.randomInstant()));
    }

    public static PendingOpenFile randomPendingOpenFileWithEmbargo() {
        return new PendingOpenFile(UUID.randomUUID(), RandomDataGenerator.randomString(),
                                   RandomDataGenerator.randomString(), RandomDataGenerator.randomInteger().longValue(),
                                   RandomDataGenerator.randomUri(), PublisherVersion.PUBLISHED_VERSION,
                                   Instant.now().plusSeconds(60 * 60 * 24),
                                   RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                                   RandomDataGenerator.randomString(),
                                   new UserUploadDetails(new Username(RandomDataGenerator.randomString()),
                                                  RandomDataGenerator.randomInstant()));
    }

    private RequestInfo toRequestInfo(User user) throws JsonProcessingException {
        return createUserRequestInfo(user.name, user.customer, user.accessRights, user.cristinId,
                                     user.topLevelCristinId);
    }

    private record InstitutionSuite (Institution owningInstitution, Institution curatingInstitution,
                                     Institution nonCuratingInstitution) {
        private static InstitutionSuite random() {
            return new InstitutionSuite(Institution.random(), Institution.random(), Institution.random());
        }
    }

    private record Institution (User registrator, User contributor, User curator, User thesisCurator,
                                User embargoThesisCurator) {
        private static Institution random() {
            var customer = randomUri();
            var topLevelCristinId = randomUri();
            return new Institution(
                User.randomRegistrator(customer, topLevelCristinId),
                User.randomContributor(customer, topLevelCristinId),
                User.randomCurator(customer, topLevelCristinId),
                User.randomThesisCurator(customer, topLevelCristinId),
                User.randomEmbargoThesisCurator(customer, topLevelCristinId));
        }
    }

    private record User(String name, URI cristinId, URI customer, URI topLevelCristinId,
                        List<AccessRight> accessRights) {
        private static User random() {
            return new User(randomString(), randomUri(), randomUri(), randomUri(), List.of());
        }

        private static User randomRegistrator(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId, getAccessRightsForRegistrator());
        }

        private static User randomContributor(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId, getAccessRightsForContributor());
        }

        private static User randomCurator(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId, getAccessRightsForCurator());
        }

        private static User randomThesisCurator(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId, getAccessRightsForThesisCurator());
        }

        private static User randomEmbargoThesisCurator(URI customer, URI topLevelCristinId) {
            return new User(randomString(), randomUri(), customer, topLevelCristinId, getAccessRightsForEmbargoThesisCurator());
        }
    }
}

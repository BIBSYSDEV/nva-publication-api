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
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.testutils.RandomDataGenerator;
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

        var registrator = randomString();
        var registratorCustomer = randomUri();
        var registratorCristinId = randomUri();
        var registratorTopLevelCristinOrgId = randomUri();

        var publication = operation == PublicationOperation.UNPUBLISH
                              ? createPublicationWithoutOpenOrInternalFiles(degreeInstanceClass,
                                                                          registrator,
                                                                          registratorCustomer,
                                                                          registratorTopLevelCristinOrgId)
                              : createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                                  registrator,
                                                                  registratorCustomer,
                                                                  registratorTopLevelCristinOrgId);

        var publicationWithStatus = publication.copy()
                                        .withStatus(operation == PublicationOperation.DELETE ? DRAFT : PUBLISHED)
                                        .build();

        var requestInfo = createUserRequestInfo(registrator,
                                                registratorCustomer,
                                                getAccessRightsForRegistrator(),
                                                registratorCristinId,
                                                registratorTopLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publicationWithStatus, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Registrator {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsForRegistrator")
    void shouldDenyRegistratorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                            Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var registrator = randomString();
        var registratorCustomer = randomUri();
        var registratorCristinId = randomUri();
        var registratorTopLevelCristinOrgId = randomUri();

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator,
                                                        registratorCustomer,
                                                        registratorTopLevelCristinOrgId);

        var requestInfo = createUserRequestInfo(registrator,
                                                registratorCustomer,
                                                getAccessRightsForRegistrator(),
                                                registratorCristinId,
                                                registratorTopLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var registratorCustomer = randomUri();
        var registratorTopLevelCristinOrgId = randomUri();

        var publication = operation == PublicationOperation.UNPUBLISH
                              ? createPublicationWithoutOpenOrInternalFiles(degreeInstanceClass,
                                                                            registrator,
                                                                            registratorCustomer,
                                                                            registratorTopLevelCristinOrgId)
                              : createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                                  registrator,
                                                                  registratorCustomer,
                                                                  registratorTopLevelCristinOrgId);

        var contributorTopLevelCristinOrgId = randomUri();
        var contributor = createContributor(Role.CREATOR, randomUri(), contributorTopLevelCristinOrgId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(contributorTopLevelCristinOrgId,
                                                                           Set.of(contributor.getIdentity().getId()))));

        var requestInfo = createUserRequestInfo(contributor.getIdentity().getName(),
                                                contributorTopLevelCristinOrgId,
                                                getAccessRightsForContributor(),
                                                contributor.getIdentity().getId(),
                                                contributorTopLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Contributor {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsForContributor")
    void shouldDenyContributorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var registrator = randomString();
        var registratorCustomer = randomUri();
        var registratorTopLevelCristinOrgId = randomUri();

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator,
                                                        registratorCustomer,
                                                        registratorTopLevelCristinOrgId);

        var contributorTopLevelCristinOrgId = randomUri();
        var contributor = createContributor(Role.CREATOR, randomUri(), contributorTopLevelCristinOrgId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(contributorTopLevelCristinOrgId,
                                                                           Set.of(contributor.getIdentity().getId()))));

        var requestInfo = createUserRequestInfo(contributor.getIdentity().getName(),
                                                contributorTopLevelCristinOrgId,
                                                getAccessRightsForContributor(),
                                                contributor.getIdentity().getId(),
                                                contributorTopLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on instance type {1} when degree has no open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorOperationsOnDegreeWithoutOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var registrator = randomString();
        var curator = randomString();
        var curatorCristinId = randomUri();

        var customer = randomUri();
        var topLevelCristinOrgId = randomUri();

        var publication = createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                            registrator,
                                                            customer,
                                                            topLevelCristinOrgId);

        var requestInfo = createUserRequestInfo(curator,
                                                customer,
                                                getAccessRightsForCurator(),
                                                curatorCristinId,
                                                topLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsForCurator")
    void shouldDenyCuratorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var registrator = randomString();
        var curator = randomString();
        var curatorCristinId = randomUri();

        var customer = randomUri();
        var topLevelCristinOrgId = randomUri();

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator,
                                                        customer,
                                                        topLevelCristinOrgId);

        var requestInfo = createUserRequestInfo(curator,
                                                customer,
                                                getAccessRightsForRegistrator(),
                                                curatorCristinId,
                                                topLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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
        var registrator = randomString();
        var registratorCustomer = randomUri();
        var registratorTopLevelCristinOrgId = randomUri();

        var publication = createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                            registrator,
                                                            registratorCustomer,
                                                            registratorTopLevelCristinOrgId);

        var contributorCristinId = randomUri();
        var curator = randomString();
        var curatorCristinId = randomUri();
        var curatingCustomer = randomUri();
        var curatingTopLevelCristinOrgId = randomUri();

        var contributor = createContributor(Role.CREATOR, contributorCristinId, curatingTopLevelCristinOrgId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingTopLevelCristinOrgId,
                                                                           Set.of(contributor.getIdentity().getId()))));

        var requestInfo = createUserRequestInfo(curator,
                                                curatingCustomer,
                                                getAccessRightsForCurator(),
                                                curatorCristinId,
                                                curatingTopLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var registratorCustomer = randomUri();
        var registratorTopLevelCristinOrgId = randomUri();

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator,
                                                        registratorCustomer,
                                                        registratorTopLevelCristinOrgId);

        var contributorCristinId = randomUri();
        var curator = randomString();
        var curatorCristinId = randomUri();
        var curatingCustomer = randomUri();
        var curatingTopLevelCristinOrgId = randomUri();

        var contributor = createContributor(Role.CREATOR, contributorCristinId, curatingTopLevelCristinOrgId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingTopLevelCristinOrgId,
                                                                           Set.of(contributor.getIdentity().getId()))));

        var requestInfo = createUserRequestInfo(curator,
                                                curatingCustomer,
                                                getAccessRightsForCurator(),
                                                curatorCristinId,
                                                curatingTopLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var thesisCurator = randomString();
        var thesisCuratorCristinId = randomUri();

        var customer = randomUri();
        var topLevelCristinOrgId = randomUri();

        var publication = createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                            registrator,
                                                            customer,
                                                            topLevelCristinOrgId);

        var requestInfo = createUserRequestInfo(thesisCurator,
                                                customer,
                                                getAccessRightsForThesisCurator(),
                                                thesisCuratorCristinId,
                                                topLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var curator = randomString();
        var curatorCristinId = randomUri();

        var customer = randomUri();
        var topLevelCristinOrgId = randomUri();

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator,
                                                        customer,
                                                        topLevelCristinOrgId);

        var requestInfo = createUserRequestInfo(curator,
                                                customer,
                                                getAccessRightsForThesisCurator(),
                                                curatorCristinId,
                                                topLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var registratorCustomer = randomUri();
        var registratorTopLevelCristinOrgId = randomUri();

        var publication = createPublicationWithoutOpenFiles(degreeInstanceClass,
                                                            registrator,
                                                            registratorCustomer,
                                                            registratorTopLevelCristinOrgId);

        var contributorCristinId = randomUri();
        var thesisCurator = randomString();
        var thesisCuratorCristinId = randomUri();
        var curatingCustomer = randomUri();
        var curatingTopLevelCristinOrgId = randomUri();

        var contributor = createContributor(Role.CREATOR, contributorCristinId, curatingTopLevelCristinOrgId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingTopLevelCristinOrgId,
                                                                           Set.of(contributor.getIdentity().getId()))));

        var requestInfo = createUserRequestInfo(thesisCurator,
                                                curatingCustomer,
                                                getAccessRightsForThesisCurator(),
                                                thesisCuratorCristinId,
                                                curatingTopLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var registratorCustomer = randomUri();
        var registratorTopLevelCristinOrgId = randomUri();

        var publication = createPublicationWithOpenFile(degreeInstanceClass,
                                                        registrator,
                                                        registratorCustomer,
                                                        registratorTopLevelCristinOrgId);

        var contributorCristinId = randomUri();
        var thesisCurator = randomString();
        var thesisCuratorCristinId = randomUri();
        var curatingCustomer = randomUri();
        var curatingTopLevelCristinOrgId = randomUri();

        var contributor = createContributor(Role.CREATOR, contributorCristinId, curatingTopLevelCristinOrgId);
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingTopLevelCristinOrgId,
                                                                           Set.of(contributor.getIdentity().getId()))));

        var requestInfo = createUserRequestInfo(thesisCurator,
                                                curatingCustomer,
                                                getAccessRightsForThesisCurator(),
                                                thesisCuratorCristinId,
                                                curatingTopLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }


    @ParameterizedTest(name = "Should deny Thesis Curator {0} operation on instance type {1} when degree has embargo "
                              + "but no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldDenyThesisCuratorOperationsOnEmbargoDegreeWithoutOpenFiles(PublicationOperation operation,
                                                                                               Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {

        var registrator = randomString();
        var thesisCurator = randomString();
        var thesisCuratorCristinId = randomUri();

        var customer = randomUri();
        var topLevelCristinOrgId = randomUri();

        var publicationWithPendingFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator, customer, topLevelCristinOrgId).copy()
                .withAssociatedArtifacts(List.of(randomPendingOpenFileWithEmbargo()))
                .build();

        var requestInfo = createUserRequestInfo(thesisCurator,
                                                customer,
                                                getAccessRightsForThesisCurator(),
                                                thesisCuratorCristinId,
                                                topLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var curator = randomString();
        var curatorCristinId = randomUri();

        var customer = randomUri();
        var topLevelCristinOrgId = randomUri();

        var publicationWithOpenFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator, customer, topLevelCristinOrgId).copy()
                .withAssociatedArtifacts(List.of(randomOpenFileWithEmbargo())).build();

        var requestInfo = createUserRequestInfo(curator,
                                                customer,
                                                getAccessRightsForThesisCurator(),
                                                curatorCristinId,
                                                topLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var thesisCurator = randomString();
        var thesisCuratorCristinId = randomUri();

        var customer = randomUri();
        var topLevelCristinOrgId = randomUri();

        var publicationWithPendingFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator, customer, topLevelCristinOrgId).copy()
                .withAssociatedArtifacts(List.of(randomPendingOpenFileWithEmbargo())).build();

        var requestInfo = createUserRequestInfo(thesisCurator,
                                                customer,
                                                getAccessRightsForEmbargoThesisCurator(),
                                                thesisCuratorCristinId,
                                                topLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var registrator = randomString();
        var curator = randomString();
        var curatorCristinId = randomUri();

        var customer = randomUri();
        var topLevelCristinOrgId = randomUri();

        var publicationWithOpenFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator, customer, topLevelCristinOrgId).copy()
                .withAssociatedArtifacts(List.of(randomOpenFileWithEmbargo())).build();

        var requestInfo = createUserRequestInfo(curator,
                                                customer,
                                                getAccessRightsForEmbargoThesisCurator(),
                                                curatorCristinId,
                                                topLevelCristinOrgId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var publication = createPublicationWithoutOpenFiles(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
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

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var publication = createPublicationWithOpenFile(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
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

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();

        var publication =
            createPublicationWithoutOpenFiles(degreeInstanceTypeClass, resourceOwner, institution, randomUri()).copy()
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

        var customer = randomUri();
        var registrator = randomString();
        var curatorUsername = randomString();
        var publication = createPublicationWithoutOpenFiles(degreeInstanceTypeClass, registrator, customer, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var cristinOrganizationId = randomUri();

        publication.getEntityDescription().setContributors(List.of());

        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of())));

        var requestInfo = createUserRequestInfo(curatorUsername, customer, getAccessRightsForThesisCurator(),
                                                cristinOrganizationId, curatingInstitution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

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
}

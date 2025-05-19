package no.unit.nva.publication.permissions.publication;

import static java.util.UUID.randomUUID;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.EVERYONE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.OWNER_ONLY;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.setContributor;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.setPublicationChannelWithDegreeScope;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.PermissionsTestUtils.Institution;
import no.unit.nva.publication.permissions.PermissionsTestUtils.InstitutionSuite;
import no.unit.nva.publication.permissions.PermissionsTestUtils.User;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DegreeDenyStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should deny anonymous user {0} operation on instance type {1} when degree has no open "
                              + "files")
    @MethodSource("argumentsForAnonymousUser")
    void shouldDenyAnonymousUserOperationsOnDegreeWithoutOpenFiles(PublicationOperation operation,
                                                                   Class<?> degreeInstanceClass) {
        var registrator = User.random();
        var publication = createPublicationWithoutAcceptedFiles(degreeInstanceClass,
                                                                registrator.name(),
                                                                registrator.customer(),
                                                                registrator.topLevelCristinId());

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), null)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny anonymous user {0} operation on instance type {1} when degree has open "
                              + "files")
    @MethodSource("argumentsForAnonymousUser")
    void shouldDenyAnonymousUserOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                                Class<?> degreeInstanceClass) {
        var registrator = User.random();
        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), null)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Registrator {0} operation on instance type {1} when degree has no open "
                              + "files")
    @MethodSource("argumentsForRegistrator")
    void shouldAllowRegistratorOperationsOnDegreeWithoutApprovedFiles(PublicationOperation operation,
                                                             Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var registrator = User.random();
        var publication = createPublicationWithoutAcceptedFiles(degreeInstanceClass,
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

    @ParameterizedTest(name = "Should deny Registrator {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsForRegistratorExcludingUploadFileAndPartialUpdate")
    void shouldDenyRegistratorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var registrator = User.random();
        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Contributor {0} operation on instance type {1} when degree has no open "
                              + "files")
    @MethodSource("argumentsForContributor")
    void shouldAllowContributorOperationsOnDegreeWithoutApprovedFiles(PublicationOperation operation,
                                                                    Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var publication = createPublicationWithoutAcceptedFiles(degreeInstanceClass,
                                                                registrator.name(),
                                                                registrator.customer(),
                                                                registrator.topLevelCristinId());

        setContributor(publication, institution.contributor());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(institution.contributor()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Contributor {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsForContributorExcludingUploadFileAndPartialUpdate")
    void shouldDenyContributorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        setContributor(publication, institution.contributor());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(institution.contributor()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on instance type {1} when degree has no open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorOperationsOnDegreeWithoutApprovedFiles(PublicationOperation operation,
                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var curator = institution.curator();

        var publication = createPublicationWithoutAcceptedFiles(degreeInstanceClass,
                                                                registrator.name(),
                                                                registrator.customer(),
                                                                registrator.topLevelCristinId());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on instance type {1} when degree has open files")
    @MethodSource("degreesProvider")
    void shouldDenyUpdateForCuratorOperationsOnDegreeWithOpenFiles(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var curator = institution.curator();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should allow Curator from curating institution {0} operation on instance type {1} "
                              + "when degree has no open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorOnCuratingInstitutionOperationsOnDegreeWithoutApprovedFiles(PublicationOperation operation,
                                                                                   Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithoutAcceptedFiles(degreeInstanceClass,
                                                                owningInstitution.registrator().name(),
                                                                owningInstitution.registrator().customer(),
                                                                owningInstitution.registrator().topLevelCristinId());

        setContributor(publication, curatingInstitution.contributor());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator from curating institution {0} operation on instance type {1} when"
                              + " degree has open files")
    @MethodSource("degreesProvider")
    void shouldDenyUpdateForCuratorOnCuratingInstitutionOperationsOnDegreeWithOpenFiles(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        setContributor(publication, curatingInstitution.contributor());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator from Registrators institution {0} operation on instance "
                              + "type {1} when degree has no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromRegistratorsInstitutionOperationsOnDegreeWithoutOpenFiles(
        PublicationOperation operation,
        Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var thesisCurator = institution.thesisCurator();

        var publication = createPublicationWithoutAcceptedFiles(degreeInstanceClass,
                                                                registrator.name(),
                                                                registrator.customer(),
                                                                registrator.topLevelCristinId());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator from Registrators institution {0} operation on instance "
                              + "type {1} when degree has open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromRegistratorsInstitutionOperationsOnDegreeWithOpenFiles(
        PublicationOperation operation,
        Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var thesisCurator = institution.thesisCurator();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator from curating institution {0} operation on instance "
                              + "type {1} when degree has no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorFromCuratingInstitutionOperationsOnDegreeWithoutApprovedFiles(
        PublicationOperation operation,
        Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithoutAcceptedFiles(degreeInstanceClass,
                                                                owningInstitution.registrator().name(),
                                                                owningInstitution.registrator().customer(),
                                                                owningInstitution.registrator().topLevelCristinId());

        setContributor(publication, curatingInstitution.contributor());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from curating institution {0} operation on instance "
                              + "type {1} when degree has open files")
    @MethodSource("degreesProvider")
    void shouldDenyUpdateForThesisCuratorFromCuratingInstitutionOperationsOnDegreeWithOpenFiles(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        setContributor(publication, curatingInstitution.contributor());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from Registrators institution {0} operation on instance "
                              + "type {1} when degree has embargo but no open files")
    @MethodSource("degreesProvider")
    void shouldDenyUpdateForThesisCuratorOperationsOnEmbargoDegreeWithoutOpenFiles(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var thesisCurator = institution.thesisCurator();

        var publicationWithPendingFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator.name(), registrator.customer(),
                              registrator.topLevelCristinId()).copy()
                .withAssociatedArtifacts(List.of(randomPendingOpenFileWithEmbargo()))
                .build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publicationWithPendingFileWithEmbargo),
                                           userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from Registrators institution {0} operation on instance "
                              + "type {1} when degree has open file with embargo")
    @MethodSource("degreesProvider")
    void shouldDenyThesisCuratorOperationsOnEmbargoDegreeWithOpenFiles(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var thesisCurator = institution.thesisCurator();

        var publicationWithOpenFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator.name(), registrator.customer(),
                              registrator.topLevelCristinId()).copy()
                .withAssociatedArtifacts(List.of(randomOpenFileWithEmbargo())).build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(thesisCurator),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publicationWithOpenFileWithEmbargo), userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should allow Embargo Thesis Curator from Registrators institution {0} operation on "
                              + "instance type {1} when degree has embargo but no open files")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowEmbargoThesisCuratorOperationsOnEmbargoDegreeWithoutOpenFiles(PublicationOperation operation,
                                                                                  Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var embargoThesisCurator = institution.embargoThesisCurator();

        var publicationWithPendingFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator.name(), registrator.customer(),
                              registrator.topLevelCristinId()).copy()
                .withAssociatedArtifacts(List.of(randomPendingOpenFileWithEmbargo())).build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(embargoThesisCurator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publicationWithPendingFileWithEmbargo), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Embargo Thesis Curator from Registrators institution {0} operation on "
                              + "instance type {1} when degree has open file with embargo")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowEmbargoThesisCuratorOperationsOnEmbargoDegreeWithOpenFiles(PublicationOperation operation,
                                                                               Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var embargoThesisCurator = institution.embargoThesisCurator();

        var publicationWithOpenFileWithEmbargo =
            createPublication(degreeInstanceClass, registrator.name(), registrator.customer(),
                              registrator.topLevelCristinId()).copy()
                .withAssociatedArtifacts(List.of(randomOpenFileWithEmbargo())).build();

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(embargoThesisCurator),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publicationWithOpenFileWithEmbargo), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Registrator {0} operation on instance type {1} when degree has open file "
                              + "and channel owned by own institution")
    @MethodSource("argumentsForRegistratorExcludingUploadFileAndPartialUpdate")
    void shouldDenyRegistratorWhenOpenFileAndChannelOwnedByOwnInstitution(PublicationOperation operation,
                                                                          Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on instance type {1} when degree has open file "
                              + "and channel owned by own institution")
    @MethodSource("degreesProvider")
    void shouldDenyUpdateForCuratorWhenOpenFileAndChannelOwnedByOwnInstitution(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(owningInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator {0} operation on instance type {1} when degree has open "
                              + "file and channel owned by own institution")
    @MethodSource("argumentsForThesisCurator")
    void shouldAllowThesisCuratorWhenOpenFileAndChannelOwnedByOwnInstitution(PublicationOperation operation,
                                                                             Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(owningInstitution.thesisCurator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator from curating institution {0} operation on instance type {1} when "
                              + "degree has open file and channel owned by own institution")
    @MethodSource("degreesProvider")
    void shouldDenyUpdateForCuratorFromCuratingInstitutionWhenOpenFileAndChannelOwnedByOwnInstitution(
        Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        var contributor = createContributor(Role.CREATOR, curatingInstitution.contributor().cristinId(),
                                            curatingInstitution.contributor().topLevelCristinId());
        publication.getEntityDescription().setContributors(List.of(contributor));
        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(curatingInstitution.contributor().topLevelCristinId(),
                                           Set.of(curatingInstitution.contributor().cristinId()))));

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from curating institution {0} operation on instance type {1} "
                              + "when degree has open file and channel owned by own institution")
    @MethodSource("degreesProvider")
    void shouldDenyUpdateForThesisCuratorFromAnotherInstitutionWhenOpenFileAndChannelOwnedByOwnInstitution(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should deny Registrator {0} operation on instance type {1} when degree has open file "
                              + "and channel owned by another institution")
    @MethodSource("argumentsForRegistratorExcludingUploadFileAndPartialUpdate")
    void shouldDenyRegistratorWhenOpenFileAndChannelOwnedByAnotherInstitution(PublicationOperation operation,
                                                                              Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var anotherInstitution = suite.nonCuratingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, anotherInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(owningInstitution.registrator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on instance type {1} when degree has open file "
                              + "and channel owned by another institution")
    @MethodSource("degreesProvider")
    void shouldDenyCuratorWhenOpenFileAndChannelOwnedByAnotherInstitution(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var anotherInstitution = suite.nonCuratingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, anotherInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(owningInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator {0} operation on instance type {1} when degree has open "
                              + "file and channel owned by another institution")
    @MethodSource("degreesProvider")
    void shouldDenyThesisCuratorWhenAcceptedFileAndChannelOwnedByAnotherInstitution(Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var anotherInstitution = suite.nonCuratingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, anotherInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(owningInstitution.thesisCurator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should deny Curator from another institution {0} operation on instance type {1} when "
                              + "degree has open file and channel owned by that institution")
    @MethodSource("degreesProvider")
    void shouldDenyUpdateForNonDegreeCuratorWhenCuratorInstitutionOnlyRelatesThroughChannelClaimAndPublicationIsADegreeWithOpenFile(
        Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, curatingInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should allow Thesis Curator from another institution {0} operation on instance type {1} "
                              + "when degree has open file and channel owned by that institution")
    @MethodSource("argumentsForThesisCuratorExcludingUploadFileAndPartialUpdate")
    void shouldAllowThesisCuratorFromAnotherInstitutionWhenOpenFileAndChannelOwnedByThatInstitution(
        PublicationOperation operation,
        Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, curatingInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.thesisCurator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator from institution Z {0} operation on instance type {1} when "
                              + "Registrator is from institution X and degree has open file and channel owned by "
                              + "institution Y")
    @MethodSource("argumentsForCurator")
    void shouldDenyCuratorFromInstitutionZWhenRegistratorFromInstitutionXAndOpenFileAndChannelOwnedByInstitutionY(
        PublicationOperation operation,
        Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();
        var anotherInstitution = suite.nonCuratingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, curatingInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(anotherInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Thesis Curator from institution Z {0} operation on instance type {1} when "
                              + "Registrator is from institution X and degree has open file and channel owned by "
                              + "institution Y")
    @MethodSource("argumentsForThesisCurator")
    void shouldDenyThesisCuratorFromInstitutionZWhenRegistratorFromInstitutionXAndOpenFileAndChannelOwnedByInstitutionY(
        PublicationOperation operation,
        Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();
        var anotherInstitution = suite.nonCuratingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            owningInstitution.registrator().name(),
                                                            owningInstitution.registrator().customer(),
                                                            owningInstitution.registrator().topLevelCristinId());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithDegreeScope(resource, curatingInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(anotherInstitution.thesisCurator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
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

        var publication = createPublicationWithoutAcceptedFiles(degreeInstanceTypeClass,
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

    @ParameterizedTest(name = "Should allow Registrator {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsEveryoneShouldBeAllowedAfterOpenFiles")
    void shouldAllowRegistratorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                               Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var registrator = User.random();
        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Contributor {0} operation on instance type {1} when degree has open files")
    @MethodSource("argumentsEveryoneShouldBeAllowedAfterOpenFiles")
    void shouldAllowContributorOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                               Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var contributor = institution.contributor();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());
        setContributor(publication, contributor);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(contributor), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from another institution {0} operation on instance type {1} "
                              + "when degree has open files")
    @MethodSource("argumentsEveryoneShouldBeAllowedAfterOpenFiles")
    void shouldAllowContributorFromAnotherInstitutionOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                                                     Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());
        setContributor(publication, curatingInstitution.contributor());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.contributor()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator from curating institution {0} operation on instance type {1} "
                              + "when degree has open files")
    @MethodSource("argumentsEveryoneShouldBeAllowedAfterOpenFiles")
    void shouldAllowCuratorFromCuratingInstitutionOperationsOnDegreeWithOpenFiles(PublicationOperation operation,
                                                                                  Class<?> degreeInstanceClass)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createPublicationWithAcceptedFile(degreeInstanceClass,
                                                            registrator.name(),
                                                            registrator.customer(),
                                                            registrator.topLevelCristinId());
        setContributor(publication, curatingInstitution.contributor());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    private static Stream<Arguments> argumentsForAnonymousUser() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.DELETE,
                                      PublicationOperation.UPDATE_FILES,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForRegistrator() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.DELETE,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForRegistratorExcludingUploadFileAndPartialUpdate() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.DELETE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsEveryoneShouldBeAllowedAfterOpenFiles() {
        final var operations = Set.of(PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForContributor() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForContributorExcludingUploadFileAndPartialUpdate() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UNPUBLISH);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForCurator() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UPDATE_FILES,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForThesisCurator() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UPDATE_FILES,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return generateAllCombinationsOfOperationsAndInstanceClasses(operations);
    }

    private static Stream<Arguments> argumentsForThesisCuratorExcludingUploadFileAndPartialUpdate() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UPDATE_FILES,
                                      PublicationOperation.UNPUBLISH);

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

    private static OpenFile randomOpenFileWithEmbargo() {
        return new OpenFile(randomUUID(), RandomDataGenerator.randomString(),
                            RandomDataGenerator.randomString(), RandomDataGenerator.randomInteger().longValue(),
                            RandomDataGenerator.randomUri(), PublisherVersion.PUBLISHED_VERSION,
                            Instant.now().plusSeconds(60 * 60 * 24),
                            RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                            RandomDataGenerator.randomString(), RandomDataGenerator.randomInstant(),
                            new UserUploadDetails(new Username(RandomDataGenerator.randomString()),
                                                  RandomDataGenerator.randomInstant()));
    }

    private static PendingOpenFile randomPendingOpenFileWithEmbargo() {
        return new PendingOpenFile(randomUUID(), RandomDataGenerator.randomString(),
                                   RandomDataGenerator.randomString(), RandomDataGenerator.randomInteger().longValue(),
                                   RandomDataGenerator.randomUri(), PublisherVersion.PUBLISHED_VERSION,
                                   Instant.now().plusSeconds(60 * 60 * 24),
                                   RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                                   RandomDataGenerator.randomString(),
                                   new UserUploadDetails(new Username(RandomDataGenerator.randomString()),
                                                         RandomDataGenerator.randomInstant()));
    }

    private RequestInfo toRequestInfo(User user) throws JsonProcessingException {
        return createUserRequestInfo(user.name(), user.customer(), user.accessRights(), user.cristinId(),
                                     user.topLevelCristinId());
    }
}

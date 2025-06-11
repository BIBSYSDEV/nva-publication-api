package no.unit.nva.publication.permissions.publication;

import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.EVERYONE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.OWNER_ONLY;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.setContributor;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.setPublicationChannelOutsideOfScope;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.setPublicationChannelWithinScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.PermissionsTestUtils.Institution;
import no.unit.nva.publication.permissions.PermissionsTestUtils.InstitutionSuite;
import no.unit.nva.publication.permissions.PermissionsTestUtils.User;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ClaimedChannelPermissionStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should deny curator from non curating institution {0} operation on publication with "
                              + "non claimed channel")
    @MethodSource("argumentsForCurator")
    void shouldDenyNonCuratingInstitutionWhenNoClaimedChannel(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.curator()),
                                                                     identityServiceClient);
        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow registrator {0} operation on publication with non claimed "
                              + "channel and no open files")
    @MethodSource("argumentsForRegistrator")
    void shouldAllowRegistratorWhenChannelIsNotClaimedAndPublicationHasNoOpenFile(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        var publicationWithStatus = publication.copy()
                                        .withStatus(operation == PublicationOperation.DELETE ? DRAFT : PUBLISHED)
                                        .build();

        var resource = Resource.fromPublication(publicationWithStatus);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow registrator {0} operation on publication with non claimed channel and "
                              + "open files")
    @MethodSource("argumentsForRegistratorAfterOpenFiles")
    void shouldAllowRegistratorWhenChannelIsNotClaimedAndPublicationHasOpenFile(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from same institution {0} operation on publication with non "
                              + "claimed channel and no open files")
    @MethodSource("argumentsForContributor")
    void shouldAllowContributorFromSameInstitutionWhenChannelIsNotClaimedAndPublicationHasNoOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var contributor = institution.contributor();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, contributor);

        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(contributor), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from another institution {0} operation on publication with "
                              + "non claimed channel and no open files")
    @MethodSource("argumentsForContributor")
    void shouldAllowContributorFromAnotherInstitutionWhenChannelIsNotClaimedAndPublicationHasNoOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.contributor()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator from curating institution {0} operation on publication with non "
                              + "claimed channel and no open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorFromCuratingInstitutionWhenChannelIsNotClaimedAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions.create(resource, userInstance).allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator from curating institution {0} operation on publication with non "
                              + "claimed channel and open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorFromCuratingInstitutionWhenChannelIsNotClaimedAndPublicationHasOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow registrator {0} operation on publication with channel claimed by own "
                              + "institution with publishing policy 'Everyone' and no open files")
    @MethodSource("argumentsForRegistrator")
    void shouldAllowRegistratorWhenChannelIsClaimedByOwnInstitutionWithPublishingPolicyEveryoneAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        var publicationWithStatus = publication.copy()
                                        .withStatus(operation == PublicationOperation.DELETE ? DRAFT : PUBLISHED)
                                        .build();

        var resource = Resource.fromPublication(publicationWithStatus);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator {0} operation on publication with channel claimed by own "
                              + "institution with publishing policy 'Everyone' and no open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorWhenChannelIsClaimedByOwnInstitutionWithPublishingPolicyEveryoneAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curator = owningInstitution.curator();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from same institution {0} operation on publication with "
                              + "channel claimed by own institution with publishing policy 'Everyone' and no open "
                              + "files")
    @MethodSource("argumentsForContributor")
    void shouldAllowContributorFromSameInstitutionWhenChannelIsClaimedByOwnInstitutionWithPublishingPolicyEveryoneAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var contributor = owningInstitution.contributor();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, contributor);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(contributor), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from another institution {0} operation on publication with "
                              + "channel claimed by own institution with publishing policy 'Everyone' and no open "
                              + "files")
    @MethodSource("argumentsForContributor")
    void shouldAllowContributorFromAnotherInstitutionWhenChannelIsClaimedByOwnInstitutionWithPublishingPolicyEveryoneAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.contributor()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator from curating institution {0} operation on publication with "
                              + "channel claimed by own institution with publishing policy 'Everyone' and no open "
                              + "files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorFromCuratingInstitutionWhenChannelIsClaimedByRegistratorInstitutionWithPublishingPolicyEveryoneAndPublicationHasNoApprovedFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(owningInstitution.registrator());
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow registrator {0} operation on publication with channel claimed by own "
                              + "institution with publishing policy 'OwnerOnly' and no open files")
    @MethodSource("argumentsForRegistrator")
    void shouldAllowRegistratorWhenChannelIsClaimedByOwnInstitutionWithPublishingPolicyOwnerOnlyAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);

        var publicationWithStatus = publication.copy()
                                        .withStatus(operation == PublicationOperation.DELETE ? DRAFT : PUBLISHED)
                                        .build();

        var resource = Resource.fromPublication(publicationWithStatus);
        setPublicationChannelWithinScope(resource, owningInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator {0} operation on publication with channel claimed by own "
                              + "institution with publishing policy 'OwnerOnly' and no open files")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorWhenChannelIsClaimedByOwnInstitutionWithPublishingPolicyOwnerOnlyAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curator = owningInstitution.curator();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from same institution {0} operation on publication with "
                              + "channel claimed by own institution with publishing policy 'OwnerOnly' and no open "
                              + "files")
    @MethodSource("argumentsForContributor")
    void shouldAllowContributorFromSameInstitutionWhenChannelIsClaimedByContributorInstitutionWithPublishingPolicyOwnerOnlyAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var contributor = owningInstitution.contributor();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, contributor);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(contributor), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @Disabled("Disabled until publishing policy is validated")
    @Test()
    @DisplayName(value = "Should deny curator from curating institution {0} operation on publication with "
                         + "channel claimed by own institution with publishing policy 'OwnerOnly' and no open "
                         + "files")
    void shouldDenyUpdateForCuratorFromCuratingInstitutionWhenChannelIsClaimedByRegistratorInstitutionWithPublishingPolicyOwnerOnlyAndPublicationHasNoOpenFile()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(owningInstitution.registrator());
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should deny contributor from another institution {0} operation on publication with "
                              + "channel claimed by own institution with publishing policy 'OwnerOnly' and no open "
                              + "files")
    @MethodSource("argumentsForContributorExcludingUploadFileAndPartialUpdate")
    void shouldAllowContributorFromAnotherInstitutionWhenChannelIsClaimedByRegistratorInstitutionWithPublishingPolicyEveryoneAndPublicationHasNoOpenFile(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.contributor()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow registrator {0} operation on publication with open files and channel "
                              + "claimed by own institution with editing policy 'Everyone'")
    @MethodSource("argumentsForRegistratorAfterOpenFiles")
    void shouldAllowRegistratorWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyEveryoneAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, EVERYONE);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator {0} operation on publication with open files and channel claimed "
                              + "by own institution with editing policy 'Everyone'")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyEveryoneAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curator = owningInstitution.curator();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, EVERYONE);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from same institution {0} operation on publication with open "
                              + "files and channel claimed by own institution with editing policy 'Everyone'")
    @MethodSource("argumentsForContributorAfterOpenFiles")
    void shouldAllowContributorFromSameInstitutionWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyEveryoneAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var contributor = owningInstitution.contributor();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, contributor);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, EVERYONE);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(contributor), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from another institution {0} operation on publication with "
                              + "open files and channel claimed by own institution with editing policy 'Everyone'")
    @MethodSource("argumentsForContributorAfterOpenFiles")
    void shouldAllowContributorFromAnotherInstitutionWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyEveryoneAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, EVERYONE);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.contributor()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator from curating institution {0} operation on publication with open "
                              + "files and channel claimed by own institution with editing policy 'Everyone'")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorFromCuratingInstitutionWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyEveryoneAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, EVERYONE);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny curator from non curating institution {0} operation on publication with "
                              + "open files and channel claimed by own institution with editing policy 'Everyone'")
    @MethodSource("argumentsForCurator")
    void shouldDenyCuratorFromNonCuratingInstitutionWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyEveryoneAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, EVERYONE);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow registrator {0} operation on publication with open files and channel "
                              + "claimed by own institution with editing policy 'OwnerOnly'")
    @MethodSource("argumentsForRegistratorAfterOpenFiles")
    void shouldAllowRegistratorWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyOwnerOnlyAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator {0} operation on publication with open files and channel claimed "
                              + "by own institution with editing policy 'OwnerOnly'")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyOwnerOnlyAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curator = owningInstitution.curator();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow contributor from same institution {0} operation on publication with open "
                              + "files and channel claimed by own institution with editing policy 'OwnerOnly'")
    @MethodSource("argumentsForContributor")
    void shouldAllowContributorFromSameInstitutionWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyOwnerOnlyAndPublicationHasApprovedFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var contributor = owningInstitution.contributor();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, contributor);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(contributor), identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny contributor from another institution {0} operation on publication with "
                              + "open files and channel claimed by own institution with editing policy 'OwnerOnly'")
    @MethodSource("argumentsForContributorExcludingUploadFileAndPartialUpdate")
    void shouldDenyContributorFromAnotherInstitutionWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyOwnerOnlyAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.contributor()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @DisplayName(value = "Should deny curator from curating institution {0} operation on publication with open "
                              + "files and channel claimed by own institution with editing policy 'OwnerOnly'")
    @Test
    void shouldDenyCuratorFromCuratingInstitutionWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyOwnerOnlyAndPublicationHasOpenFiles()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @ParameterizedTest(name = "Should deny curator from non curating institution {0} operation on publication with "
                              + "open files and channel claimed by own institution with editing policy 'OwnerOnly'")
    @MethodSource("argumentsForCurator")
    void shouldDenyCuratorFromNonCuratingInstitutionWhenChannelIsClaimedByOwnInstitutionWithEditingPolicyOwnerOnlyAndPublicationHasOpenFiles(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny contributor from non curating institution {0} operation on publication with "
                              + "channel claimed by contributors institution")
    @MethodSource("argumentsForContributor")
    void shouldDenyContributorFromNonCuratingInstitutionWhenChannelIsClaimedByTheirInstitution(
        PublicationOperation operation) throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, nonCuratingInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(
            toRequestInfo(nonCuratingInstitution.contributor()),
            identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow curator from non curating institution {0} operation on publication "
                              + "with channel claimed by curators institution")
    @MethodSource("argumentsForCurator")
    void shouldAllowCuratorFromNonCuratingInstitutionWhenChannelIsClaimedByTheirInstitution(
        PublicationOperation operation) throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, nonCuratingInstitution, EVERYONE, EVERYONE);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.curator()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow editor from non curating institution {0} operation on publication "
                              + "with channel claimed by editors institution")
    @MethodSource("argumentsForEditor")
    void shouldAllowEditorFromNonCuratingInstitutionWhenChannelIsClaimedByEditorInstitution(
        PublicationOperation operation) throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator).copy()
                              .withStatus(operation == PublicationOperation.REPUBLISH ? UNPUBLISHED : PUBLISHED)
                              .build();

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, nonCuratingInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.editor()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(operation));
    }

    @DisplayName(value = "Should deny editor from non curating institution {0} operation on publication "
                              + "with channel claimed by another institution")
    @Test
    void shouldDenyEditorFromNonCuratingInstitutionWhenChannelIsClaimedByAnotherInstitution() throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, EVERYONE, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.editor()),
                                                                     identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(UPDATE));
    }

    @Test()
    void shouldAllowEditorFromNonCuratingInstitutionWhenChannelIsNotClaimedByAnyone() throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.editor()),
                                                                     identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @Test
    void shouldAllowRegistratorToUploadFileOnPublicationWithoutClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowRegistratorToUploadFileOnPublicationWithClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, institution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowContributorToUploadFileOnPublicationWithoutClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var contributor = institution.contributor();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, contributor);
        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(contributor),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowContributorToUploadFileOnPublicationWithClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var contributor = institution.contributor();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, contributor);
        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, institution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(contributor),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowCuratorToUploadFileOnPublicationWithoutClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var curator = institution.curator();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowCuratorToUploadFileOnPublicationWithClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var curator = institution.curator();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, institution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowContributorFromAnotherInstitutionToUploadFileOnPublicationWithoutClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());
        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.contributor()),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowContributorFromAnotherInstitutionToUploadFileOnPublicationWithClaimedChannelWithPolicyOwnerOnly()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());
        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.contributor()),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowCuratorFromCuratingInstitutionToUploadFileOnPublicationWithoutClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());
        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowCuratorFromCuratingInstitutionToUploadFileOnPublicationWithClaimedChannelWithPolicyOwnerOnly()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());
        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curatingInstitution.curator()),
                                                                     identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldDenyCuratorFromNonCuratingInstitutionToUploadFileOnPublicationWithoutClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());
        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of());

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.curator()),
                                                                     identityServiceClient);
        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldDenyCuratorFromNonCuratingInstitutionToUploadFileOnPublicationWithClaimedChannel()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curatingInstitution = suite.curatingInstitution();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        setContributor(publication, curatingInstitution.contributor());
        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, owningInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(nonCuratingInstitution.curator()),
                                                                     identityServiceClient);
        Assertions.assertFalse(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(PublicationOperation.UPLOAD_FILE));
    }

    @Test
    void shouldAllowExternalUserWhenPublishingPolicyOwnerOnly() {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var publication = createNonDegreePublicationWithoutFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, institution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = createExternalUser(resource);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(UPDATE));
    }

    @Test
    void shouldAllowExternalUserWhenEditingPolicyOwnerOnly() {
        var institution = Institution.random();
        var registrator = institution.registrator();
        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);

        var resource = Resource.fromPublication(publication);
        setPublicationChannelWithinScope(resource, institution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = createExternalUser(resource);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(UPDATE));
    }

    @Test
    void shouldAllowRegistratorWhenChannelIsClaimedByAnotherInstitutionWithEditingPolicyOwnerOnlyButOutsideOfScope()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        var resource = Resource.fromPublication(publication);
        setPublicationChannelOutsideOfScope(resource, nonCuratingInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(registrator), identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                   .create(resource, userInstance)
                                   .allowsAction(UPDATE));
    }

    @Test
    void shouldAllowCuratorWhenChannelIsClaimedByAnotherInstitutionWithEditingPolicyOwnerOnlyButOutsideOfScope()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var curator = owningInstitution.curator();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        var resource = Resource.fromPublication(publication);
        setPublicationChannelOutsideOfScope(resource, nonCuratingInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(curator), identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(UPDATE));
    }

    @Test
    void shouldAllowEditorWhenChannelIsClaimedByAnotherInstitutionWithEditingPolicyOwnerOnlyButOutsideOfScope()
        throws JsonProcessingException, UnauthorizedException {
        var suite = InstitutionSuite.random();
        var owningInstitution = suite.owningInstitution();
        var registrator = owningInstitution.registrator();
        var editor = owningInstitution.editor();
        var nonCuratingInstitution = suite.nonCuratingInstitution();

        var publication = createNonDegreePublicationWithFinalizedFiles(registrator);
        var resource = Resource.fromPublication(publication);
        setPublicationChannelOutsideOfScope(resource, nonCuratingInstitution, OWNER_ONLY, OWNER_ONLY);

        var userInstance = RequestUtil.createUserInstanceFromRequest(toRequestInfo(editor), identityServiceClient);
        Assertions.assertTrue(PublicationPermissions
                                  .create(resource, userInstance)
                                  .allowsAction(UPDATE));
    }

    private static Stream<Arguments> argumentsForRegistrator() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.DELETE,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return operations.stream().map(Arguments::of);
    }

    private static Stream<Arguments> argumentsForRegistratorAfterOpenFiles() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return operations.stream().map(Arguments::of);
    }

    private static Stream<Arguments> argumentsForContributor() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return operations.stream().map(Arguments::of);
    }

    private static Stream<Arguments> argumentsForContributorAfterOpenFiles() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return operations.stream().map(Arguments::of);
    }

    private static Stream<Arguments> argumentsForContributorExcludingUploadFileAndPartialUpdate() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UNPUBLISH);

        return operations.stream().map(Arguments::of);
    }

    private static Stream<Arguments> argumentsForCurator() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.UPDATE_FILES,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.UPLOAD_FILE,
                                      PublicationOperation.PARTIAL_UPDATE);

        return operations.stream().map(Arguments::of);
    }

    private static Stream<Arguments> argumentsForEditor() {
        final var operations = Set.of(UPDATE,
                                      PublicationOperation.PARTIAL_UPDATE,
                                      PublicationOperation.UNPUBLISH,
                                      PublicationOperation.REPUBLISH);

        return operations.stream().map(Arguments::of);
    }

    private static UserInstance createExternalUser(Resource resource) {
        return UserInstance.createExternalUser(resource.toPublication().getResourceOwner(), randomUri());
    }

    private RequestInfo toRequestInfo(User user) throws JsonProcessingException {
        return createUserRequestInfo(user.name(), user.customer(), user.accessRights(), user.cristinId(),
                                     user.topLevelCristinId());
    }

    private Publication createNonDegreePublicationWithFinalizedFiles(User registrator) {
        return createPublicationWithFinalizedFiles(AcademicArticle.class,
                                                   registrator.name(),
                                                   registrator.customer(),
                                                   registrator.topLevelCristinId());
    }

    private Publication createNonDegreePublicationWithoutFinalizedFiles(User registrator) {
        return createPublicationWithoutFinalizedFiles(AcademicArticle.class,
                                                      registrator.name(),
                                                      registrator.customer(),
                                                      registrator.topLevelCristinId());
    }
}

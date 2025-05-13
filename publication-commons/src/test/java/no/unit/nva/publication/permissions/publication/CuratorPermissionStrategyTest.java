package no.unit.nva.publication.permissions.publication;

import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.getAccessRightsForCurator;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.getAccessRightsForThesisCurator;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.Role;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class CuratorPermissionStrategyTest extends PublicationPermissionStrategyTest {

    //region Non-degree publications
    @ParameterizedTest(name = "Should allow Curator {0} operation on non-degree resources belonging to the "
                              + "institution based on publication owner")
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE,
        names = {"UPDATE", "UNPUBLISH", "SUPPORT_REQUEST_CREATE"})
    void shouldAllowCuratorOnNonDegreeBasedOnOwner(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createNonDegreePublication(resourceOwner, institution).copy()
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForCurator(), cristinId,
                                                publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on non-degree resources belonging to the "
                              + "institution based on contributors")
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE,
        names = {"UPDATE", "UNPUBLISH", "UPDATE_FILES", "SUPPORT_REQUEST_CREATE"})
    void shouldAllowCuratorOnNonDegreeBasedOnContributors(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var customer = randomUri();
        var contributor = randomString();
        var contributorCristinId = randomUri();
        var curatorUsername = randomString();
        var personCristinId = randomUri();
        var topLevelCristinOrgId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, customer, getAccessRightsForCurator(), personCristinId,
                                                topLevelCristinOrgId);
        var publication = createPublicationWithContributor(contributor, contributorCristinId, Role.CREATOR,
                                                           customer, topLevelCristinOrgId).copy()
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on non-degree resources belonging to the institution"
                              + " for not allowed operations")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UNPUBLISH", "UPDATE", "SUPPORT_REQUEST_CREATE"})
    void shouldDenyCuratorOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForCurator(), cristinId,
                                                null);
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(operation));
    }

    @Test
    void isCuratorOnPublicationShouldReturnTrueWhenCuratorIsAssociatedWithPublication()
        throws JsonProcessingException, UnauthorizedException {
        var username = randomString();
        var institution = randomUri();
        var cristinId = randomUri();
        var requestInfo = createUserRequestInfo(username, institution, getAccessRightsForCurator(), randomUri(),
                                                cristinId);
        var publication = createNonDegreePublication(username, institution, cristinId);
        var permissionStrategy = PublicationPermissions.create(Resource.fromPublication(publication),
                                                               RequestUtil.createUserInstanceFromRequest(
                                                                          requestInfo, identityServiceClient));
        assertThat(permissionStrategy.isCuratorOnPublication(), is(equalTo(true)));
    }
    //endregion

    //region Degree publications
    @ParameterizedTest(name = "Should deny Curator {0} operation on degree resources belonging to the institution")
    @EnumSource(value = PublicationOperation.class)
    void shouldDenyNonRelatedCuratorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var customerId = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, customerId, getAccessRightsForCurator(), cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, customerId);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on degree resources belonging to the institution "
                              + "with MANAGE_DEGREE access rights")
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE,
        names = {"UPDATE", "UNPUBLISH", "SUPPORT_REQUEST_CREATE", "PARTIAL_UPDATE"})
    void shouldAllowCuratorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution).copy()
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on degree resources with no matching resource owner "
                              + "affiliation or curating institution")
    @EnumSource(value = PublicationOperation.class)
    void shouldDenyNotRelatedCuratorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var requestInfo = createUserRequestInfo(randomString(), randomUri(), getAccessRightsForThesisCurator(),
                                                randomUri(), randomUri());

        var publication = createDegreePublicationWithContributor(randomString(), randomUri(), Role.CREATOR,
                                                                 randomUri(), randomUri());

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on degree resources with matching resource owner "
                              + "affiliation")
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE,
        names = {"UPDATE", "UNPUBLISH", "SUPPORT_REQUEST_CREATE", "PARTIAL_UPDATE"})
    void shouldAllowCuratorOnDegreeWithResourceOwnerAffiliation(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var cristinTopLevelId = randomUri();

        var requestInfo = createUserRequestInfo(randomString(), randomUri(), getAccessRightsForThesisCurator(),
                                                randomUri(), cristinTopLevelId);

        var publication = createDegreePhd(randomString(), randomUri(), cristinTopLevelId);
        setFileToPendingOpenFiles(publication);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @Test
    void shouldAllowCuratorToPerformApproveFilesWhenDegreeAndManageDegreeAccessRightIsPresent()
        throws JsonProcessingException, UnauthorizedException {
        var cristinTopLevelId = randomUri();

        var requestInfo = createUserRequestInfo(randomString(), randomUri(), List.of(AccessRight.MANAGE_DEGREE),
                                                randomUri(), cristinTopLevelId);

        var publication = createDegreePhd(randomString(), randomUri(), cristinTopLevelId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(PublicationOperation.APPROVE_FILES));
    }

    @Test
    void shouldNotAllowCuratorToPerformApproveFilesOnDegreesWhenMissingManageDegreeAccess()
        throws JsonProcessingException, UnauthorizedException {
        var cristinTopLevelId = randomUri();

        var requestInfo = createUserRequestInfo(randomString(), randomUri(), List.of(AccessRight.MANAGE_PUBLISHING_REQUESTS),
                                                randomUri(), cristinTopLevelId);

        var publication = createDegreePhd(randomString(), randomUri(), cristinTopLevelId);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(PublicationOperation.APPROVE_FILES));
    }
    //endregion
}

package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.Role;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class CuratorPermissionStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should allow Curator {0} operation on non-degree resources belonging to the "
                              + "institution based on publication owner")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE", "TERMINATE"})
    void shouldAllowCuratorOnNonDegreeBasedOnOwner(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createNonDegreePublication(resourceOwner, institution).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRights(), cristinId,
                                                publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on non-degree resources belonging to the "
                              + "institution based on contributors")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE", "TERMINATE"})
    void shouldAllowCuratorOnNonDegreeBasedOnContributors(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var usersTopCristinOrg = uriFromTestCase(TEST_ORG_NTNU_ROOT);
        var institution = uriFromTestCase(TEST_ORG_NTNU_DEPARTMENT_OF_LANGUAGES);
        var contributor = randomString();
        var contributorCristinId = randomUri();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, randomUri(), getCuratorAccessRights(), cristinId, usersTopCristinOrg);
        var publication = createPublicationWithContributor(contributor, contributorCristinId, Role.CREATOR,
                                                           institution, randomUri()).copy()
                              .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                              .build();
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on non-degree resources belonging to the institution"
                              + " for not allowed operations")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UNPUBLISH", "UPDATE"})
    void shouldDenyCuratorOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRights(), cristinId, null);
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance, uriRetriever)
                                   .allowsAction(operation));
    }

    @Test
    void shouldDenyAccessRightForCuratorToUnpublishDegreePublicationForDifferentInstitution()
        throws JsonProcessingException, UnauthorizedException {
        var curatorName = randomString();
        var resourceOwner = randomString();
        var institution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorName,
                                                institution,
                                                getCuratorWithPublishDegreeAccessRight(),
                                                cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, RequestUtil.createUserInstanceFromRequest(
                                       requestInfo, identityServiceClient), uriRetriever)
                                   .allowsAction(UNPUBLISH));
    }


    @Test
    void shouldDenyCuratorPermissionToUnpublishPublicationWhenPublicationIsFromAnotherInstitution()
        throws JsonProcessingException, UnauthorizedException {

        var curatorName = randomString();
        var curatorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceOwnerInstitution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorName, curatorInstitution, getCuratorAccessRights(), cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, resourceOwnerInstitution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, RequestUtil.createUserInstanceFromRequest(
                                       requestInfo, identityServiceClient), uriRetriever)
                                   .allowsAction(UNPUBLISH));
    }
}

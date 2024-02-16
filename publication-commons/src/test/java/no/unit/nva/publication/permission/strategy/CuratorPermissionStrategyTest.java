package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.Role;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class CuratorPermissionStrategyTest extends PublicationPermissionStrategyTest {

    //region Non-degree publications
    @ParameterizedTest(name = "Should allow Curator {0} operation on non-degree resources belonging to the "
                              + "institution based on publication owner")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE"}) // and terminate
    //and terminate
    void shouldAllowCuratorOnNonDegreeBasedOnOwner(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRights(), cristinId);
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on non-degree resources belonging to the "
                              + "institution based on contributors")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE"}) // and terminate
        //and terminate
    void shouldAllowCuratorOnNonDegreeBasedOnContributors(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var contributor = randomString();
        var contributorCristinId = randomUri();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRights(), cristinId);
        var publication = createPublicationWithContributor(contributor, contributorCristinId, Role.CREATOR,
                                                           institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Curator {0} operation on non-degree resources belonging to the institution"
                              + " for not allowed operations")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"REPUBLISH", "UNPUBLISH", "UPDATE"})
    void shouldDenyCuratorOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRights(), cristinId);
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }
    //endregion

    //region Degree publications
    @ParameterizedTest(name = "Should deny Curator {0} operation on degree resources belonging to the institution")
    @EnumSource(value = PublicationOperation.class)
    void shouldDenyCuratorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRights(), cristinId);
        var publication = createDegreePhd(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on degree resources belonging to the institution "
                              + "with MANAGE_DEGREE access rights")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE"}) // and terminate
    void shouldAllowCuratorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getCuratorAccessRightsWithDegree(),
                                                cristinId);
        var publication = createDegreePhd(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    //endregion
}

package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.testing.associatedartifacts.PublishedFileGenerator;
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
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE", "TERMINATE"})
    void shouldAllowCuratorOnNonDegreeBasedOnOwner(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createNonDegreePublication(resourceOwner, institution).copy()
                              .withAssociatedArtifacts(List.of(PublishedFileGenerator.random().toUnpublishedFile()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForCurator(), cristinId,
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

        var requestInfo = createUserRequestInfo(curatorUsername, randomUri(), getAccessRightsForCurator(), cristinId,
                                                usersTopCristinOrg);
        var publication = createPublicationWithContributor(contributor, contributorCristinId, Role.CREATOR,
                                                           institution, randomUri()).copy()
                              .withAssociatedArtifacts(List.of(PublishedFileGenerator.random().toUnpublishedFile()))
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

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForCurator(), cristinId,
                                                null);
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance, uriRetriever)
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

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForCurator(), cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance, uriRetriever)
                                   .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Curator {0} operation on degree resources belonging to the institution "
                              + "with MANAGE_DEGREE access rights")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE", "TERMINATE"})
    void shouldAllowCuratorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var curatorUsername = randomString();
        var cristinId = randomUri();

        var publication = createDegreePhd(resourceOwner, institution).copy()
                              .withAssociatedArtifacts(List.of(PublishedFileGenerator.random().toUnpublishedFile()))
                              .build();

        var requestInfo = createUserRequestInfo(curatorUsername, institution, getAccessRightsForThesisCurator(),
                                                cristinId, publication.getResourceOwner().getOwnerAffiliation());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(operation));
    }

    //endregion
}

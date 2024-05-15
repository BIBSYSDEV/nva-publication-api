package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class ResourceOwnerPermissionStrategyTest extends PublicationPermissionStrategyTest {

    //region Non-degree publications
    @ParameterizedTest(name = "Should allow ResourceOwner {0} operation on own published non-degree resource")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE", "TERMINATE", "TICKET_PUBLISH"})
    void shouldAllowResourceOwnerOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication = createNonDegreePublication(resourceOwner, institution);
        publication.setAssociatedArtifacts(new AssociatedArtifactList());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny ResourceOwner {0} operation on own published non-degree resource")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UPDATE", "UNPUBLISH"})
    void shoulDenyResourceOwnerOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                  .create(publication, userInstance)
                                  .allowsAction(operation));
    }
    //endregion

    //region Degree publications
    @ParameterizedTest(name = "Should deny ResourceOwner {0} operation on own published degree resource")
    @EnumSource(value = PublicationOperation.class)
    void shoulDenyResourceOwnerOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication = createDegreePhd(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance)
                                   .allowsAction(operation));
    }

    //endregion
}

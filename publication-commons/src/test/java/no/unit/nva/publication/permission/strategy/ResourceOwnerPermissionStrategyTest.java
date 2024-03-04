package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class ResourceOwnerPermissionStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should allow ResourceOwner {0} operation on own published non-degree resource")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE", "TERMINATE", "TICKET_PUBLISH"})
    void shouldAllowResourceOwnerOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institution, cristinId, randomUri());
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
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
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(operation));
    }

    @Test
    void shouldAllowResourceOwnerToUpdateDegreeInDraftStatus()
        throws JsonProcessingException, UnauthorizedException {

        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, editorInstitution, cristinId, randomUri());
        var publication = createDegreePhd(resourceOwner, editorInstitution, randomUri())
                              .copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .build();

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UPDATE));
    }

    @Test
    void shoulDenyMissingResourceOwnerToUpdateDegreeInDraftStatus()
        throws JsonProcessingException, UnauthorizedException {

        var editorInstitution = randomUri();
        var cristinId = randomUri();


        var requestInfo = createUserRequestInfo(randomString(), editorInstitution, cristinId, randomUri());
        var publication = createDegreePhd(randomString(), editorInstitution, randomUri())
                              .copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withResourceOwner(null)
                              .build();

        Assertions.assertFalse(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UPDATE));
    }
}

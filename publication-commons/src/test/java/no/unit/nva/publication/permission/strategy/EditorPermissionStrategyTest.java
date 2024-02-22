package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class EditorPermissionStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should allow editor {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE"})
    void shouldAllowEditorOnNonDegreeBasedOnOwner(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var editorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorUsername, institution, getEditorAccessRightsWithDegree(),
                                                cristinId, randomUri());
        var publication =
            createNonDegreePublication(resourceOwner, institution).copy()
                .withStatus(PublicationOperation.UNPUBLISH == operation ? PUBLISHED : UNPUBLISHED)
                .build();
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, userInstance, uriRetriever)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny editor {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UNPUBLISH", "UPDATE",
        "TICKET_PUBLISH", "TERMINATE"})
    void shouldDenyEditorOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var editorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorUsername, institution, getEditorAccessRightsWithDegree(),
                                                cristinId, randomUri());
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance, uriRetriever)
                                   .allowsAction(operation));
    }

    @Test
    void shouldGiveEditorPermissionToUnpublishPublicationWhenPublicationIsFromTheirInstitution()
        throws JsonProcessingException, UnauthorizedException {

        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getEditorAccessRightsWithDegree(),
                                                cristinId, randomUri());
        var publication = createPublication(resourceOwner, editorInstitution, cristinId);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldGiveEditorPermissionToUnpublishPublicationWhenPublicationIsFromAnotherInstitution()
        throws JsonProcessingException, UnauthorizedException {

        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceOwnerInstitution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getEditorAccessRightsWithDegree(),
                                                cristinId, resourceOwnerInstitution);
        var publication = createPublication(resourceOwner, resourceOwnerInstitution, cristinId);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }
}

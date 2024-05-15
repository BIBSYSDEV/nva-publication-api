package no.unit.nva.publication.permission.strategy;

import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;

class EditorPermissionStrategyTest extends PublicationPermissionStrategyTest {

    @ParameterizedTest(name = "Should allow editor {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"DELETE", "TICKET_PUBLISH"})
    void shouldAllowEditorOnNonDegreeBasedOnOwner(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var editorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorUsername, institution, getAccessRightsForEditor(),
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
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UNPUBLISH", "UPDATE", "TERMINATE"})
    void shouldDenyEditorOnNonDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var institution = randomUri();
        var resourceOwner = randomString();
        var editorUsername = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorUsername, institution, getAccessRightsForEditor(),
                                                cristinId, randomUri());
        var publication = createNonDegreePublication(resourceOwner, institution);
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, userInstance, uriRetriever)
                                   .allowsAction(operation));
    }

    public static Stream<Named<Publication>> generateCombinations() {
        var resourceOwner = randomString();
        return Stream.of(
            Named.of("Editor is publication owner and publication has files",
                     createPublication(resourceOwner,
                                       URI.create("https://editorInstitution"),
                                       URI.create("https://editorInstitution"))),
            Named.of("Editor is not publication owner and publication has files",
                     createPublication(resourceOwner,
                                       URI.create("https://editorInstitution"),
                                       URI.create("https://other"))),
            Named.of("Editor is publication owner and publication has no files",
                     createPublication(resourceOwner,
                                       URI.create("https://editorInstitution"),
                                       URI.create("https://editorInstitution"))
                         .copy().withAssociatedArtifacts(null).build()),
            Named.of("Editor is not publication owner and publication has no files",
                     createPublication(resourceOwner,
                                       URI.create("https://editorInstitution"),
                                       URI.create("https://other"))
                         .copy().withAssociatedArtifacts(null).build())
        );
    }

    @ParameterizedTest()
    @DisplayName("Should deny unpublish operation")
    @MethodSource("generateCombinations")
    void shouldAllowEditorOnPublicationWithAndWithoutFiles(Publication publication)
        throws JsonProcessingException, UnauthorizedException {

        var editorName = randomString();
        var cristinId = randomUri();
        var editorInstitution = URI.create("https://editorInstitution");

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getAccessRightsForEditor(),
                                                cristinId, randomUri());

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }
}

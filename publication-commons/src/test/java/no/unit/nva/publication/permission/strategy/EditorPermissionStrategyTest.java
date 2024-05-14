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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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

    public static Stream<Arguments> generateCombinations() {
        return Stream.of(
            Arguments.of(URI.create("https://editorInstitution"), URI.create("https://editorInstitution"), true, true),
            Arguments.of(URI.create("https://editorInstitution"), URI.create("https://other"), true, true),
            Arguments.of(URI.create("https://editorInstitution"), URI.create("https://editorInstitution"), false, true),
            Arguments.of(URI.create("https://editorInstitution"), URI.create("https://other"), false, true)
        );
    }

    @ParameterizedTest(name = "Should deny editor from {0} UNPUBLISH operation on resource from {1} with files {2}")
    @MethodSource("generateCombinations")
    void shouldAllowEditorOnPublicationWithAndWithoutFiles(URI editorInstitution, URI publicationCustomer,
                                                        boolean hasFiles,
                                      boolean expected)
        throws JsonProcessingException, UnauthorizedException {

        var editorName = randomString();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getAccessRightsForEditor(),
                                                cristinId, randomUri());
        Publication publication;
        if (hasFiles){
            publication = createPublication(resourceOwner, publicationCustomer, cristinId);
        } else {
            publication = createPublication(resourceOwner, publicationCustomer, cristinId)
                              .copy().withAssociatedArtifacts(null).build();
        }

        Assertions.assertEquals(expected, PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }
}

package no.unit.nva.publication.permissions.publication;

import static no.unit.nva.model.PublicationOperation.REPUBLISH;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Set;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.Role;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class EditorPermissionStrategyTest extends PublicationPermissionStrategyTest {


    @ParameterizedTest(name = "Should deny editor {0} operation on non-degree resources")
    @EnumSource(value = PublicationOperation.class, mode = Mode.EXCLUDE, names = {"UNPUBLISH", "UPDATE",
        "PARTIAL_UPDATE", "TERMINATE"})
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

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(operation));
    }

    @ParameterizedTest
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE,
        names = {"UNPUBLISH", "UPDATE", "PARTIAL_UPDATE"})
    void shouldAllowEditorUpdateOnDegreeWithResourceOwnerAffiliation(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var cristinTopLevelId = randomUri();

        var requestInfo = createUserRequestInfo(randomString(), randomUri(), getAccessRightsForEditor(),
                                                randomUri(), cristinTopLevelId);

        var publication = createDegreePhd(randomString(), randomUri(), cristinTopLevelId);
        setFileToPendingOpenFiles(publication);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should allow Editor {0} operation on degree resources with matching resource owner "
                              + "affiliation")
    @EnumSource(value = PublicationOperation.class, mode = Mode.INCLUDE, names = {"REPUBLISH", "TERMINATE"})
    void shouldAllowEditorOnDegreeWhenPublicationIsUnpublishedAndEditorIsFromCuratingInstitution(
        PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {

        var cristinTopLevelId = randomUri();

        var requestInfo = createUserRequestInfo(randomString(), randomUri(), getAccessRightsForEditor(),
                                                randomUri(), cristinTopLevelId);

        var publication = createDegreePhd(randomString(), randomUri(), cristinTopLevelId);
        setFileToPendingOpenFiles(publication);
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(cristinTopLevelId, Set.of())));
        publication.setStatus(UNPUBLISHED);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(operation));
    }

    @ParameterizedTest(name = "Should deny Editor {0} operation on degree resources with no matching resource owner "
                              + "affiliation or curating institution")
    @EnumSource(value = PublicationOperation.class)
    void shouldDenyNotRelatedEditorOnDegree(PublicationOperation operation)
        throws JsonProcessingException, UnauthorizedException {
        var requestInfo = createUserRequestInfo(randomString(), randomUri(), getAccessRightsForEditor(),
                                                randomUri(), randomUri());

        var publication = createDegreePublicationWithContributor(randomString(), randomUri(), Role.CREATOR,
                                                                 randomUri(), randomUri());

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), userInstance)
                                   .allowsAction(operation));
    }

    @DisplayName("Should allow editor to republish publication when curating institution matches editor institution")
    @Test
    void shouldAllowEditorToRepublishUnpublishedPublicationWithEditorInstitutionInCuratingInstitutions()
        throws JsonProcessingException, UnauthorizedException {

        var editorInstitution = randomUri();

        var requestInfo = createUserRequestInfo(randomString(), randomUri(), getAccessRightsForEditor(),
                                                randomUri(), editorInstitution);
        var publication = createNonDegreePublication(randomString(), randomUri()).copy()
                              .withCuratingInstitutions(Set.of(new CuratingInstitution(editorInstitution,
                                                                                       Set.of(randomUri()))))
                              .withStatus(UNPUBLISHED).build();
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

        Assertions.assertTrue(PublicationPermissions
                                  .create(Resource.fromPublication(publication), userInstance)
                                  .allowsAction(REPUBLISH));
    }
}

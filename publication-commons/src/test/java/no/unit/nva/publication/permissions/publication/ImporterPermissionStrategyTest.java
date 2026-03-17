package no.unit.nva.publication.permissions.publication;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class ImporterPermissionStrategyTest extends PublicationPermissionStrategyTest {

  private static List<AccessRight> getAccessRightsForImporter() {
    return List.of(AccessRight.MANAGE_IMPORT, AccessRight.MANAGE_RESOURCES_ALL);
  }

  @ParameterizedTest(
      name = "Should allow importer {0} operation on resources belonging to the institution")
  @EnumSource(
      value = PublicationOperation.class,
      mode = Mode.INCLUDE,
      names = {"ADD_ADDITIONAL_IDENTIFIERS"})
  void shouldAllowImporterToUpdateAdditionalIdentifiers(PublicationOperation operation)
      throws JsonProcessingException, UnauthorizedException {

    var institution = randomUri();
    var resourceOwner = randomString();
    var importerUsername = randomString();
    var cristinId = randomUri();

    var publication = createNonDegreePublication(resourceOwner, institution);
    var requestInfo =
        createUserRequestInfo(
            importerUsername,
            institution,
            getAccessRightsForImporter(),
            cristinId,
            publication.getResourceOwner().getOwnerAffiliation());
    var userInstance =
        RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

    Assertions.assertTrue(
        PublicationPermissions.create(Resource.fromPublication(publication), userInstance)
            .allowsAction(operation));
  }

  @ParameterizedTest(name = "Should deny importer {0} operation when user lacks MANAGE_IMPORT")
  @EnumSource(
      value = PublicationOperation.class,
      mode = Mode.INCLUDE,
      names = {"ADD_ADDITIONAL_IDENTIFIERS"})
  void shouldDenyUpdateAdditionalIdentifiersWithoutManageImport(PublicationOperation operation)
      throws JsonProcessingException, UnauthorizedException {

    var institution = randomUri();
    var resourceOwner = randomString();
    var username = randomString();
    var cristinId = randomUri();

    var publication = createNonDegreePublication(resourceOwner, institution);
    var requestInfo =
        createUserRequestInfo(
            username,
            institution,
            List.of(AccessRight.MANAGE_RESOURCES_STANDARD),
            cristinId,
            publication.getResourceOwner().getOwnerAffiliation());
    var userInstance =
        RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

    Assertions.assertFalse(
        PublicationPermissions.create(Resource.fromPublication(publication), userInstance)
            .allowsAction(operation));
  }

  @ParameterizedTest(name = "Should deny importer {0} for non-identifier operations")
  @EnumSource(
      value = PublicationOperation.class,
      mode = Mode.EXCLUDE,
      names = {"ADD_ADDITIONAL_IDENTIFIERS"})
  void shouldNotGrantOtherOperationsForImporter(PublicationOperation operation)
      throws JsonProcessingException, UnauthorizedException {

    var institution = randomUri();
    var resourceOwner = randomString();
    var importerUsername = randomString();
    var cristinId = randomUri();

    var publication = createNonDegreePublication(resourceOwner, institution);
    var requestInfo =
        createUserRequestInfo(
            importerUsername,
            institution,
            List.of(AccessRight.MANAGE_IMPORT),
            cristinId,
            publication.getResourceOwner().getOwnerAffiliation());
    var userInstance =
        RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

    Assertions.assertFalse(
        PublicationPermissions.create(Resource.fromPublication(publication), userInstance)
            .allowsAction(operation));
  }

  @ParameterizedTest(name = "Should allow importer {0} even when user is at different institution")
  @EnumSource(
      value = PublicationOperation.class,
      mode = Mode.INCLUDE,
      names = {"ADD_ADDITIONAL_IDENTIFIERS"})
  void shouldAllowImporterAtDifferentInstitution(PublicationOperation operation)
      throws JsonProcessingException, UnauthorizedException {

    var importerUsername = randomString();
    var cristinId = randomUri();

    var publication = createNonDegreePublication(randomString(), randomUri());
    var requestInfo =
        createUserRequestInfo(
            importerUsername, randomUri(), getAccessRightsForImporter(), cristinId, randomUri());
    var userInstance =
        RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);

    Assertions.assertTrue(
        PublicationPermissions.create(Resource.fromPublication(publication), userInstance)
            .allowsAction(operation));
  }
}

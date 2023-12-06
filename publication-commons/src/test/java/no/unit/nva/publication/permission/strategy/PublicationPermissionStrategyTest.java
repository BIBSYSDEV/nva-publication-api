package no.unit.nva.publication.permission.strategy;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PublicationPermissionStrategyTest {

    public static final String AT = "@";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String INJECT_NVA_USERNAME_CLAIM = "custom:nvaUsername";
    public static final String INJECT_COGNITO_GROUPS_CLAIM = "cognito:groups";
    public static final String INJECT_CRISTIN_ID_CLAIM = "custom:cristinId";

    @Test
    void shouldDenyPermissionToDeletePublicationWhenUserHasNoAccessRights() throws JsonProcessingException {
        var requestInfo = createRequestInfo(randomString(), randomUri());
        var publication = createPublication(randomString(), randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(requestInfo)
                                   .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldDenyPermissionToDeletePublicationWhenUserMissingBasicInfo() throws JsonProcessingException {
        var requestInfo = createRequestInfo(null, URI.create(""));
        var publication = createPublication(randomString(), randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(requestInfo)
                                   .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldGiveEditorPermissionToDeletePublicationWhenPublicationIsFromTheirInstitution()
        throws JsonProcessingException {

        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights());
        var publication = createPublication(resourceOwner, editorInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldGiveEditorPermissionToDeletePublicationWhenPublicationIsFromAnotherInstitution()
        throws JsonProcessingException {

        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceOwnerInstitution = randomUri();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights());
        var publication = createPublication(resourceOwner, resourceOwnerInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldGiveEditorPermissionToDeleteDegreeWhenDegreeIsFromTheirInstitution() throws JsonProcessingException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights());
        var publication = createDegreePhd(resourceOwner, editorInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldGiveEditorPermissionToDeleteDegreeWhenDegreeIsFromAnotherInstitution() throws JsonProcessingException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceInstitution = randomUri();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights());
        var publication = createDegreePhd(resourceOwner, resourceInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldDenyPermissionToDeleteDegreeWhenUserIsDoesNotHaveAccessRightPublishDegree()
        throws JsonProcessingException {
        var username = randomString();
        var institution = randomUri();
        var requestInfo = createRequestInfo(username, institution, getCuratorAccessRights());
        var publication = createDegreePhd(username, institution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(requestInfo)
                                   .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldAllowPermissionToDeleteDegreeWhenUserIsCuratorWithPermissionToPublishDegree()
        throws JsonProcessingException {
        var username = randomString();
        var institution = randomUri();
        var requestInfo = createRequestInfo(username,
                                            institution,
                                            getCuratorWithPublishDegreeAccessRight());
        var publication = createDegreePhd(username, institution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldGiveCuratorPermissionToDeleteDegreePublicationWhenUserHasPublishDegreeAccessRight()
        throws JsonProcessingException {

        var curatorName = randomString();
        var resourceOwner = randomString();
        var institution = randomUri();

        var requestInfo = createRequestInfo(curatorName,
                                            institution,
                                            getCuratorWithPublishDegreeAccessRight());
        var publication = createDegreePhd(resourceOwner, institution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldDenyAccessRightForCuratorToDeleteDegreePublicationForDifferentInstitution()
        throws JsonProcessingException {
        var curatorName = randomString();
        var resourceOwner = randomString();
        var institution = randomUri();
        var requestInfo = createRequestInfo(curatorName,
                                            institution,
                                            getCuratorWithPublishDegreeAccessRight());
        var publication = createDegreePhd(resourceOwner, randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(requestInfo)
                                   .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldDenyCuratorPermissionToDeletePublicationWhenPublicationIsFromAnotherInstitution()
        throws JsonProcessingException {

        var curatorName = randomString();
        var curatorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceOwnerInstitution = randomUri();

        var requestInfo = createRequestInfo(curatorName, curatorInstitution, getCuratorAccessRights());
        var publication = createDegreePhd(resourceOwner, resourceOwnerInstitution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(requestInfo)
                                   .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldGivePermissionToDeletePublicationWhenUserIsContributor() throws JsonProcessingException {
        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();

        var requestInfo = createRequestInfo(contributorName, contributorInstitutionId, contributorCristinId);
        var publication = createPublicationWithContributor(contributorName, contributorCristinId, Role.CREATOR);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldDenyPermissionToDeletePublicationWhenUserIsContributorButNotCreator() throws JsonProcessingException {
        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();

        var requestInfo = createRequestInfo(contributorName, contributorInstitutionId);
        var publication = createPublicationWithContributor(contributorName, contributorCristinId, null);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(requestInfo)
                                   .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldGivePermissionToDeletePublicationWhenUserIsResourceOwner() throws JsonProcessingException {
        var resourceOwner = randomString();
        var institutionId = randomUri();

        var requestInfo = createRequestInfo(resourceOwner, institutionId);
        var publication = createPublication(resourceOwner, institutionId);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermissionToUnpublish(publication));
    }

    @Test
    void shouldGivePermissionToOperateOnPublicationWhenEditor() throws JsonProcessingException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights());
        var publication = createPublication(resourceOwner, editorInstitution);

        Assertions.assertTrue(EditorPermissionStrategy
                                  .fromRequestInfo(requestInfo)
                                  .hasPermission(publication));
    }

    private static Function<AccessRight, String> getCognitoGroup(URI institutionId) {
        return accessRight -> accessRight.name() + AT + institutionId.toString();
    }

    private List<AccessRight> getCuratorWithPublishDegreeAccessRight() {
        var curatorAccessRight = getCuratorAccessRights();
        curatorAccessRight.add(AccessRight.PUBLISH_DEGREE);
        return curatorAccessRight;
    }

    private Publication createPublication(String resourceOwner, URI customer) {
        return randomPublication().copy()
                   .withResourceOwner(new ResourceOwner(new Username(resourceOwner), customer))
                   .withPublisher(new Organization.Builder().withId(customer).build())
                   .withStatus(PublicationStatus.PUBLISHED)
                   .build();
    }

    private Publication createDegreePhd(String resourceOwner, URI customer) {
        var publication = createPublication(resourceOwner, customer);

        var degreePhd = new DegreePhd(new MonographPages(), new PublicationDate());
        var reference = new Reference.Builder().withPublicationInstance(degreePhd).build();
        var entityDescription = publication.getEntityDescription().copy().withReference(reference).build();

        return publication.copy().withEntityDescription(entityDescription).build();
    }

    private Publication createPublicationWithContributor(String contributorName, URI contributorId,
                                                         Role contributorRole) {
        var publication = randomPublication();
        var identity = new Identity.Builder()
                           .withName(contributorName)
                           .withId(contributorId)
                           .build();
        var contributor = new Contributor.Builder()
                              .withIdentity(identity)
                              .withRole(new RoleType(contributorRole))
                              .build();
        var entityDescription = publication.getEntityDescription().copy()
                                    .withContributors(List.of(contributor))
                                    .build();

        return publication.copy().withEntityDescription(entityDescription).build();
    }

    private List<AccessRight> getEditorAccessRights() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.PUBLISH_DEGREE);
        accessRights.add(AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES);
        return accessRights;
    }

    private List<AccessRight> getCuratorAccessRights() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.EDIT_OWN_INSTITUTION_RESOURCES);
        return accessRights;
    }

    private RequestInfo createRequestInfo(String username, URI institutionId) throws JsonProcessingException {
        return createRequestInfo(username, institutionId, new ArrayList<>());
    }

    private RequestInfo createRequestInfo(String username, URI institutionId, URI cristinId)
        throws JsonProcessingException {
        return createRequestInfo(username, institutionId, new ArrayList<>(), cristinId);
    }

    private RequestInfo createRequestInfo(String username, URI institutionId, List<AccessRight> accessRights)
        throws JsonProcessingException {
        return createRequestInfo(username, institutionId, accessRights, null);
    }

    private RequestInfo createRequestInfo(String username, URI institutionId, List<AccessRight> accessRights,
                                          URI cristinId)
        throws JsonProcessingException {

        if (!accessRights.contains(AccessRight.USER)) {
            accessRights.add(AccessRight.USER);
        }

        var cognitoGroups = accessRights.stream().map(getCognitoGroup(institutionId)).toList();

        var claims = new HashMap<String, String>();
        claims.put(INJECT_COGNITO_GROUPS_CLAIM, String.join(",", cognitoGroups));

        if (nonNull(username)) {
            claims.put(INJECT_NVA_USERNAME_CLAIM, username);
        }

        if (nonNull(cristinId)) {
            claims.put(INJECT_CRISTIN_ID_CLAIM, cristinId.toString());
        }

        var requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(claims));

        return requestInfo;
    }

    private JsonNode getRequestContextForClaim(Map<String, String> claimKeyValuePairs) throws JsonProcessingException {
        Map<String, Map<String, Map<String, String>>> map = Map.of(
            AUTHORIZER, Map.of(
                CLAIMS, claimKeyValuePairs
            )
        );
        return dtoObjectMapper.readTree(dtoObjectMapper.writeValueAsString(map));
    }
}

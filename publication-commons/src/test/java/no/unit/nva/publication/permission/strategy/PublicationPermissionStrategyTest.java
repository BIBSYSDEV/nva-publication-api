package no.unit.nva.publication.permission.strategy;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.listPublicationInstanceTypes;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
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
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.RequestUtil;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicationPermissionStrategyTest {

    public static final String AT = "@";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String INJECT_NVA_USERNAME_CLAIM = "custom:nvaUsername";
    public static final String INJECT_COGNITO_GROUPS_CLAIM = "cognito:groups";
    public static final String INJECT_CRISTIN_ID_CLAIM = "custom:cristinId";
    private IdentityServiceClient identityServiceClient;

    @BeforeEach
    void setUp() throws NotFoundException {
        this.identityServiceClient = mock(IdentityServiceClient.class);
        when(this.identityServiceClient.getExternalClient(any())).thenReturn(
            new GetExternalClientResponse(randomString(), randomString(), randomUri(), randomUri()));
    }

    @Test
    void shouldDenyPermissionToDeletePublicationWhenUserHasNoAccessRights()
        throws JsonProcessingException, UnauthorizedException {
        var cristinId = randomUri();
        var requestInfo = createRequestInfo(randomString(), randomUri(), new ArrayList<>(), cristinId);
        var publication = createPublication(randomString(), randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                        requestInfo, identityServiceClient),
                                                    requestInfo.getAccessRights(),
                                                    requestInfo.getPersonCristinId())
                                   .hasPermissionToUnpublish());
    }

    @Test
    void shouldDenyPermissionToDeletePublicationWhenUserMissingBasicInfo()
        throws JsonProcessingException {
        var cristinId = randomUri();
        var requestInfo = createRequestInfo(null, URI.create(""), cristinId);
        var publication = createPublication(randomString(), randomUri());

        Assertions.assertThrows(UnauthorizedException.class, () -> PublicationPermissionStrategy
                                   .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                        requestInfo, identityServiceClient),
                                                    requestInfo.getAccessRights(),
                                                    requestInfo.getPersonCristinId())
                                   .hasPermissionToUnpublish());
    }

    @Test
    void shouldGiveEditorPermissionToDeletePublicationWhenPublicationIsFromTheirInstitution()
        throws JsonProcessingException, UnauthorizedException {

        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId);
        var publication = createPublication(resourceOwner, editorInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                       requestInfo, identityServiceClient),
                                                   requestInfo.getAccessRights(),
                                                   requestInfo.getPersonCristinId())
                                  .hasPermissionToUnpublish());
    }

    @Test
    void shouldGiveEditorPermissionToDeletePublicationWhenPublicationIsFromAnotherInstitution()
        throws JsonProcessingException, UnauthorizedException {

        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceOwnerInstitution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId);
        var publication = createPublication(resourceOwner, resourceOwnerInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                       requestInfo, identityServiceClient),
                                                   requestInfo.getAccessRights(),
                                                   requestInfo.getPersonCristinId())
                                  .hasPermissionToUnpublish());
    }

    @Test
    void shouldGiveEditorPermissionToDeleteDegreeWhenDegreeIsFromTheirInstitution()
        throws JsonProcessingException, UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId);
        var publication = createDegreePhd(resourceOwner, editorInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                       requestInfo, identityServiceClient),
                                                   requestInfo.getAccessRights(),
                                                   requestInfo.getPersonCristinId())
                                  .hasPermissionToUnpublish());
    }

    @Test
    void shouldGiveEditorPermissionToDeleteDegreeWhenDegreeIsFromAnotherInstitution()
        throws JsonProcessingException, UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceInstitution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId);
        var publication = createDegreePhd(resourceOwner, resourceInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                       requestInfo, identityServiceClient),
                                                   requestInfo.getAccessRights(),
                                                   requestInfo.getPersonCristinId())
                                  .hasPermissionToUnpublish());
    }

    @Test
    void shouldDenyEditorPermissionToDeleteDegreeWhenMissingManageDegree()
        throws JsonProcessingException, UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceInstitution = randomUri();
        var cristinId = randomUri();

        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_RESOURCES_ALL);

        var requestInfo = createRequestInfo(editorName, editorInstitution, accessRights, cristinId);
        var publication = createDegreePhd(resourceOwner, resourceInstitution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(publication,
                                                    RequestUtil.createAnyUserInstanceFromRequest(requestInfo,
                                                                                                 identityServiceClient),
                                                    accessRights,
                                                    requestInfo.getPersonCristinId())
                                   .hasPermissionToDelete());
    }

    @Test
    void shouldDenyPermissionToDeleteDegreeWhenUserIsDoesNotHaveAccessRightPublishDegree()
        throws JsonProcessingException, UnauthorizedException {
        var username = randomString();
        var institution = randomUri();
        var cristinId = randomUri();
        var requestInfo = createRequestInfo(username, institution, getCuratorAccessRights(), cristinId);
        var publication = createDegreePhd(username, institution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                        requestInfo, identityServiceClient),
                                                    requestInfo.getAccessRights(),
                                                    requestInfo.getPersonCristinId())
                                   .hasPermissionToUnpublish());
    }

    @Test
    void shouldAllowPermissionToDeleteDegreeWhenUserIsCuratorWithPermissionToPublishDegree()
        throws JsonProcessingException, UnauthorizedException {
        var username = randomString();
        var institution = randomUri();
        var cristinId = randomUri();
        var requestInfo = createRequestInfo(username,
                                            institution,
                                            getCuratorWithPublishDegreeAccessRight(),
                                            cristinId);
        var publication = createDegreePhd(username, institution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                       requestInfo, identityServiceClient),
                                                   requestInfo.getAccessRights(),
                                                   requestInfo.getPersonCristinId())
                                  .hasPermissionToUnpublish());
    }

    @Test
    void shouldGiveCuratorPermissionToDeleteDegreePublicationWhenUserHasPublishDegreeAccessRight()
        throws JsonProcessingException, UnauthorizedException {

        var curatorName = randomString();
        var resourceOwner = randomString();
        var institution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createRequestInfo(curatorName,
                                            institution,
                                            getCuratorWithPublishDegreeAccessRight(),
                                            cristinId);
        var publication = createDegreePhd(resourceOwner, institution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                       requestInfo, identityServiceClient),
                                                   requestInfo.getAccessRights(),
                                                   requestInfo.getPersonCristinId())
                                  .hasPermissionToUnpublish());
    }

    @Test
    void shouldDenyAccessRightForCuratorToDeleteDegreePublicationForDifferentInstitution()
        throws JsonProcessingException, UnauthorizedException {
        var curatorName = randomString();
        var resourceOwner = randomString();
        var institution = randomUri();
        var cristinId = randomUri();
        var requestInfo = createRequestInfo(curatorName,
                                            institution,
                                            getCuratorWithPublishDegreeAccessRight(),
                                            cristinId);
        var publication = createDegreePhd(resourceOwner, randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                        requestInfo, identityServiceClient),
                                                    requestInfo.getAccessRights(),
                                                    requestInfo.getPersonCristinId())
                                   .hasPermissionToUnpublish());
    }

    @Test
    void shouldDenyCuratorPermissionToDeletePublicationWhenPublicationIsFromAnotherInstitution()
        throws JsonProcessingException, UnauthorizedException {

        var curatorName = randomString();
        var curatorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceOwnerInstitution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createRequestInfo(curatorName, curatorInstitution, getCuratorAccessRights(), cristinId);
        var publication = createDegreePhd(resourceOwner, resourceOwnerInstitution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                        requestInfo, identityServiceClient),
                                                    requestInfo.getAccessRights(),
                                                    requestInfo.getPersonCristinId())
                                   .hasPermissionToUnpublish());
    }

    @Test
    void shouldGivePermissionToDeletePublicationWhenUserIsContributor()
        throws JsonProcessingException, UnauthorizedException {
        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();

        var requestInfo = createRequestInfo(contributorName, contributorInstitutionId, contributorCristinId);
        var publication = createPublicationWithContributor(contributorName, contributorCristinId, Role.CREATOR);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                       requestInfo, identityServiceClient),
                                                   requestInfo.getAccessRights(),
                                                   requestInfo.getPersonCristinId())
                                  .hasPermissionToUnpublish());
    }

    @Test
    void shouldDenyPermissionToDeletePublicationWhenUserIsContributorButNotCreator()
        throws JsonProcessingException, UnauthorizedException {
        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();

        var requestInfo = createRequestInfo(contributorName, contributorInstitutionId, contributorCristinId);
        var publication = createPublicationWithContributor(contributorName, contributorCristinId, null);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                        requestInfo, identityServiceClient),
                                                    requestInfo.getAccessRights(),
                                                    requestInfo.getPersonCristinId())
                                   .hasPermissionToUnpublish());
    }

    @Test
    void shouldGivePermissionToDeletePublicationWhenUserIsResourceOwner()
        throws JsonProcessingException, UnauthorizedException {
        var resourceOwner = randomString();
        var institutionId = randomUri();
        var cristinId = randomUri();

        var requestInfo = createRequestInfo(resourceOwner, institutionId, cristinId);
        var publication = createNonDegreePublication(resourceOwner, institutionId);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                       requestInfo, identityServiceClient),
                                                   requestInfo.getAccessRights(),
                                                   requestInfo.getPersonCristinId())
                                  .hasPermissionToUnpublish());
    }

    @Test
    void shouldGivePermissionToOperateOnPublicationWhenEditor() throws JsonProcessingException, UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId);
        var publication = createPublication(resourceOwner, editorInstitution);

        Assertions.assertTrue(
            PublicationPermissionStrategy.fromRequestInfo(publication, RequestUtil.createAnyUserInstanceFromRequest(
                                                              requestInfo, identityServiceClient),
                                                          requestInfo.getAccessRights(),
                                                          requestInfo.getPersonCristinId())
                .hasPermissionToUnpublish());
    }

    private static Function<AccessRight, String> getCognitoGroup(URI institutionId) {
        return accessRight -> accessRight.toPersistedString() + AT + institutionId.toString();
    }

    private List<AccessRight> getCuratorWithPublishDegreeAccessRight() {
        var curatorAccessRight = getCuratorAccessRights();
        curatorAccessRight.add(AccessRight.MANAGE_DEGREE);
        return curatorAccessRight;
    }

    private Publication createPublication(String resourceOwner, URI customer) {
        return randomPublication().copy()
                   .withResourceOwner(new ResourceOwner(new Username(resourceOwner), customer))
                   .withPublisher(new Organization.Builder().withId(customer).build())
                   .withStatus(PublicationStatus.PUBLISHED)
                   .build();
    }

    private Publication createNonDegreePublication(String resourceOwner, URI customer) {
        var publicationInstanceTypes = listPublicationInstanceTypes();
        var nonDegreePublicationInstances = publicationInstanceTypes.stream().filter(this::isNonDegreeClass).collect(
            Collectors.toList());
        return PublicationGenerator.randomPublication(randomElement(nonDegreePublicationInstances)).copy()
                   .withResourceOwner(new ResourceOwner(new Username(resourceOwner), customer))
                   .withPublisher(new Organization.Builder().withId(customer).build())
                   .withStatus(PublicationStatus.PUBLISHED)
                   .build();
    }

    private boolean isNonDegreeClass(Class<?> publicationInstance) {
        var listOfDegreeClasses = Set.of("DegreeMaster", "DegreeBachelor", "DegreePhd");
        return !listOfDegreeClasses.contains(publicationInstance.getSimpleName());
    }

    private Publication createDegreePhd(String resourceOwner, URI customer) {
        var publication = createPublication(resourceOwner, customer);

        var degreePhd = new DegreePhd(new MonographPages(), new PublicationDate(),
                                      Set.of(new UnconfirmedDocument(randomString())));
        var reference = new Reference.Builder().withPublicationInstance(degreePhd).build();
        var entityDescription = publication.getEntityDescription().copy().withReference(reference).build();

        return publication.copy().withEntityDescription(entityDescription).build();
    }

    private Publication createPublicationWithContributor(String contributorName, URI contributorId,
                                                         Role contributorRole) {
        var publicationInstanceTypes = listPublicationInstanceTypes();
        var nonDegreePublicationInstances = publicationInstanceTypes.stream()
                                                .filter(this::isNonDegreeClass)
                                                .collect(Collectors.toList());
        var publication = PublicationGenerator.randomPublication(randomElement(nonDegreePublicationInstances));
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
        accessRights.add(AccessRight.MANAGE_DEGREE);
        accessRights.add(AccessRight.MANAGE_RESOURCES_ALL);
        return accessRights;
    }

    private List<AccessRight> getCuratorAccessRights() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_RESOURCES_STANDARD);
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

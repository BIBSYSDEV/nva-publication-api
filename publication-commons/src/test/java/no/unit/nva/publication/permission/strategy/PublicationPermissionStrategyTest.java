package no.unit.nva.publication.permission.strategy;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationOperation.DELETE;
import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.utils.RdfUtils.APPLICATION_JSON;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.streamToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
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
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PublicationPermissionStrategyTest {

    public static final String AT = "@";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String INJECT_NVA_USERNAME_CLAIM = "custom:nvaUsername";
    public static final String INJECT_CUSTOMER_ID_CLAIM = "custom:customerId";
    public static final String INJECT_COGNITO_GROUPS_CLAIM = "cognito:groups";
    public static final String INJECT_CRISTIN_ID_CLAIM = "custom:cristinId";
    public static final String INJECT_TOP_ORG_CRISTIN_ID_CLAIM = "custom:topOrgCristinId";
    protected static final String TEST_ORG_NTNU_ROOT = "194.0.0.0";
    protected static final String TEST_ORG_NTNU_OFFICE_INTERNATIONAL = "194.14.62.0";
    protected static final String TEST_ORG_NTNU_DEPARTMENT_OF_LANGUAGES = "194.62.60.0";
    protected static final String TEST_ORG_SIKT_DEPARTMENT_OF_COMMUNICATION = "20754.6.0.0";
    IdentityServiceClient identityServiceClient;
    public static final ObjectMapper dtoObjectMapper = JsonUtils.dtoObjectMapper;
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";

    private static final URI EXTERNAL_CLIENT_CUSTOMER_URI = URI.create("https://example.com/external-client-org");

    protected UriRetriever uriRetriever;

    @BeforeEach
    void setUp() throws NotFoundException {
        this.identityServiceClient = mock(IdentityServiceClient.class);
        setupUriRetriever();

        when(this.identityServiceClient.getExternalClient(any())).thenReturn(
            new GetExternalClientResponse(randomString(), randomString(), EXTERNAL_CLIENT_CUSTOMER_URI, randomUri()));
    }

    private void setupUriRetriever() {
        this.uriRetriever = mock(UriRetriever.class);
        setupCristinResponse(TEST_ORG_NTNU_ROOT);
        setupCristinResponse(TEST_ORG_NTNU_OFFICE_INTERNATIONAL);
        setupCristinResponse(TEST_ORG_NTNU_DEPARTMENT_OF_LANGUAGES);
        setupCristinResponse(TEST_ORG_SIKT_DEPARTMENT_OF_COMMUNICATION);
    }

    protected URI uriFromTestCase(String testCase) {
        return URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/"+testCase);
    }

    private void setupCristinResponse(String testCase) {
        var content = streamToString(inputStreamFromResources("cristin-orgs/"+testCase+".json"));
        when(uriRetriever.getRawContent(eq(uriFromTestCase(testCase)), eq(APPLICATION_JSON)))
            .thenReturn(Optional.of(content));
    }

    @Test
    void shouldDenyPermissionToUnpublishPublicationWhenUserHasNoAccessRights()
        throws JsonProcessingException, UnauthorizedException {
        var cristinId = randomUri();
        var requestInfo = createUserRequestInfo(randomString(), randomUri(), new ArrayList<>(), cristinId, null);
        var publication = createPublication(randomString(), randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, RequestUtil.createUserInstanceFromRequest(
                                       requestInfo, identityServiceClient), uriRetriever)
                                   .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldDenyPermissionToUnpublishPublicationWhenUserMissingBasicInfo()
        throws JsonProcessingException {
        var cristinId = randomUri();
        var requestInfo = createUserRequestInfo(null, URI.create(""), cristinId);
        var publication = createPublication(randomString(), randomUri());

        Assertions.assertThrows(UnauthorizedException.class, () -> PublicationPermissionStrategy
                                                                       .create(publication,
                                                                               RequestUtil.createUserInstanceFromRequest(
                                                                                   requestInfo, identityServiceClient),
                                                                               uriRetriever)
                                                                       .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldAllowResourceOwnerToUpdateDegreeInDraftStatus()
        throws JsonProcessingException, UnauthorizedException {

        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, editorInstitution)
                              .copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .build();

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UPDATE));
    }

    @Test
    void shouldGiveEditorPermissionToUnpublishPublicationWhenPublicationIsFromTheirInstitution()
        throws JsonProcessingException, UnauthorizedException {

        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId,
                                                null);
        var publication = createPublication(resourceOwner, editorInstitution);

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

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId,
                                                null);
        var publication = createPublication(resourceOwner, resourceOwnerInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldGiveEditorPermissionToUnpublishDegreeWhenDegreeIsFromTheirInstitution()
        throws JsonProcessingException, UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, editorInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldGiveEditorPermissionToUnpublishDegreeWhenDegreeIsFromAnotherInstitution()
        throws JsonProcessingException, UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceInstitution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, resourceInstitution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
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

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, accessRights, cristinId, null);
        var publication = createDegreePhd(resourceOwner, resourceInstitution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication,
                                           RequestUtil.createUserInstanceFromRequest(requestInfo,
                                                                                     identityServiceClient),
                                           uriRetriever)
                                   .allowsAction(DELETE));
    }

    @Test
    void shouldDenyPermissionToUnpublishDegreeWhenUserIsDoesNotHaveAccessRightPublishDegree()
        throws JsonProcessingException, UnauthorizedException {
        var username = randomString();
        var institution = randomUri();
        var cristinId = randomUri();
        var requestInfo = createUserRequestInfo(username, institution, getCuratorAccessRights(), cristinId, null);
        var publication = createDegreePhd(username, institution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, RequestUtil.createUserInstanceFromRequest(
                                       requestInfo, identityServiceClient), uriRetriever)
                                   .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldAllowPermissionToUnpublishDegreeWhenUserIsCuratorWithPermissionToPublishDegree()
        throws JsonProcessingException, UnauthorizedException {
        var username = randomString();
        var institution = randomUri();
        var cristinId = randomUri();
        var requestInfo = createUserRequestInfo(username,
                                                institution,
                                                getCuratorWithPublishDegreeAccessRight(),
                                                cristinId,
                                                null);
        var publication = createDegreePhd(username, institution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldGiveCuratorPermissionToUnpublishDegreePublicationWhenUserHasPublishDegreeAccessRight()
        throws JsonProcessingException, UnauthorizedException {

        var curatorName = randomString();
        var resourceOwner = randomString();
        var institution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorName,
                                                institution,
                                                getCuratorWithPublishDegreeAccessRight(),
                                                cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, institution);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldDenyAccessRightForCuratorToUnpublishDegreePublicationForDifferentInstitution()
        throws JsonProcessingException, UnauthorizedException {
        var curatorName = randomString();
        var resourceOwner = randomString();
        var institution = randomUri();
        var cristinId = randomUri();
        var requestInfo = createUserRequestInfo(curatorName,
                                                institution,
                                                getCuratorWithPublishDegreeAccessRight(),
                                                cristinId,
                                                null);
        var publication = createDegreePhd(resourceOwner, randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, RequestUtil.createUserInstanceFromRequest(
                                       requestInfo, identityServiceClient), uriRetriever)
                                   .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldDenyCuratorPermissionToUnpublishPublicationWhenPublicationIsFromAnotherInstitution()
        throws JsonProcessingException, UnauthorizedException {

        var curatorName = randomString();
        var curatorInstitution = randomUri();
        var resourceOwner = randomString();
        var resourceOwnerInstitution = randomUri();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(curatorName, curatorInstitution, getCuratorAccessRights(), cristinId, null);
        var publication = createDegreePhd(resourceOwner, resourceOwnerInstitution);

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, RequestUtil.createUserInstanceFromRequest(
                                       requestInfo, identityServiceClient), uriRetriever)
                                   .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldGivePermissionToUnpublishPublicationWhenUserIsContributor()
        throws JsonProcessingException, UnauthorizedException {
        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();

        var requestInfo = createUserRequestInfo(contributorName, contributorInstitutionId, contributorCristinId);
        var publication = createPublicationWithContributor(contributorName, contributorCristinId, Role.CREATOR,
                                                           randomUri());

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }

    @Disabled("Not valid anymore?")
    @Test
    void shouldDenyPermissionToUnpublishPublicationWhenUserIsContributorButNotCreator()
        throws JsonProcessingException, UnauthorizedException {
        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();

        var requestInfo = createUserRequestInfo(contributorName, contributorInstitutionId, contributorCristinId);
        var publication = createPublicationWithContributor(contributorName, contributorCristinId, null, randomUri());

        Assertions.assertFalse(PublicationPermissionStrategy
                                   .create(publication, RequestUtil.createUserInstanceFromRequest(
                                       requestInfo, identityServiceClient), uriRetriever)
                                   .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldGivePermissionToUnpublishPublicationWhenUserIsResourceOwner()
        throws JsonProcessingException, UnauthorizedException {
        var resourceOwner = randomString();
        var institutionId = randomUri();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(resourceOwner, institutionId, cristinId);
        var publication = createNonDegreePublication(resourceOwner, institutionId);

        Assertions.assertTrue(PublicationPermissionStrategy
                                  .create(publication, RequestUtil.createUserInstanceFromRequest(
                                      requestInfo, identityServiceClient), uriRetriever)
                                  .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldGivePermissionToOperateOnPublicationWhenEditor() throws JsonProcessingException, UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, getEditorAccessRights(), cristinId, null);
        var publication = createPublication(resourceOwner, editorInstitution);

        Assertions.assertTrue(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient), uriRetriever)
                .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldGivePermissionToEditPublicationWhenTrustedClient()
        throws JsonProcessingException, UnauthorizedException {
        var publication = createNonDegreePublication(randomString(), EXTERNAL_CLIENT_CUSTOMER_URI);
        var requestInfo = createThirdPartyRequestInfo(getEditorAccessRights());

        Assertions.assertTrue(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient), uriRetriever)
                .allowsAction(UPDATE));
    }

    @Test
    void shouldDenyTrustedClientEditPublicationWithoutMatchingCustomer()
        throws JsonProcessingException, UnauthorizedException {
        var publication = createPublication(randomString(), randomUri());
        var requestInfo = createThirdPartyRequestInfo(getEditorAccessRights());

        Assertions.assertFalse(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient), uriRetriever)
                .allowsAction(UPDATE));
    }

    @Test
    void shouldDenyTrustedClientEditPublicationWithMissingPublisher()
        throws JsonProcessingException, UnauthorizedException {
        var publication = createPublication(randomString(), randomUri());
        publication.setPublisher(null);
        var requestInfo = createThirdPartyRequestInfo(getEditorAccessRights());

        Assertions.assertFalse(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient), uriRetriever)
                .allowsAction(UPDATE));
    }

    @Test
    void shouldThrowUnauthorizedExceptionFromAuthorize() throws JsonProcessingException, UnauthorizedException {
        var publication = createDegreePhd(randomString(), randomUri());
        var requestInfo = createThirdPartyRequestInfo(getCuratorAccessRights());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var strategy = PublicationPermissionStrategy.create(publication, userInstance, uriRetriever);

        Assertions.assertThrows(UnauthorizedException.class, () -> strategy.authorize(UPDATE));
    }

    @Test
    void getAllAllowedOperationsShouldReturnNothingWhenUserHasNoAccessRights() throws JsonProcessingException,
                                                                                      UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var requestInfo = createUserRequestInfo(editorName, editorInstitution, List.of(), cristinId, null);
        var publication = createPublication(resourceOwner, editorInstitution);

        assertThat(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient), uriRetriever)
                .getAllAllowedActions(), is(empty()));
    }

    @Test
    void getAllAllowedOperationsShouldReturnUpdateUnpublishWhenUserHasAllAccessRights() throws JsonProcessingException,
                                                                                               UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var cristinId = randomUri();

        var allAccessRights = List.of(AccessRight.values());
        var requestInfo = createUserRequestInfo(editorName, editorInstitution, allAccessRights, cristinId, null);
        var publication = createPublication(resourceOwner, editorInstitution);

        assertThat(
            PublicationPermissionStrategy.create(publication, RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient), uriRetriever)
                .getAllAllowedActions(), containsInAnyOrder(UPDATE, UNPUBLISH));
    }

    @Test
    void shouldLogWhenAuthorizingAnOperation() throws JsonProcessingException, UnauthorizedException {
        var appender = LogUtils.getTestingAppenderForRootLogger();

        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();

        var requestInfo = createUserRequestInfo(contributorName, contributorInstitutionId, contributorCristinId);
        var publication = createPublicationWithContributor(contributorName, contributorCristinId, Role.CREATOR,
                                                           randomUri());

        PublicationPermissionStrategy
            .create(publication, RequestUtil.createUserInstanceFromRequest(
                requestInfo, identityServiceClient), uriRetriever)
            .authorize(UNPUBLISH);
        assertThat(appender.getMessages(), containsString(contributorName));
        assertThat(appender.getMessages(), containsString(publication.getIdentifier().toString()));
        assertThat(appender.getMessages(), containsString("ContributorPermissionStrategy"));
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
                   .withStatus(PUBLISHED)
                   .build();
    }

    Publication createNonDegreePublication(String resourceOwner, URI customer) {
        return PublicationGenerator.randomPublicationNonDegree().copy()
                   .withResourceOwner(new ResourceOwner(new Username(resourceOwner), customer))
                   .withPublisher(new Organization.Builder().withId(customer).build())
                   .withStatus(PUBLISHED)
                   .build();
    }

    Publication createDegreePhd(String resourceOwner, URI customer) {
        var publication = createPublication(resourceOwner, customer);

        var degreePhd = new DegreePhd(new MonographPages(), new PublicationDate(),
                                      Set.of(new UnconfirmedDocument(randomString())));
        var reference = new Reference.Builder().withPublicationInstance(degreePhd).build();
        var entityDescription = publication.getEntityDescription().copy().withReference(reference).build();

        return publication.copy().withEntityDescription(entityDescription).build();
    }

    protected Publication createPublicationWithContributor(String contributorName, URI contributorId,
                                                         Role contributorRole, URI institutionId) {
        var publication = PublicationGenerator.randomPublicationNonDegree();
        var identity = new Identity.Builder()
                           .withName(contributorName)
                           .withId(contributorId)
                           .build();
        var contributor = new Contributor.Builder()
                              .withIdentity(identity)
                              .withAffiliations(List.of(new Organization.Builder().withId(institutionId).build()))
                              .withRole(new RoleType(contributorRole))
                              .build();
        var entityDescription = publication.getEntityDescription().copy()
                                    .withContributors(List.of(contributor))
                                    .build();

        return publication.copy().withEntityDescription(entityDescription)
                   .withStatus(PUBLISHED).build();
    }

    protected List<AccessRight> getEditorAccessRights() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_DEGREE);
        accessRights.add(AccessRight.MANAGE_RESOURCES_ALL);
        return accessRights;
    }

    protected List<AccessRight> getCuratorAccessRights() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_RESOURCES_STANDARD);
        return accessRights;
    }

    protected List<AccessRight> getCuratorAccessRightsWithDegree() {
        var accessRights = new ArrayList<AccessRight>();
        accessRights.add(AccessRight.MANAGE_DEGREE);
        accessRights.add(AccessRight.MANAGE_RESOURCES_STANDARD);
        return accessRights;
    }

    RequestInfo createUserRequestInfo(String username, URI institutionId, URI cristinId)
        throws JsonProcessingException {
        return createUserRequestInfo(username, institutionId, new ArrayList<>(), cristinId, null);
    }

    RequestInfo createUserRequestInfo(String username, URI institutionId, List<AccessRight> accessRights,
                                      URI personCristinId, URI topLevelCristinOrgId)
        throws JsonProcessingException {

        var cognitoGroups = accessRights.stream().map(getCognitoGroup(institutionId)).toList();

        var claims = new HashMap<String, String>();
        claims.put(INJECT_CUSTOMER_ID_CLAIM, institutionId.toString());

        claims.put(INJECT_COGNITO_GROUPS_CLAIM, String.join(",", cognitoGroups));

        if (nonNull(username)) {
            claims.put(INJECT_NVA_USERNAME_CLAIM, username);
        }

        if (nonNull(personCristinId)) {
            claims.put(INJECT_CRISTIN_ID_CLAIM, personCristinId.toString());
        }

        if (nonNull(topLevelCristinOrgId)) {
            claims.put(INJECT_TOP_ORG_CRISTIN_ID_CLAIM, topLevelCristinOrgId.toString());
        }

        var requestInfo = new RequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(claims));

        return requestInfo;
    }

    private RequestInfo createThirdPartyRequestInfo(List<AccessRight> accessRights)
        throws JsonProcessingException {

        var cognitoGroups = accessRights.stream().map(getCognitoGroup(
            PublicationPermissionStrategyTest.EXTERNAL_CLIENT_CUSTOMER_URI)).toList();

        var claims = new HashMap<String, String>();
        claims.put(INJECT_COGNITO_GROUPS_CLAIM, String.join(",", cognitoGroups));
        claims.put(ISS_CLAIM, EXTERNAL_ISSUER);
        claims.put(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID);

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

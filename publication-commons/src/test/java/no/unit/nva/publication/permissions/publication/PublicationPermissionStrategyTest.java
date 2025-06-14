package no.unit.nva.publication.permissions.publication;

import static java.util.Objects.nonNull;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationOperation.UPDATE_FILES;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.fromInstanceClassesExcluding;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomFinalizedFiles;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomNonFinalizedFiles;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.getAccessRightsForCurator;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.SCOPE_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_TOKEN = "Bearer token";
    public static final String BACKEND_SCOPE = "https://api.nva.unit.no/scopes/backend";
    private static final String SCOPES_THIRD_PARTY_PUBLICATION_READ = "https://api.nva.unit.no/scopes/third-party/publication-read";
    IdentityServiceClient identityServiceClient;
    public static final ObjectMapper dtoObjectMapper = JsonUtils.dtoObjectMapper;
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";

    protected static final URI EXTERNAL_CLIENT_CUSTOMER_URI = URI.create("https://example.com/external-client-org");

    @BeforeEach
    void setUp() throws NotFoundException {
        this.identityServiceClient = mock(IdentityServiceClient.class);

        when(this.identityServiceClient.getExternalClient(any())).thenReturn(
            new GetExternalClientResponse(randomString(), randomString(), EXTERNAL_CLIENT_CUSTOMER_URI, randomUri()));
    }

    @Test
    void shouldDenyPermissionToUnpublishPublicationWhenUserHasNoAccessRights()
        throws JsonProcessingException, UnauthorizedException {
        var cristinId = randomUri();
        var requestInfo = createUserRequestInfo(randomString(), randomUri(), new ArrayList<>(), cristinId, null);
        var publication = createPublication(randomString(), randomUri(), randomUri());

        Assertions.assertFalse(PublicationPermissions
                                   .create(Resource.fromPublication(publication), RequestUtil.createUserInstanceFromRequest(
                                       requestInfo, identityServiceClient))
                                   .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldDenyPermissionToUnpublishPublicationWhenUserMissingBasicInfo()
        throws JsonProcessingException {
        var cristinId = randomUri();
        var requestInfo = createUserRequestInfo(null, URI.create(""), cristinId, randomUri());
        var publication = createPublication(randomString(), randomUri(), randomUri());

        Assertions.assertThrows(UnauthorizedException.class, () ->
                                                                 PublicationPermissions
                                                                     .create(Resource.fromPublication(publication),
                                                                             RequestUtil.createUserInstanceFromRequest(
                                                                                 requestInfo, identityServiceClient))
                                                                     .allowsAction(UNPUBLISH));
    }

    @Test
    void shouldThrowUnauthorizedExceptionFromAuthorize() throws JsonProcessingException, UnauthorizedException {
        var publication = createDegreePhd(randomString(), randomUri());
        var requestInfo = createThirdPartyRequestInfo(getAccessRightsForCurator());
        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var strategy = PublicationPermissions.create(Resource.fromPublication(publication), userInstance);

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
        var publication = createPublication(resourceOwner, editorInstitution, randomUri());

        assertThat(
            PublicationPermissions.create(Resource.fromPublication(publication), RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient))
                .getAllAllowedActions(), is(empty()));
    }

    @Test
    void getAllAllowedOperationsShouldReturnUpdateUnpublishWhenUserHasAllAccessRights() throws JsonProcessingException,
                                                                                               UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var personCristinId = randomUri();
        var topLevelCristinOrgId = randomUri();

        var allAccessRights = List.of(AccessRight.values());
        var requestInfo = createUserRequestInfo(editorName, editorInstitution, allAccessRights, personCristinId, topLevelCristinOrgId);
        var publication = createPublication(resourceOwner, editorInstitution, topLevelCristinOrgId);

        assertThat(
            PublicationPermissions.create(Resource.fromPublication(publication), RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient))
                .getAllAllowedActions(), hasItems(UPDATE, UNPUBLISH, UPDATE_FILES));
    }

    @Test
    void shouldLogWhenAuthorizingAnOperation() throws JsonProcessingException, UnauthorizedException {
        var appender = LogUtils.getTestingAppenderForRootLogger();

        var contributorName = randomString();
        var contributorCristinId = randomUri();
        var contributorInstitutionId = randomUri();

        var requestInfo = createUserRequestInfo(contributorName, contributorInstitutionId, contributorCristinId,
                                                randomUri());
        var publication = createPublicationWithContributor(contributorName, contributorCristinId, Role.CREATOR,
                                                           randomUri(), randomUri());

        PublicationPermissions
            .create(Resource.fromPublication(publication), RequestUtil.createUserInstanceFromRequest(
                requestInfo, identityServiceClient))
            .authorize(UPDATE);
        assertThat(appender.getMessages(), containsString(contributorName));
        assertThat(appender.getMessages(), containsString(publication.getIdentifier().toString()));
        assertThat(appender.getMessages(), containsString("ContributorGrantStrategy"));
    }

    @Test
    void isPublishingCuratorOnPublicationShouldReturnTrueWhenUserHasManagePublicationFilesAccessRight()
        throws JsonProcessingException, UnauthorizedException {
        var editorName = randomString();
        var editorInstitution = randomUri();
        var resourceOwner = randomString();
        var personCristinId = randomUri();
        var topLevelCristinOrgId = randomUri();

        var allAccessRights = List.of(AccessRight.values());
        var requestInfo = createUserRequestInfo(editorName, editorInstitution, allAccessRights, personCristinId, topLevelCristinOrgId);
        var publication = createPublication(resourceOwner, editorInstitution, topLevelCristinOrgId);

        assertTrue(
            PublicationPermissions.create(Resource.fromPublication(publication), RequestUtil.createUserInstanceFromRequest(
                    requestInfo, identityServiceClient))
                .isPublishingCuratorOnPublication());
    }

    private static Function<AccessRight, String> getCognitoGroup(URI institutionId) {
        return accessRight -> accessRight.toPersistedString() + AT + institutionId.toString();
    }

    static Publication createPublication(Class<?> instanceTypeClass,
                                         String resourceOwner,
                                         URI customer,
                                         URI cristinId) {
        return randomPublication(instanceTypeClass).copy()
                   .withResourceOwner(new ResourceOwner(new Username(resourceOwner), cristinId))
                   .withPublisher(new Organization.Builder().withId(customer).build())
                   .withStatus(PUBLISHED)
                   .build();
    }

    static Publication createPublication(String resourceOwner, URI customer, URI topLevelCristinOrgId) {
        return randomPublication().copy()
                   .withResourceOwner(new ResourceOwner(new Username(resourceOwner), topLevelCristinOrgId))
                   .withPublisher(new Organization.Builder().withId(customer).build())
                   .withStatus(PUBLISHED)
                   .build();
    }

    static Publication createPublicationWithFinalizedFiles(Class<?> instanceTypeClass,
                                                           String resourceOwner,
                                                           URI customer,
                                                           URI cristinId) {
        return createPublication(instanceTypeClass, resourceOwner, customer, cristinId).copy()
                   .withAssociatedArtifacts(randomFinalizedFiles())
                   .build();
    }

    static Publication createPublicationWithoutFinalizedFiles(Class<?> instanceTypeClass,
                                                              String resourceOwner,
                                                              URI customer,
                                                              URI cristinId) {
        return createPublication(instanceTypeClass, resourceOwner, customer, cristinId).copy()
                   .withAssociatedArtifacts(randomNonFinalizedFiles())
                   .build();
    }

    static void setFileToPendingOpenFiles(Publication publication) {
        var list = publication.getAssociatedArtifacts()
                       .stream()
                       .filter(File.class::isInstance)
                       .map(File.class::cast)
                       .map(File::toPendingOpenFile)
                       .collect(Collectors.toCollection(() -> new ArrayList<AssociatedArtifact>()));
        publication.setAssociatedArtifacts(new AssociatedArtifactList(list));
    }

    Publication createNonDegreePublication(String resourceOwner, URI customer, URI ownerAffiliation) {
        return fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES)
                   .copy()
                   .withResourceOwner(new ResourceOwner(new Username(resourceOwner), ownerAffiliation))
                   .withPublisher(new Organization.Builder().withId(customer).build())
                   .withStatus(PUBLISHED)
                   .build();
    }

    Publication createNonDegreePublication(String resourceOwner, URI customer) {
        return createNonDegreePublication(resourceOwner, customer, randomUri());
    }

    Publication createDegreePhd(String resourceOwner, URI customer) {
        var publication = createPublication(resourceOwner, customer, randomUri());

        var degreePhd = new DegreePhd(new MonographPages(), new PublicationDate(),
                                      Set.of(new UnconfirmedDocument(randomString(), randomInteger())));
        var reference = new Reference.Builder().withPublicationInstance(degreePhd).build();
        var entityDescription = publication.getEntityDescription().copy().withReference(reference).build();

        return publication.copy().withEntityDescription(entityDescription).build();
    }

    Publication createDegreePhd(String resourceOwner, URI customerId, URI ownerAffiliation) {
        var publication = createPublication(resourceOwner, customerId, ownerAffiliation);

        var degreePhd = new DegreePhd(new MonographPages(), new PublicationDate(),
                                      Set.of(new UnconfirmedDocument(randomString(), randomInteger())));
        var reference = new Reference.Builder().withPublicationInstance(degreePhd).build();
        var entityDescription = publication.getEntityDescription().copy().withReference(reference).build();

        return publication.copy().withEntityDescription(entityDescription).build();
    }

    protected Publication createPublicationWithContributor(String contributorName, URI contributorId,
                                                           Role contributorRole, URI customerId,
                                                           URI topLevelCristinOrgId) {
        var publication = fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES);
        publication.setPublisher(new Organization.Builder().withId(customerId).build());
        var identity = new Identity.Builder()
                           .withName(contributorName)
                           .withId(contributorId)
                           .withVerificationStatus(ContributorVerificationStatus.VERIFIED)
                           .build();
        var contributor = new Contributor.Builder()
                              .withIdentity(identity)
                              .withAffiliations(List.of(new Organization.Builder().withId(topLevelCristinOrgId).build()))
                              .withRole(new RoleType(contributorRole))
                              .build();
        var entityDescription = publication.getEntityDescription().copy()
                                    .withContributors(List.of(contributor))
                                    .build();

        return publication.copy().withEntityDescription(entityDescription)
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withCuratingInstitutions(Set.of(new CuratingInstitution(topLevelCristinOrgId, Set.of(randomUri()))))
                   .withStatus(PUBLISHED).build();
    }

    public Publication createDegreePublicationWithContributor(String contributorName, URI contributorId,
                                                       Role contributorRole, URI customerId,
                                                       URI topLevelCristinOrgId) {
        var publication = createPublicationWithContributor(contributorName, contributorId, contributorRole,
                                         customerId, topLevelCristinOrgId);
        publication.getEntityDescription()
            .getReference()
            .setPublicationInstance(new DegreePhd(new MonographPages(), new PublicationDate(),
                                                  Set.of(new UnconfirmedDocument(randomString(), randomInteger()))));

        return publication;
    }

    RequestInfo createUserRequestInfo(String username, URI institutionId, URI cristinId, URI topLevelCristinOrgId)
        throws JsonProcessingException {
        return createUserRequestInfo(username, institutionId, new ArrayList<>(), cristinId, topLevelCristinOrgId);
    }

    RequestInfo createUserRequestInfo(String username, URI customerId, List<AccessRight> accessRights,
                                      URI personCristinId, URI topLevelCristinOrgId)
        throws JsonProcessingException {

        var cognitoGroups = accessRights.stream().map(getCognitoGroup(customerId)).toList();

        var claims = new HashMap<String, String>();
        claims.put(INJECT_CUSTOMER_ID_CLAIM, customerId.toString());

        claims.put(INJECT_COGNITO_GROUPS_CLAIM, String.join(",", cognitoGroups));
        claims.put(CognitoUserInfo.ACCESS_RIGHTS_CLAIM, accessRights.stream()
            .map(AccessRight::toPersistedString)
            .collect(Collectors.joining(",")));

        if (nonNull(username)) {
            claims.put(INJECT_NVA_USERNAME_CLAIM, username);
        }

        if (nonNull(personCristinId)) {
            claims.put(INJECT_CRISTIN_ID_CLAIM, personCristinId.toString());
        }

        if (nonNull(topLevelCristinOrgId)) {
            claims.put(INJECT_TOP_ORG_CRISTIN_ID_CLAIM, topLevelCristinOrgId.toString());
        }

        var requestInfo = getRequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(claims));

        return requestInfo;
    }

    private static RequestInfo getRequestInfo() {
        return attempt(() -> RequestInfo.fromString("{}")).orElseThrow();
    }

    private RequestInfo createThirdPartyRequestInfo(List<AccessRight> accessRights)
        throws JsonProcessingException {

        var cognitoGroups = accessRights.stream().map(getCognitoGroup(
            PublicationPermissionStrategyTest.EXTERNAL_CLIENT_CUSTOMER_URI)).toList();

        var claims = new HashMap<String, String>();
        claims.put(INJECT_COGNITO_GROUPS_CLAIM, String.join(",", cognitoGroups));
        claims.put(ISS_CLAIM, EXTERNAL_ISSUER);
        claims.put(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID);
        claims.put(SCOPE_CLAIM, SCOPES_THIRD_PARTY_PUBLICATION_READ);

        var requestInfo = getRequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(claims));

        return requestInfo;
    }

    protected RequestInfo createThirdPartyRequestInfo()
        throws JsonProcessingException {

        var claims = new HashMap<String, String>();
        claims.put(ISS_CLAIM, EXTERNAL_ISSUER);
        claims.put(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID);
        claims.put(SCOPE_CLAIM, SCOPES_THIRD_PARTY_PUBLICATION_READ);

        var requestInfo = getRequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(claims));
        requestInfo.setHeaders(Map.of(AUTHORIZATION, BEARER_TOKEN));

        return requestInfo;
    }

    protected RequestInfo createBackendRequestInfo()
        throws JsonProcessingException {

        var claims = new HashMap<String, String>();
        claims.put(ISS_CLAIM, EXTERNAL_ISSUER);
        claims.put(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID);
        claims.put(SCOPE_CLAIM, BACKEND_SCOPE);

        var requestInfo = getRequestInfo();
        requestInfo.setRequestContext(getRequestContextForClaim(claims));
        requestInfo.setHeaders(Map.of(AUTHORIZATION, BEARER_TOKEN));

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

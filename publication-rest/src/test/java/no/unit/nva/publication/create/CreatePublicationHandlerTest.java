package no.unit.nva.publication.create;

import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.CustomerFixtures.customerAcceptingFilesForAllTypes;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.publication.create.CreatePublicationHandler.API_HOST;
import static no.unit.nva.publication.service.impl.ResourceService.NOT_PUBLISHABLE;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeIdentityServiceClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.hamcrest.core.IsEqual;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;

@ExtendWith(MockitoExtension.class)
class CreatePublicationHandlerTest extends ResourcesLocalTest {

  public static final String NVA_UNIT_NO = "nva.unit.no";
  public static final String WILDCARD = "*";
  public static final Javers JAVERS = JaversBuilder.javers().build();
  public static final String ASSOCIATED_ARTIFACTS_FIELD = "associatedArtifacts";
  private static final String CUSTOMER_API_NOT_RESPONDING_OR_NOT_RESPONDING_AS_EXPECTED =
      "Customer API not responding or not responding as expected!";
  private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
  private static final String EXTERNAL_CLIENT_ID = "external-client-id";
  private static final Integer HTTP_STATUS_UNPROCESSABLE_CONTENT = 422;
  private static final String COGNITO_AUTHORIZER_URLS = "COGNITO_AUTHORIZER_URLS";
  private static final String SCOPES_THIRD_PARTY_PUBLICATION_READ =
      "https://api.nva.unit.no/scopes/third-party/publication-read";
  private final Context context = new FakeContext();
  private String testUserName;
  private CreatePublicationHandler handler;
  private ByteArrayOutputStream outputStream;
  private Publication samplePublication;
  private URI topLevelCristinOrgId;
  private GetExternalClientResponse getExternalClientResponse;
  private ResourceService resourceService;
  private Environment environmentMock;
  private FakeIdentityServiceClient identityServiceClient;
  private URI customerId;

  /** Setting up test environment. */
  @BeforeEach
  public void setUp() {
    super.init();

    environmentMock = mock(Environment.class);
    identityServiceClient = spy(new FakeIdentityServiceClient().withDefaultCustomers());

    lenient().when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
    when(environmentMock.readEnv(API_HOST)).thenReturn(NVA_UNIT_NO);
    when(environmentMock.readEnv(COGNITO_AUTHORIZER_URLS)).thenReturn("http://localhost:3000");

    resourceService = getResourceService(client);

    handler = new CreatePublicationHandler(resourceService, environmentMock, identityServiceClient);
    outputStream = new ByteArrayOutputStream();
    samplePublication = randomPublication();
    testUserName = samplePublication.getResourceOwner().getOwner().getValue();
    topLevelCristinOrgId = randomUri();
    customerId =
        UriWrapper.fromHost(NVA_UNIT_NO)
            .addChild("customer", UUID.randomUUID().toString())
            .getUri();

    getExternalClientResponse =
        new GetExternalClientResponse(EXTERNAL_CLIENT_ID, "someone@123", customerId, randomUri());
    lenient()
        .doReturn(getExternalClientResponse)
        .when(identityServiceClient)
        .getExternalClient(any());

    identityServiceClient.withCustomer(customerAcceptingFilesForAllTypes(customerId));
  }

  private static Class<?>[] protectedDegreeInstanceTypeClassesProvider() {
    return PROTECTED_DEGREE_INSTANCE_TYPES;
  }

  @Test
  void requestToHandlerReturnsMinRequiredFieldsWhenRequestBodyIsEmpty() throws Exception {
    var inputStream = createPublicationRequest(null);
    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    var publicationResponse = actual.getBodyObject(PublicationResponseElevatedUser.class);
    assertExistenceOfMinimumRequiredFields(publicationResponse);
  }

  @Test
  void requestToHandlerReturnsMinRequiredFieldsWhenRequestContainsEmptyResource() throws Exception {
    var request = createEmptyPublicationRequest();
    var inputStream = createPublicationRequest(request);
    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    var publicationResponse = actual.getBodyObject(PublicationResponseElevatedUser.class);
    assertExistenceOfMinimumRequiredFields(publicationResponse);
  }

  @Test
  void shouldPersistDraftWhenRegularUserAttemptsToPersistPublicationWithStatusPublished()
      throws Exception {
    var request = createEmptyPublicationRequest();
    request.setStatus(PublicationStatus.PUBLISHED);
    var inputStream = createPublicationRequest(request);
    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    var publicationResponse = actual.getBodyObject(PublicationResponseElevatedUser.class);
    assertThat(publicationResponse.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
  }

  @ParameterizedTest(
      name =
          "requestToHandlerWithStatusFromAnExternalClientShouldPersistDocumentWithSameStatus with"
              + " status: \"{0}\"")
  @ValueSource(strings = {"DRAFT", "PUBLISHED"})
  void shouldPersistProvidedStatusWhenMachineUserPersistsPublicationWithAnyStatus(
      String statusString) throws Exception {
    var status = PublicationStatus.valueOf(statusString);
    var request = createEmptyPublicationRequest();
    request.setStatus(status);
    request.setEntityDescription(randomPublishableEntityDescription());
    var inputStream = requestFromExternalClient(request);
    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    var publicationResponse = actual.getBodyObject(PublicationResponseElevatedUser.class);
    assertThat(publicationResponse.getStatus(), is(equalTo(status)));
  }

  @Test
  void shouldSetPublishedDateWhenMachineUserPersistsPublicationWithStatusPublished()
      throws Exception {
    var request = createEmptyPublicationRequest();
    request.setStatus(PublicationStatus.PUBLISHED);
    request.setEntityDescription(randomPublishableEntityDescription());
    var inputStream = requestFromExternalClient(request);
    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    var publicationResponse = actual.getBodyObject(PublicationResponseElevatedUser.class);
    assertNotNull(publicationResponse.getPublishedDate());
  }

  @Test
  void
      shouldReturnBadRequestWhenAnExternalClientTriesToCreatePublishedDocumentWithoutTitleAndDoiRef()
          throws Exception {
    var request = createEmptyPublicationRequest();
    request.setStatus(PublicationStatus.PUBLISHED);
    var inputStream = requestFromExternalClient(request);
    handler.handleRequest(inputStream, outputStream, context);

    var actual = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    var body = actual.getBodyObject(Problem.class);
    assertThat(body.getDetail(), is(IsEqual.equalTo(NOT_PUBLISHABLE)));
  }

  @Test
  void shouldReturnsExternalClientDetailsWhenCalledWithThirdPartyCredentials() throws Exception {
    var request = createEmptyPublicationRequest();
    var inputStream = requestFromExternalClient(request);
    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    var publicationResponse = actual.getBodyObject(PublicationResponseElevatedUser.class);

    var expectedOwner = getExternalClientResponse.getActingUser();
    var expectedOwnerAffiliation = getExternalClientResponse.getCristinUrgUri();
    var expectedPublisherId = getExternalClientResponse.getCustomerUri();

    assertThat(
        publicationResponse.getResourceOwner().getOwner().getValue(),
        is(IsEqual.equalTo(expectedOwner)));
    assertThat(
        publicationResponse.getResourceOwner().getOwnerAffiliation(),
        is(equalTo(expectedOwnerAffiliation)));
    assertThat(publicationResponse.getPublisher().getId(), is(equalTo(expectedPublisherId)));
  }

  @Test
  void
      shouldSaveAllSuppliedInformationOfPublicationRequestExceptForInternalInformationDecidedByService()
          throws Exception {
    var publicationWithoutFilesAndCuratingInstitutions =
        samplePublication
            .copy()
            .withAssociatedArtifacts(List.of())
            .withCuratingInstitutions(Set.of())
            .build();
    var request =
        CreatePublicationRequest.fromPublication(publicationWithoutFilesAndCuratingInstitutions);
    var inputStream = createPublicationRequest(request);
    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);

    var actualPublicationResponse = actual.getBodyObject(PublicationResponseElevatedUser.class);

    var expectedPublicationResponse =
        constructResponseSettingFieldsThatAreNotCopiedByTheRequest(
            publicationWithoutFilesAndCuratingInstitutions, actualPublicationResponse);

    var diff = JAVERS.compare(expectedPublicationResponse, actualPublicationResponse);
    assertThat(
        actualPublicationResponse.getIdentifier(),
        is(equalTo(expectedPublicationResponse.getIdentifier())));
    assertThat(
        actualPublicationResponse.getPublisher(),
        is(equalTo(expectedPublicationResponse.getPublisher())));
    assertThat(
        diff.prettyPrint(), actualPublicationResponse, is(equalTo(expectedPublicationResponse)));
  }

  @Test
  void shouldReturnUnauthorizedWhenUserCannotBeIdentified() throws IOException {
    var event = requestWithoutUsername(createEmptyPublicationRequest());
    handler.handleRequest(event, outputStream, context);
    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
  }

  @ParameterizedTest(
      name =
          "should return forbidden when creating instance type {0} without being a thesis curator")
  @MethodSource("protectedDegreeInstanceTypeClassesProvider")
  void shouldReturnForbiddenCreatingProtectedDegreePublicationWithoutBeingThesisCurator(
      final Class<?> protectedDegreeInstanceClass) throws IOException {
    var thesisPublication =
        samplePublication
            .copy()
            .withEntityDescription(publishableEntityDescription(protectedDegreeInstanceClass))
            .build();
    var event =
        requestWithoutAccessRights(CreatePublicationRequest.fromPublication(thesisPublication));
    handler.handleRequest(event, outputStream, context);
    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
  }

  @ParameterizedTest(
      name =
          "should allow creating protected degree instance type {0} when done by external client")
  @MethodSource("protectedDegreeInstanceTypeClassesProvider")
  void shouldPersistDegreePublicationWhenUserIsExternalClient(Class<?> protectedDegreeInstanceClass)
      throws IOException {
    var thesisPublication =
        samplePublication
            .copy()
            .withAssociatedArtifacts(List.of())
            .withEntityDescription(publishableEntityDescription(protectedDegreeInstanceClass))
            .build();
    var event =
        requestFromExternalClient(CreatePublicationRequest.fromPublication(thesisPublication));
    handler.handleRequest(event, outputStream, context);
    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
  }

  @Test
  void shouldReturnUnauthorizedWhenRequestIsFromExternalClientAndClientIdIsMissing()
      throws IOException {
    var event = requestFromExternalClientWithoutClientId(createEmptyPublicationRequest());
    handler.handleRequest(event, outputStream, context);
    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
  }

  @Test
  void shouldReturnBadGatewayIfIdentityServiceIsUnavailable() throws IOException {
    var event = prepareRequestWithFileForTypeWhereNotAllowed();
    identityServiceClient.withUnavailableIdentityService();

    handler.handleRequest(event, outputStream, context);

    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
    assertThat(
        response.getBodyObject(Problem.class).getDetail(),
        containsString(CUSTOMER_API_NOT_RESPONDING_OR_NOT_RESPONDING_AS_EXPECTED));
  }

  @Test
  void shouldReturnInternalServerErrorIfCustomerIsNotFound() throws IOException {
    var event = prepareRequestWithFileForTypeWhereNotAllowed();
    var handlerWithoutCustomers =
        new CreatePublicationHandler(
            resourceService, environmentMock, new FakeIdentityServiceClient());

    handlerWithoutCustomers.handleRequest(event, outputStream, context);

    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
  }

  // Following tests are commented out pending decision on how to handle user customer and not
  // publication customer
  //    @Test
  //    void
  // shouldReturnBadRequestIfProvidedWithOneOrMoreFilesHasNullRightsRetentionSetButCustomerHasAOverridableConfig()
  //        throws IOException {
  //
  //        WireMock.reset();
  //        stubSuccessfulTokenResponse();
  //        stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs(customerId);
  //
  //        var file = new PendingOpenFile(UUID.randomUUID(),
  //                                       RandomDataGenerator.randomString(),
  //                                       RandomDataGenerator.randomString(),
  //                                       RandomDataGenerator.randomInteger().longValue(),
  //                                       RandomDataGenerator.randomUri(),
  //                                       PublisherVersion.ACCEPTED_VERSION,
  //                                       (Instant) null,
  //
  // RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
  //                                       RandomDataGenerator.randomString(),
  //                                       new UserUploadDetails(null, null));
  //        // Waiting for datamodel changes as
  //        // Generator sets publisherAuth to true
  //
  //
  // file.setRightsRetentionStrategy(NullRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY));
  //
  //        var request = createEmptyPublicationRequest();
  //        request.setAssociatedArtifacts(new AssociatedArtifactList(file));
  //        request.setEntityDescription(publishableEntityDescription(AcademicArticle.class));
  //
  //        var inputStream = createPublicationRequest(request);
  //
  //        handler.handleRequest(inputStream, outputStream, context);
  //
  //        var actual = GatewayResponse.fromOutputStream(outputStream, Problem.class);
  //        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
  //    }

  @Test
  void shouldAllowNullRightsHolder() throws IOException {
    var request = createEmptyPublicationRequest();

    var inputStream = createPublicationRequest(request);

    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
  }

  @Test
  void shouldNotAllowEmptyRightsHolder() throws IOException {
    var request = createEmptyPublicationRequest();
    // Completely empty string in transformed to null during serialization/deserialization
    request.setRightsHolder(" ");

    var inputStream = createPublicationRequest(request);

    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HTTP_STATUS_UNPROCESSABLE_CONTENT)));
  }

  @Test
  void shouldNotAllowLeadingWhitespaceForRightsHolder() throws IOException {
    var request = createEmptyPublicationRequest();
    request.setRightsHolder(" abc");

    var inputStream = createPublicationRequest(request);

    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HTTP_STATUS_UNPROCESSABLE_CONTENT)));
  }

  @Test
  void shouldNotAllowTrailingWhitespaceForRightsHolder() throws IOException {
    var request = createEmptyPublicationRequest();
    request.setRightsHolder("abcåøæ ");

    var inputStream = createPublicationRequest(request);

    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HTTP_STATUS_UNPROCESSABLE_CONTENT)));
  }

  @Test
  void shouldNotAllowExecutableScriptForRightsHolder() throws IOException {
    var request = createEmptyPublicationRequest();
    request.setRightsHolder("<script>alert('Oh no!');</script>");

    var inputStream = createPublicationRequest(request);

    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HTTP_STATUS_UNPROCESSABLE_CONTENT)));
  }

  @Test
  void shouldAllowUnicodeLettersAndDigitsInRightsHolder() throws IOException {
    var request = createEmptyPublicationRequest();
    request.setRightsHolder("1234567890ÅØÆåøæ AOEaoe");

    var inputStream = createPublicationRequest(request);

    handler.handleRequest(inputStream, outputStream, context);

    var actual =
        GatewayResponse.fromOutputStream(outputStream, PublicationResponseElevatedUser.class);
    assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
  }

  @Test
  void shouldValidateOkIfEntityDescriptionIsNotSet() throws IOException {
    var event = createPublicationRequestFromString("{}");

    handler.handleRequest(event, outputStream, context);

    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
  }

  @Test
  void shouldValidateOkIfReferenceIsNotSet() throws IOException {
    var body = bodyWithNoReference();
    var event = createPublicationRequestFromString(body);

    handler.handleRequest(event, outputStream, context);

    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
  }

  @Test
  void shouldValidateOkIfInstanceTypeIsNotSet() throws IOException {
    var body = bodyWithEmptyReference();
    var event = createPublicationRequestFromString(body);

    handler.handleRequest(event, outputStream, context);

    var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
    assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
  }

  private static String bodyWithNoReference() {
    return """
    {
        "entityDescription": {
            "type": "EntityDescription"
        }
    }
    """;
  }

  private static String bodyWithEmptyReference() {
    return """
    {
        "entityDescription": {
            "type": "EntityDescription",
            "reference": {
                "type": "Reference"
            }
        }
    }
    """;
  }

  private static void updateCreatePublicationRequestWithInvalidAssociatedArtifact(
      ObjectNode publicationRequestJsonObject) throws JsonProcessingException {
    var associatedArtifacts =
        (ArrayNode) publicationRequestJsonObject.get(ASSOCIATED_ARTIFACTS_FIELD);
    associatedArtifacts.add(createNullAssociatedArtifact());
  }

  private static JsonNode createNullAssociatedArtifact() throws JsonProcessingException {
    var nullObject = dtoObjectMapper.writeValueAsString(new NullAssociatedArtifact());
    return dtoObjectMapper.readTree(nullObject);
  }

  private InputStream prepareRequestWithFileForTypeWhereNotAllowed()
      throws JsonProcessingException {
    var publicationRequestJsonObject = createCreatePublicationRequestAsJsonObject();
    return createPublicationRequestFromString(
        dtoObjectMapper.writeValueAsString(publicationRequestJsonObject));
  }

  private InputStream createPublicationRequestEventWithInvalidAssociatedArtifacts()
      throws JsonProcessingException {
    var publicationRequestJsonObject = createCreatePublicationRequestAsJsonObject();
    updateCreatePublicationRequestWithInvalidAssociatedArtifact(publicationRequestJsonObject);
    return createPublicationRequestFromString(
        dtoObjectMapper.writeValueAsString(publicationRequestJsonObject));
  }

  private ObjectNode createCreatePublicationRequestAsJsonObject() throws JsonProcessingException {
    var publicationRequest =
        dtoObjectMapper.writeValueAsString(
            CreatePublicationRequest.fromPublication(samplePublication));
    return (ObjectNode) dtoObjectMapper.readTree(publicationRequest);
  }

  private CreatePublicationRequest createEmptyPublicationRequest() {
    return new CreatePublicationRequest();
  }

  private PublicationResponse constructResponseSettingFieldsThatAreNotCopiedByTheRequest(
      Publication samplePublication, PublicationResponse actualPublicationResponse) {
    var expectedPublication =
        setAllFieldsThatAreNotCopiedFromTheCreateRequest(
            samplePublication, actualPublicationResponse);
    return PublicationResponseElevatedUser.fromPublication(expectedPublication);
  }

  private Publication setAllFieldsThatAreNotCopiedFromTheCreateRequest(
      Publication samplePublication, PublicationResponse actualPublicationResponse) {
    return attempt(() -> removeAllFieldsThatAreNotCopiedFromTheCreateRequest(samplePublication))
        .map(
            publication ->
                setAllFieldsThatAreAutomaticallySetByResourceService(
                    publication, actualPublicationResponse))
        .orElseThrow();
  }

  private Publication setAllFieldsThatAreAutomaticallySetByResourceService(
      Publication samplePublication, PublicationResponse actualPublicationResponse) {
    return samplePublication
        .copy()
        .withIdentifier(actualPublicationResponse.getIdentifier())
        .withCreatedDate(actualPublicationResponse.getCreatedDate())
        .withModifiedDate(actualPublicationResponse.getModifiedDate())
        .withIndexedDate(actualPublicationResponse.getIndexedDate())
        .withStatus(PublicationStatus.DRAFT)
        .withResourceOwner(actualPublicationResponse.getResourceOwner())
        .build();
  }

  private Publication removeAllFieldsThatAreNotCopiedFromTheCreateRequest(
      Publication samplePublication) {
    return samplePublication
        .copy()
        .withDoi(null)
        .withHandle(null)
        .withLink(null)
        .withPublishedDate(null)
        .withPublisher(new Organization.Builder().withId(customerId).build())
        .withResourceOwner(null)
        .build();
  }

  private void assertExistenceOfMinimumRequiredFields(PublicationResponse publicationResponse) {
    assertThat(publicationResponse.getIdentifier(), is(not(nullValue())));
    assertThat(publicationResponse.getIdentifier(), is(instanceOf(SortableIdentifier.class)));
    assertThat(publicationResponse.getCreatedDate(), is(not(nullValue())));
    assertThat(
        publicationResponse.getResourceOwner().getOwner().getValue(),
        is(IsEqual.equalTo(testUserName)));
    assertThat(
        publicationResponse.getResourceOwner().getOwnerAffiliation(),
        is(equalTo(topLevelCristinOrgId)));
    assertThat(publicationResponse.getPublisher().getId(), is(equalTo(customerId)));
  }

  private InputStream createPublicationRequest(CreatePublicationRequest request)
      throws JsonProcessingException {

    return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
        .withUserName(testUserName)
        .withCurrentCustomer(customerId)
        .withTopLevelCristinOrgId(topLevelCristinOrgId)
        .withBody(request)
        .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD, MANAGE_DEGREE)
        .build();
  }

  private InputStream requestWithoutAccessRights(CreatePublicationRequest request)
      throws JsonProcessingException {

    return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
        .withUserName(testUserName)
        .withCurrentCustomer(customerId)
        .withTopLevelCristinOrgId(topLevelCristinOrgId)
        .withBody(request)
        .build();
  }

  private InputStream createPublicationRequestFromString(String request)
      throws JsonProcessingException {

    return new HandlerRequestBuilder<String>(dtoObjectMapper)
        .withUserName(testUserName)
        .withCurrentCustomer(customerId)
        .withTopLevelCristinOrgId(topLevelCristinOrgId)
        .withBody(request)
        .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD, MANAGE_DEGREE)
        .build();
  }

  private InputStream requestFromExternalClient(CreatePublicationRequest request)
      throws JsonProcessingException {
    return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
        .withBody(request)
        .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
        .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
        .withScope(SCOPES_THIRD_PARTY_PUBLICATION_READ)
        .build();
  }

  private InputStream requestFromExternalClientWithoutClientId(CreatePublicationRequest request)
      throws JsonProcessingException {
    return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
        .withBody(request)
        .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
        .withScope(SCOPES_THIRD_PARTY_PUBLICATION_READ)
        .build();
  }

  private InputStream requestWithoutUsername(CreatePublicationRequest request)
      throws JsonProcessingException {

    return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
        .withCurrentCustomer(customerId)
        .withBody(request)
        .build();
  }

  private EntityDescription randomPublishableEntityDescription() {
    return new EntityDescription.Builder()
        .withMainTitle(randomString())
        .withReference(
            new Reference.Builder()
                .withDoi(RandomDataGenerator.randomDoi())
                .withPublicationInstance(PublicationInstanceBuilder.randomPublicationInstance())
                .build())
        .build();
  }

  private EntityDescription publishableEntityDescription(final Class<?> instanceClass) {
    return new EntityDescription.Builder()
        .withMainTitle(randomString())
        .withReference(
            new Reference.Builder()
                .withDoi(RandomDataGenerator.randomDoi())
                .withPublicationInstance(
                    PublicationInstanceBuilder.randomPublicationInstance(instanceClass))
                .build())
        .build();
  }
}

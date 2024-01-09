package no.unit.nva.publication.create;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.events.bodies.CreatePublicationRequest;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.validation.config.CustomerApiFilesAllowedForTypesConfigSupplier;
import no.unit.nva.publication.validation.DefaultPublicationValidator;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
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
@WireMockTest(httpsEnabled = true)
class CreatePublicationHandlerTest extends ResourcesLocalTest {

    public static final String NVA_UNIT_NO = "nva.unit.no";
    public static final String WILDCARD = "*";
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final Clock CLOCK = Clock.systemDefaultZone();
    public static final String ASSOCIATED_ARTIFACTS_FIELD = "associatedArtifacts";
    private String testUserName;
    private URI customerId;
    private CreatePublicationHandler handler;
    private ByteArrayOutputStream outputStream;
    private final Context context = new FakeContext();
    private Publication samplePublication;
    private URI topLevelCristinOrgId;

    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";
    private GetExternalClientResponse getExternalClientResponse;
    private HttpClient configClient;
    private ResourceService resourceService;
    private Environment environmentMock;
    private IdentityServiceClient identityServiceClient;

    public static Stream<Exception> httpClientExceptionsProvider() {
        return Stream.of(new ConnectException(), new InterruptedException());
    }

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) throws NotFoundException {
        super.init();

        customerId = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl() + "/customer/" + UUID.randomUUID());

        stubCustomerRequestWhereAllTypesAllowFiles(customerId);

        getExternalClientResponse = new GetExternalClientResponse(EXTERNAL_CLIENT_ID,
                                                                  "someone@123",
                                                                  customerId,
                                                                  randomUri());

        environmentMock = mock(Environment.class);
        identityServiceClient = mock(IdentityServiceClient.class);

        lenient().when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);
        when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environmentMock.readEnv(API_HOST)).thenReturn(NVA_UNIT_NO);

        resourceService = new ResourceService(client, CLOCK);
        configClient = WiremockHttpClient.create();

        var publicationValidator =
            new DefaultPublicationValidator(new CustomerApiFilesAllowedForTypesConfigSupplier(configClient));

        handler = new CreatePublicationHandler(resourceService,
                                               environmentMock,
                                               identityServiceClient,
                                               publicationValidator);
        outputStream = new ByteArrayOutputStream();
        samplePublication = randomPublication();
        testUserName = samplePublication.getResourceOwner().getOwner().getValue();
        topLevelCristinOrgId = randomUri();
    }

    @Test
    void shouldAcceptUnpublishableFileType() throws IOException {
        var serialized = IoUtils.stringFromResources(Path.of("publication_with_unpublishable_file.json"));
        var deserialized = attempt(() -> dtoObjectMapper.readValue(serialized, Publication.class)).orElseThrow();
        var publishingRequest = CreatePublicationRequest.fromPublication(deserialized);
        var inputStream = createPublicationRequest(publishingRequest);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    }

    @Test
    void requestToHandlerReturnsMinRequiredFieldsWhenRequestBodyIsEmpty()
        throws Exception {
        var inputStream = createPublicationRequest(null);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertExistenceOfMinimumRequiredFields(publicationResponse);
    }

    @Test
    void requestToHandlerReturnsMinRequiredFieldsWhenRequestContainsEmptyResource() throws Exception {
        var request = createEmptyPublicationRequest();
        var inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertExistenceOfMinimumRequiredFields(publicationResponse);
    }

    @Test
    void shouldPersistDraftWhenRegularUserAttemptsToPersistPublicationWithStatusPublished() throws Exception {
        var request = createEmptyPublicationRequest();
        request.setStatus(PublicationStatus.PUBLISHED);
        var inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertThat(publicationResponse.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
    }

    @ParameterizedTest(name = "requestToHandlerWithStatusFromAnExternalClientShouldPersistDocumentWithSameStatus with"
                              + " status: \"{0}\"")
    @ValueSource(strings = {"DRAFT", "PUBLISHED"})
    void shouldPersistProvidedStatusWhenMachineUserPersistsPublicationWithAnyStatus(String statusString)
        throws Exception {
        var status = PublicationStatus.valueOf(statusString);
        var request = createEmptyPublicationRequest();
        request.setStatus(status);
        request.setEntityDescription(randomPublishableEntityDescription());
        var inputStream = requestFromExternalClient(request);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertThat(publicationResponse.getStatus(), is(equalTo(status)));
    }

    @Test
    void shouldReturnBadRequestWhenAnExternalClientTriesToCreatePublishedDocumentWithoutTitleAndDoiRef()
        throws Exception {
        var request = createEmptyPublicationRequest();
        request.setStatus(PublicationStatus.PUBLISHED);
        var inputStream = requestFromExternalClient(request);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        var body = actual.getBodyObject(Problem.class);
        assertThat(body.getDetail(), is(equalTo(NOT_PUBLISHABLE)));
    }

    @Test
    void shouldReturnsExternalClientDetailsWhenCalledWithThirdPartyCredentials() throws Exception {
        var request = createEmptyPublicationRequest();
        var inputStream = requestFromExternalClient(request);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);

        var expectedOwner = getExternalClientResponse.getActingUser();
        var expectedOwnerAffiliation = getExternalClientResponse.getCristinUrgUri();
        var expectedPublisherId = getExternalClientResponse.getCustomerUri();

        assertThat(publicationResponse.getResourceOwner().getOwner().getValue(), is(equalTo(expectedOwner)));
        assertThat(publicationResponse.getResourceOwner().getOwnerAffiliation(), is(equalTo(expectedOwnerAffiliation)));
        assertThat(publicationResponse.getPublisher().getId(), is(equalTo(expectedPublisherId)));
    }

    @Test
    void shouldReturnsResourceWithFilSetWhenRequestContainsFileSet() throws Exception {
        var associatedArtifactsInPublication = randomPublication().getAssociatedArtifacts();
        var request = createEmptyPublicationRequest();
        request.setAssociatedArtifacts(associatedArtifactsInPublication);
        request.setEntityDescription(randomPublishableEntityDescription());

        var inputStream = createPublicationRequest(request);

        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        assertThat(publicationResponse.getAssociatedArtifacts(), is(equalTo(associatedArtifactsInPublication)));
        assertExistenceOfMinimumRequiredFields(publicationResponse);
    }

    @Test
    void shouldSaveAllSuppliedInformationOfPublicationRequestExceptForInternalInformationDecidedByService()
        throws Exception {
        var request = CreatePublicationRequest.fromPublication(samplePublication);
        var inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);

        var actualPublicationResponse = actual.getBodyObject(PublicationResponse.class);

        var expectedPublicationResponse =
            constructResponseSettingFieldsThatAreNotCopiedByTheRequest(samplePublication, actualPublicationResponse);

        var diff = JAVERS.compare(expectedPublicationResponse, actualPublicationResponse);
        assertThat(actualPublicationResponse.getIdentifier(), is(equalTo(expectedPublicationResponse.getIdentifier())));
        assertThat(actualPublicationResponse.getPublisher(), is(equalTo(expectedPublicationResponse.getPublisher())));
        assertThat(diff.prettyPrint(), actualPublicationResponse, is(equalTo(expectedPublicationResponse)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserCannotBeIdentified() throws IOException {
        var event = requestWithoutUsername(createEmptyPublicationRequest());
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnForbiddenWhenNoAccessRight() throws IOException {
        var thesisPublication = samplePublication.copy()
                                    .withEntityDescription(thesisPublishableEntityDescription())
                                    .build();
        var event = requestWithoutAccessRights(CreatePublicationRequest.fromPublication(thesisPublication));
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldPersistDegreePublicationWhenUserIsExternalClient() throws IOException {
        var thesisPublication = samplePublication.copy()
                                    .withEntityDescription(thesisPublishableEntityDescription())
                                    .build();
        var event = requestFromExternalClient(CreatePublicationRequest.fromPublication(thesisPublication));
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    }

    private void stubCustomerRequestWhereAllTypesAllowFiles(URI customerUri) {
        stubCustomerRequestWithResponse(customerUri, allInstanceTypes());
    }

    private void stubCustomerRequestWhereNoTypesAllowFiles(URI customerUri) {
        stubCustomerRequestWithResponse(customerUri, noInstanceTypes());
    }

    private void stubCustomerRequestWithResponse(URI customerUri, String response) {
        stubFor(get(urlEqualTo(customerUri.getPath()))
                    .willReturn(aResponse().withBody(response).withStatus(200))
        );
    }

    private String allInstanceTypes() {
        return IoUtils.stringFromResources(Path.of("customerResponseFileUploadAllowedForAllTypes.json"));
    }

    private String noInstanceTypes() {
        return IoUtils.stringFromResources(Path.of("customerResponseFileUploadAllowedForNoTypes.json"));
    }

    @Test
    void shouldReturnUnauthorizedWhenRequestIsFromExternalClientAndClientIdIsMissing() throws IOException {
        var event = requestFromExternalClientWithoutClientId(createEmptyPublicationRequest());
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenAssociatedArtifactsIsBad() throws IOException {
        var event = createPublicationRequestEventWithInvalidAssociatedArtifacts();
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString("AssociatedArtifact"));
    }

    @ParameterizedTest
    @MethodSource("httpClientExceptionsProvider")
    void shouldReturnBadGatewayIfCustomerApiHttpClientThrowsException(Exception exceptionToThrow)
        throws IOException, InterruptedException {
        WireMock.reset();
        configClient = mock(HttpClient.class);

        when(configClient.send(any(), any())).thenThrow(exceptionToThrow);

        var publicationValidator =
            new DefaultPublicationValidator(new CustomerApiFilesAllowedForTypesConfigSupplier(configClient));

        handler = new CreatePublicationHandler(resourceService,
                                               environmentMock,
                                               identityServiceClient,
                                               publicationValidator);

        var event = prepareRequestWithFileForTypeWhereNotAllowed();
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString("Gateway not responding or not responding as expected!"));
    }

    @Test
    void shouldReturnBadGatewayIfCustomerApiDoesNotRespondWithSuccessOk() throws IOException {
        var event = prepareRequestWithFileForTypeWhereNotAllowed();
        WireMock.reset();
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString("Gateway not responding or not responding as expected!"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"allowFileUploadForTypes\": {}}"})
    void shouldReturnBadRequestIfMalformedConfigReceivedFromCustomerApi(String customerResponse) throws IOException {
        var event = prepareRequestWithFileForTypeWhereNotAllowed();
        WireMock.reset();
        stubCustomerRequestWithResponse(customerId, customerResponse);
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString("Gateway not responding or not responding as expected!"));
    }

    @Test
    void shouldReturnBadRequestIfProvidingOneOrMoreFilesWhenNotAllowedInCustomerConfiguration() throws IOException {
        var event = prepareRequestWithFileForTypeWhereNotAllowed();
        WireMock.reset();
        stubCustomerRequestWhereNoTypesAllowFiles(customerId);
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString("Files not allowed for instance type"));
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

    private InputStream prepareRequestWithFileForTypeWhereNotAllowed() throws JsonProcessingException {
        var publicationRequestJsonObject = createCreatePublicationRequestAsJsonObject();
        return createPublicationRequestFromString(dtoObjectMapper.writeValueAsString(publicationRequestJsonObject));
    }

    private InputStream createPublicationRequestEventWithInvalidAssociatedArtifacts() throws JsonProcessingException {
        var publicationRequestJsonObject = createCreatePublicationRequestAsJsonObject();
        updateCreatePublicationRequestWithInvalidAssociatedArtifact(publicationRequestJsonObject);
        return createPublicationRequestFromString(dtoObjectMapper.writeValueAsString(publicationRequestJsonObject));
    }

    private static void
    updateCreatePublicationRequestWithInvalidAssociatedArtifact(ObjectNode publicationRequestJsonObject)
        throws JsonProcessingException {
        var associatedArtifacts = (ArrayNode) publicationRequestJsonObject.get(ASSOCIATED_ARTIFACTS_FIELD);
        associatedArtifacts.add(createNullAssociatedArtifact());
    }

    private ObjectNode createCreatePublicationRequestAsJsonObject() throws JsonProcessingException {
        var publicationRequest =
            dtoObjectMapper.writeValueAsString(CreatePublicationRequest.fromPublication(samplePublication));
        return (ObjectNode) dtoObjectMapper.readTree(publicationRequest);
    }

    private static JsonNode createNullAssociatedArtifact() throws JsonProcessingException {
        var nullObject = dtoObjectMapper.writeValueAsString(new NullAssociatedArtifact());
        return dtoObjectMapper.readTree(nullObject);
    }

    private CreatePublicationRequest createEmptyPublicationRequest() {
        return new CreatePublicationRequest();
    }

    private PublicationResponse constructResponseSettingFieldsThatAreNotCopiedByTheRequest(
        Publication samplePublication, PublicationResponse actualPublicationResponse) {
        var expectedPublication = setAllFieldsThatAreNotCopiedFromTheCreateRequest(samplePublication,
                                                                                   actualPublicationResponse);
        return PublicationResponse.fromPublication(expectedPublication);
    }

    private Publication setAllFieldsThatAreNotCopiedFromTheCreateRequest(
        Publication samplePublication, PublicationResponse actualPublicationResponse) {
        return attempt(() -> removeAllFieldsThatAreNotCopiedFromTheCreateRequest(samplePublication))
                   .map(publication ->
                            setAllFieldsThatAreAutomaticallySetByResourceService(publication,
                                                                                 actualPublicationResponse))
                   .orElseThrow();
    }

    private Publication setAllFieldsThatAreAutomaticallySetByResourceService(
        Publication samplePublication,
        PublicationResponse actualPublicationResponse) {
        return samplePublication.copy()
                   .withIdentifier(actualPublicationResponse.getIdentifier())
                   .withCreatedDate(actualPublicationResponse.getCreatedDate())
                   .withModifiedDate(actualPublicationResponse.getModifiedDate())
                   .withIndexedDate(actualPublicationResponse.getIndexedDate())
                   .withStatus(PublicationStatus.DRAFT)
                   .withResourceOwner(actualPublicationResponse.getResourceOwner())
                   .build();
    }

    private Publication removeAllFieldsThatAreNotCopiedFromTheCreateRequest(Publication samplePublication) {
        return samplePublication.copy()
                   .withDoi(null)
                   .withHandle(null)
                   .withLink(null)
                   .withPublishedDate(null)
                   .withPublisher(new Organization.Builder().withLabels(null).withId(customerId).build())
                   .withResourceOwner(null)
                   .build();
    }

    private void assertExistenceOfMinimumRequiredFields(PublicationResponse publicationResponse) {
        assertThat(publicationResponse.getIdentifier(), is(not(nullValue())));
        assertThat(publicationResponse.getIdentifier(), is(instanceOf(SortableIdentifier.class)));
        assertThat(publicationResponse.getCreatedDate(), is(not(nullValue())));
        assertThat(publicationResponse.getResourceOwner().getOwner().getValue(), is(equalTo(testUserName)));
        assertThat(publicationResponse.getResourceOwner().getOwnerAffiliation(), is(equalTo(topLevelCristinOrgId)));
        assertThat(publicationResponse.getPublisher().getId(), is(equalTo(customerId)));
    }

    private InputStream createPublicationRequest(CreatePublicationRequest request) throws JsonProcessingException {

        return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
                   .withUserName(testUserName)
                   .withCurrentCustomer(customerId)
                   .withTopLevelCristinOrgId(topLevelCristinOrgId)
                   .withBody(request)
                   .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD, MANAGE_DEGREE)
                   .build();
    }

    private InputStream requestWithoutAccessRights(CreatePublicationRequest request) throws JsonProcessingException {

        return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
                   .withUserName(testUserName)
                   .withCurrentCustomer(customerId)
                   .withTopLevelCristinOrgId(topLevelCristinOrgId)
                   .withBody(request)
                   .build();
    }

    private InputStream createPublicationRequestFromString(String request) throws JsonProcessingException {

        return new HandlerRequestBuilder<String>(dtoObjectMapper)
                   .withUserName(testUserName)
                   .withCurrentCustomer(customerId)
                   .withTopLevelCristinOrgId(topLevelCristinOrgId)
                   .withBody(request)
                   .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD, MANAGE_DEGREE)
                   .build();
    }

    private InputStream requestFromExternalClient(CreatePublicationRequest request) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
                   .withBody(request)
                   .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                   .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                   .build();
    }

    private InputStream requestFromExternalClientWithoutClientId(CreatePublicationRequest request)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreatePublicationRequest>(dtoObjectMapper)
                   .withBody(request)
                   .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                   .build();
    }

    private InputStream requestWithoutUsername(CreatePublicationRequest request) throws JsonProcessingException {

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

    private EntityDescription thesisPublishableEntityDescription() {
        return new EntityDescription.Builder()
                   .withMainTitle(randomString())
                   .withReference(
                       new Reference.Builder()
                           .withDoi(RandomDataGenerator.randomDoi())
                           .withPublicationInstance(
                               PublicationInstanceBuilder.randomPublicationInstance(DegreeMaster.class))
                           .build())
                   .build();
    }
}

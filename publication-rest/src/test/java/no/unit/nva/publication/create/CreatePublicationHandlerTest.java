package no.unit.nva.publication.create;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomSuccessfulCustomerResponse;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseAcceptingFilesForAllTypes;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseNotFound;
import static no.unit.nva.publication.CustomerApiStubs.stubSuccessfulCustomerResponseAllowingFilesForNoTypes;
import static no.unit.nva.publication.CustomerApiStubs.stubSuccessfulTokenResponse;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;
import no.unit.nva.publication.events.bodies.CreatePublicationRequest;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.permission.strategy.PermissionStrategy;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
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
@WireMockTest(httpsEnabled = true)
class CreatePublicationHandlerTest extends ResourcesLocalTest {

    public static final String NVA_UNIT_NO = "nva.unit.no";
    public static final String WILDCARD = "*";
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String ASSOCIATED_ARTIFACTS_FIELD = "associatedArtifacts";
    private static final String CUSTOMER_API_NOT_RESPONDING_OR_NOT_RESPONDING_AS_EXPECTED
        = "Customer API not responding or not responding as expected!";
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";
    private final Context context = new FakeContext();
    private String testUserName;
    private CreatePublicationHandler handler;
    private ByteArrayOutputStream outputStream;
    private Publication samplePublication;
    private URI topLevelCristinOrgId;
    private GetExternalClientResponse getExternalClientResponse;
    private ResourceService resourceService;
    private Environment environmentMock;
    private IdentityServiceClient identityServiceClient;
    private FakeSecretsManagerClient secretsManagerClient;
    private URI customerId;

    public static Stream<Exception> httpClientExceptionsProvider() {
        return Stream.of(new ConnectException(), new InterruptedException());
    }

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) throws NotFoundException {
        super.init();

        environmentMock = mock(Environment.class);
        identityServiceClient = mock(IdentityServiceClient.class);

        when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environmentMock.readEnv(API_HOST)).thenReturn(NVA_UNIT_NO);
        lenient().when(environmentMock.readEnv("BACKEND_CLIENT_SECRET_NAME")).thenReturn("secret");

        var baseUrl = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        lenient().when(environmentMock.readEnv("BACKEND_CLIENT_AUTH_URL"))
            .thenReturn(baseUrl.toString());

        resourceService = getResourceServiceBuilder().build();

        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("secret", credentials.toString());

        var httpClient = WiremockHttpClient.create();

        handler = new CreatePublicationHandler(resourceService,
                                               environmentMock,
                                               identityServiceClient,
                                               secretsManagerClient,
                                               httpClient);
        outputStream = new ByteArrayOutputStream();
        samplePublication = randomPublication();
        testUserName = samplePublication.getResourceOwner().getOwner().getValue();
        topLevelCristinOrgId = randomUri();
        customerId = UriWrapper.fromUri(wireMockRuntimeInfo.getHttpsBaseUrl())
                         .addChild("customer", UUID.randomUUID().toString())
                         .getUri();

        getExternalClientResponse = new GetExternalClientResponse(EXTERNAL_CLIENT_ID,
                                                                  "someone@123",
                                                                  customerId,
                                                                  randomUri());
        lenient().when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);

        stubSuccessfulTokenResponse();

        stubCustomerResponseAcceptingFilesForAllTypes(customerId);
    }

    private static Class<?>[] protectedDegreeInstanceTypeClassesProvider() {
        return PermissionStrategy.PROTECTED_DEGREE_INSTANCE_TYPES;
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
        assertThat(body.getDetail(), is(IsEqual.equalTo(NOT_PUBLISHABLE)));
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

        assertThat(publicationResponse.getResourceOwner().getOwner().getValue(), is(IsEqual.equalTo(expectedOwner)));
        assertThat(publicationResponse.getResourceOwner().getOwnerAffiliation(), is(equalTo(expectedOwnerAffiliation)));
        assertThat(publicationResponse.getPublisher().getId(), is(equalTo(expectedPublisherId)));
    }

    @Test
    void shouldSaveAllSuppliedInformationOfPublicationRequestExceptForInternalInformationDecidedByService()
        throws Exception {
        var publicationWithoutFiles = samplePublication.copy().withAssociatedArtifacts(List.of()).build();
        var request = CreatePublicationRequest.fromPublication(publicationWithoutFiles);
        var inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);

        var actualPublicationResponse = actual.getBodyObject(PublicationResponse.class);

        var expectedPublicationResponse =
            constructResponseSettingFieldsThatAreNotCopiedByTheRequest(publicationWithoutFiles,
                                                                       actualPublicationResponse);

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

    @ParameterizedTest(name = "should return forbidden when creating instance type {0} without being a thesis curator")
    @MethodSource("protectedDegreeInstanceTypeClassesProvider")
    void shouldReturnForbiddenCreatingProtectedDegreePublicationWithoutBeingThesisCurator(
        final Class<?> protectedDegreeInstanceClass) throws IOException {
        var thesisPublication = samplePublication.copy()
                                    .withEntityDescription(publishableEntityDescription(protectedDegreeInstanceClass))
                                    .build();
        var event = requestWithoutAccessRights(CreatePublicationRequest.fromPublication(thesisPublication));
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @ParameterizedTest(name = "should allow creating protected degree instance type {0} when done by external client")
    @MethodSource("protectedDegreeInstanceTypeClassesProvider")
    void shouldPersistDegreePublicationWhenUserIsExternalClient(Class<?> protectedDegreeInstanceClass) throws IOException {
        var thesisPublication = samplePublication.copy()
                                    .withEntityDescription(publishableEntityDescription(protectedDegreeInstanceClass))
                                    .build();
        var event = requestFromExternalClient(CreatePublicationRequest.fromPublication(thesisPublication));
        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
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

        var httpClient = mock(HttpClient.class);

        doThrow(exceptionToThrow).when(httpClient).send(any(), any());

        handler = new CreatePublicationHandler(resourceService,
                                               environmentMock,
                                               identityServiceClient,
                                               secretsManagerClient,
                                               httpClient);

        var event = prepareRequestWithFileForTypeWhereNotAllowed();

        handler.handleRequest(event, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString(CUSTOMER_API_NOT_RESPONDING_OR_NOT_RESPONDING_AS_EXPECTED));
    }

    @Test
    void shouldReturnBadGatewayIfCustomerApiDoesNotRespondWithSuccessOk() throws IOException, InterruptedException {
        var event = prepareRequestWithFileForTypeWhereNotAllowed();

        WireMock.reset();

        stubSuccessfulTokenResponse();
        stubCustomerResponseNotFound(customerId);

        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString(CUSTOMER_API_NOT_RESPONDING_OR_NOT_RESPONDING_AS_EXPECTED));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "[]", "{\"allowFileUploadForTypes\": {}}"})
    void shouldReturnBadRequestIfMalformedConfigReceivedFromCustomerApi(String customerResponse)
        throws IOException {
        var event = prepareRequestWithFileForTypeWhereNotAllowed();
        WireMock.reset();

        stubSuccessfulTokenResponse();
        stubCustomSuccessfulCustomerResponse(customerId, customerResponse);

        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString(CUSTOMER_API_NOT_RESPONDING_OR_NOT_RESPONDING_AS_EXPECTED));
    }

    @Test
    void shouldReturnBadRequestIfProvidingOneOrMoreFilesWhenNotAllowedInCustomerConfiguration()
        throws IOException {
        var event = prepareRequestWithFileForTypeWhereNotAllowed();
        WireMock.reset();

        stubSuccessfulTokenResponse();
        stubSuccessfulCustomerResponseAllowingFilesForNoTypes(customerId);

        handler.handleRequest(event, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        var body = response.getBodyObject(Problem.class);
        assertThat(body.getDetail(), containsString("Files not allowed for instance type"));
    }

    @Test
    void shouldReturnBadRequestIfProvidedWithOneOrMoreFilesHasNullRightsRetentionSetButCustomerHasAOveridableConfig()
        throws IOException {

        WireMock.reset();
        stubSuccessfulTokenResponse();
        stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs(customerId);

        var file = new UnpublishedFile(UUID.randomUUID(),
                                       RandomDataGenerator.randomString(),
                                       RandomDataGenerator.randomString(),
                                       RandomDataGenerator.randomInteger().longValue(),
                                       RandomDataGenerator.randomUri(),
                                       false,
                                       PublisherVersion.ACCEPTED_VERSION,
                                       (Instant) null,
                                       RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                                       RandomDataGenerator.randomString(),
                                       new UploadDetails(null, null));
        // Waiting for datamodel changes as
        // Generator sets publisherAuth to true

        file.setRightsRetentionStrategy(NullRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY));

        var associatedArtifactsInPublication = new AssociatedArtifactList(file);

        var request = createEmptyPublicationRequest();
        request.setAssociatedArtifacts(associatedArtifactsInPublication);
        request.setEntityDescription(publishableEntityDescription(AcademicArticle.class));

        var inputStream = createPublicationRequest(request);

        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    void shouldReturnFileWithOverridenByRRSIfConfiguredOnCustomerAndSetOnFile()
        throws IOException {

        WireMock.reset();
        stubSuccessfulTokenResponse();
        stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs(customerId);

        var file = new UnpublishedFile(UUID.randomUUID(),
                                       RandomDataGenerator.randomString(),
                                       RandomDataGenerator.randomString(),
                                       RandomDataGenerator.randomInteger().longValue(),
                                       RandomDataGenerator.randomUri(),
                                       false,
                                       PublisherVersion.ACCEPTED_VERSION,
                                       (Instant) null,
                                       RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                                       RandomDataGenerator.randomString(),
                                       new UploadDetails(null, null));
        // Waiting for datamodel changes as
        // Generator sets publisherAuth to true

        file.setRightsRetentionStrategy(OverriddenRightsRetentionStrategy.create(null, testUserName)); //
        // configuredType null as it
        // should not be used

        var associatedArtifactsInPublication = new AssociatedArtifactList(file);

        var request = createEmptyPublicationRequest();
        request.setAssociatedArtifacts(associatedArtifactsInPublication);
        request.setEntityDescription(publishableEntityDescription(AcademicArticle.class));

        var inputStream = createPublicationRequest(request);

        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, PublicationResponse.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));

        var publicationResponse = actual.getBodyObject(PublicationResponse.class);
        var actualFile = (File)
                             Lists.newArrayList(publicationResponse.getAssociatedArtifacts().stream().iterator())
                                 .getFirst();
        assertTrue(actualFile.getRightsRetentionStrategy() instanceof OverriddenRightsRetentionStrategy);
        assertThat(((OverriddenRightsRetentionStrategy) actualFile.getRightsRetentionStrategy()).getOverriddenBy(),
                   is(equalTo(testUserName)));
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

    @Test
    void shouldReturnBadRequestWhenAttemptingToPersistPublicationWithMultipleCristinIdentifiers() throws Exception {
        var request = createEmptyPublicationRequest();
        request.setAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Cristin", randomString()),
                                                new AdditionalIdentifier("Cristin", randomString())));
        var inputStream = createPublicationRequest(request);
        handler.handleRequest(inputStream, outputStream, context);

        var actual = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        var publicationResponse = actual.getBodyObject(Problem.class);
        assertThat(publicationResponse.getDetail(), containsString("Multiple Cristin identifiers are not allowed"));
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

    private static void
    updateCreatePublicationRequestWithInvalidAssociatedArtifact(ObjectNode publicationRequestJsonObject)
        throws JsonProcessingException {
        var associatedArtifacts = (ArrayNode) publicationRequestJsonObject.get(ASSOCIATED_ARTIFACTS_FIELD);
        associatedArtifacts.add(createNullAssociatedArtifact());
    }

    private static JsonNode createNullAssociatedArtifact() throws JsonProcessingException {
        var nullObject = dtoObjectMapper.writeValueAsString(new NullAssociatedArtifact());
        return dtoObjectMapper.readTree(nullObject);
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

    private ObjectNode createCreatePublicationRequestAsJsonObject() throws JsonProcessingException {
        var publicationRequest =
            dtoObjectMapper.writeValueAsString(CreatePublicationRequest.fromPublication(samplePublication));
        return (ObjectNode) dtoObjectMapper.readTree(publicationRequest);
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
                   .withPublisher(new Organization.Builder().withId(customerId).build())
                   .withResourceOwner(null)
                   .build();
    }

    private void assertExistenceOfMinimumRequiredFields(PublicationResponse publicationResponse) {
        assertThat(publicationResponse.getIdentifier(), is(not(nullValue())));
        assertThat(publicationResponse.getIdentifier(), is(instanceOf(SortableIdentifier.class)));
        assertThat(publicationResponse.getCreatedDate(), is(not(nullValue())));
        assertThat(publicationResponse.getResourceOwner().getOwner().getValue(), is(IsEqual.equalTo(testUserName)));
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

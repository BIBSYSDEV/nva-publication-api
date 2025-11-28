package no.unit.nva.publication.fetch;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.ETAG;
import static com.google.common.net.HttpHeaders.LOCATION;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.UUID.randomUUID;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.testing.PublicationGenerator.fromInstanceClassesExcluding;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomHiddenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomInternalFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.DO_NOT_REDIRECT_QUERY_PARAM;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ENV_NAME_NVA_FRONTEND_DOMAIN;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.ApiGatewayHandler.RESOURCE;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.Customer;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.FileDto;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ReadResourceService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.validation.ETag;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;

@ExtendWith(MockitoExtension.class)
@WireMockTest(httpsEnabled = true)
class FetchPublicationHandlerTest extends ResourcesLocalTest {

    public static final String IDENTIFIER_VALUE = "0ea0dd31-c202-4bff-8521-afd42b1ad8db";
    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_TYPE = restApiMapper.getTypeFactory()
                                                                           .constructParametricType(
                                                                               GatewayResponse.class,
                                                                               PublicationResponse.class);
    public static final String DATACITE_XML_RESOURCE_ELEMENT =
        "<resource xmlns=\"http://datacite" + ".org/schema/kernel-4\">";
    public static final String PUBLISHER_NAME = "publisher name";
    private static final Map<String, String> NO_QUERY_PARAMS = Map.of();
    private static final String TEXT_ANY = "text/*";
    private static final String TEXT_HTML = "text/html";
    private static final String APPLICATION_XHTML = "application/xhtml+xml";
    private static final String FIREFOX_DEFAULT_ACCEPT_HEADER =
        "text/html,application/xhtml+xml,application/xml;q=0" + ".9,image/avif,image/webp,*/*;q=0.8";
    private static final String WEBKIT_DEFAULT_ACCEPT_HEADER =
        "application/xml,application/xhtml+xml,text/html;q=0" + ".9,text/plain;q=0.8,image/png,*/*;q=0.5";
    private static final String IDENTIFIER_NULL_ERROR = "Identifier is not a valid UUID: null";
    private static final String COGNITO_AUTHORIZER_URLS = "COGNITO_AUTHORIZER_URLS";
    private final Context context = new FakeContext();
    private ResourceService publicationService;
    private ByteArrayOutputStream output;
    private FetchPublicationHandler fetchPublicationHandler;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp(@Mock Environment environment, @Mock IdentityServiceClient identityServiceClient) {
        super.init();
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv(COGNITO_AUTHORIZER_URLS)).thenReturn("http://localhost:3000");
        lenient().when(environment.readEnv(ENV_NAME_NVA_FRONTEND_DOMAIN)).thenReturn("localhost");

        publicationService = getResourceService(client);
        output = new ByteArrayOutputStream();
        var uriRetriever = new UriRetriever(WiremockHttpClient.create());
        fetchPublicationHandler = new FetchPublicationHandler(publicationService, uriRetriever, environment,
                                                              identityServiceClient);
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    void handlerReturnsOkResponseOnValidInput() throws IOException, ApiGatewayException {
        var publication = createPublication();
        Resource.fromPublication(publication).publish(publicationService, UserInstance.fromPublication(publication));
        var publicationIdentifier = publication.getIdentifier().toString();

        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier), output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("handler should return allowedOperations on files")
    void handlerReturnsAllowedOperationsOnFiles() throws IOException, ApiGatewayException {
        var publication = createPublication();
        Resource.fromPublication(publication).publish(publicationService, UserInstance.fromPublication(publication));
        var publicationIdentifier = publication.getIdentifier().toString();

        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier), output, context);
        var gatewayResponse = parseHandlerResponse();

        var file = gatewayResponse.getBodyObject(PublicationResponse.class)
                       .getAssociatedArtifacts()
                       .stream()
                       .filter(artifact -> artifact.getArtifactType().equals(OpenFile.TYPE))
                       .map(FileDto.class::cast)
                       .findFirst()
                       .orElseThrow();

        assertTrue(file.allowedOperations().contains(FileOperation.READ_METADATA));
    }

    @Test
    @DisplayName("handler should only define file type once")
    void handlerShouldOnlyMentionTypeOnce() throws IOException, ApiGatewayException {
        // had an issue that "type" was serialized zero or multiple times in the response of a FileResponse
        var publication = createPublication();
        Resource.fromPublication(publication).publish(publicationService, UserInstance.fromPublication(publication));
        var publicationIdentifier = publication.getIdentifier().toString();

        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier), output, context);
        var gatewayResponse = parseHandlerResponse();

        assertEquals(1, Pattern.compile(Pattern.quote("\"OpenFile\""), Pattern.DOTALL)
                            .matcher(gatewayResponse.getBody())
                            .results()
                            .count());
    }

    @Test
    void shouldReturnOkResponseWithDataCiteXmlBodyOnValidInput(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws IOException, ApiGatewayException {
        var publication = createPublicationWithPublisher(wireMockRuntimeInfo);
        var publicationIdentifier = publication.getIdentifier().toString();
        Resource.fromPublication(publication).publish(publicationService, UserInstance.fromPublication(publication));
        var headers = Map.of(HttpHeaders.ACCEPT, MediaTypes.APPLICATION_DATACITE_XML.toString());
        createCustomerMock(publication.getPublisher());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier, headers, NO_QUERY_PARAMS),
                                              output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertEquals(MediaTypes.APPLICATION_DATACITE_XML.toString(), gatewayResponse.getHeaders().get(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertTrue(gatewayResponse.getBody().contains(DATACITE_XML_RESOURCE_ELEMENT));
    }

    // TODO: Extend beyond JournalArticle
    @Test
    void shouldReturnSchemaOrgProfileWhenSchemaOrgMediaTypeIsRequested() throws IOException, ApiGatewayException {
        var publication = createPublication(JournalArticle.class);
        Resource.fromPublication(publication).publish(publicationService, UserInstance.fromPublication(publication));
        var identifier = publication.getIdentifier().toString();
        var headers = Map.of(ACCEPT, MediaTypes.SCHEMA_ORG.toString());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(identifier, headers, NO_QUERY_PARAMS), output,
                                              context);
        var gatewayResponse = parseHandlerResponse();
        var contentType = gatewayResponse.getHeaders().get(CONTENT_TYPE);
        assertThat(contentType, is(equalTo(MediaTypes.SCHEMA_ORG.toString())));
        assertThat(gatewayResponse.getBody(), containsString("\"@vocab\":\"https://schema.org/\""));
    }

    @Test
    void shouldReturnAllowedOperations(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws ApiGatewayException, IOException {
        var publication = createPublicationWithPublisher(wireMockRuntimeInfo);
        var publicationIdentifier = publication.getIdentifier().toString();
        Resource.fromPublication(publication).publish(publicationService, UserInstance.fromPublication(publication));
        createCustomerMock(publication.getPublisher());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier), output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getBody(), containsString("allowedOperations"));
    }

    @ParameterizedTest(name = "should redirect to frontend landing page when accept header is {0}")
    @ValueSource(strings = {TEXT_HTML, APPLICATION_XHTML, TEXT_ANY, FIREFOX_DEFAULT_ACCEPT_HEADER,
        WEBKIT_DEFAULT_ACCEPT_HEADER})
    void shouldRedirectToFrontendLandingPageIfPreferredContentTypeIsHtml(String acceptHeaderValue)
        throws ApiGatewayException, IOException {
        var publication = createPublication(JournalArticle.class);
        Resource.fromPublication(publication).publish(publicationService, UserInstance.fromPublication(publication));
        var identifier = publication.getIdentifier().toString();
        var headers = Map.of(ACCEPT, acceptHeaderValue);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(identifier, headers, NO_QUERY_PARAMS), output,
                                              context);

        var valueType = restApiMapper.getTypeFactory().constructParametricType(GatewayResponse.class, Void.class);

        GatewayResponse<Void> response = restApiMapper.readValue(output.toString(), valueType);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_SEE_OTHER)));

        var expectedLandingPage = "https://localhost/registration/" + identifier;
        assertThat(response.getHeaders().get(LOCATION), is(equalTo(expectedLandingPage)));
    }

    @Test
    @DisplayName("handler Returns NotFound Response On Publication Missing")
    void handlerReturnsNotFoundResponseOnPublicationMissing() throws IOException {

        fetchPublicationHandler.handleRequest(generateHandlerRequest(IDENTIFIER_VALUE), output, context);
        var gatewayResponse = parseFailureResponse();

        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        var actualDetail = getProblemDetail(gatewayResponse);
        assertThat(actualDetail, containsString(ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE));
        assertThat(actualDetail, containsString(IDENTIFIER_VALUE));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Empty Input")
    void handlerReturnsBadRequestResponseOnEmptyInput() throws IOException {
        var inputStream = new HandlerRequestBuilder<InputStream>(restApiMapper).withBody(null)
                              .withHeaders(null)
                              .withPathParameters(null)
                              .build();
        fetchPublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseFailureResponse();
        var actualDetail = gatewayResponse.getBodyObject(Problem.class).getDetail();
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(IDENTIFIER_NULL_ERROR));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        var inputStream = generateHandlerRequestWithMissingPathParameter();
        fetchPublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseFailureResponse();
        var actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(IDENTIFIER_NULL_ERROR));
    }

    @Test
    void handlerReturnsGoneWithPublicationDetailWhenPublicationIsDeletedAndDuplicateOfValueIsNotPresent()
        throws IOException, ApiGatewayException {
        var publication = createDeletedPublicationWithDuplicate(null);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var gatewayResponse = parseFailureResponse();
        var expectedTombstone = PublicationResponseElevatedUser.fromPublication(
            publication.copy().withAssociatedArtifacts(List.of()).build());

        var problem = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(), Problem.class);
        var actualPublication = JsonUtils.dtoObjectMapper.convertValue(problem.getParameters().get(RESOURCE),
                                                                       PublicationResponseElevatedUser.class);

        assertThat(actualPublication, is(equalTo(expectedTombstone)));
    }

    @Test
    void handlerRedirectToDuplicatePublicationWhenDeletedPublicationHasDuplicate()
        throws ApiGatewayException, IOException {
        var duplicateOfIdentifier = UriWrapper.fromUri(randomUri())
                                        .addChild(SortableIdentifier.next().toString())
                                        .getUri();
        var publication = createDeletedPublicationWithDuplicate(duplicateOfIdentifier);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var valueType = restApiMapper.getTypeFactory().constructParametricType(GatewayResponse.class, Void.class);

        GatewayResponse<Void> response = restApiMapper.readValue(output.toString(), valueType);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_MOVED_PERM)));

        assertThat(response.getHeaders().get(LOCATION), is(equalTo(publication.getDuplicateOf().toString())));
        assertEquals("no-cache", response.getHeaders().get(CACHE_CONTROL));
    }

    @Test
    void handlerReturnsGoneExceptionWhenQueryParameterDoNotRedirectIsSuppliedAndThePublicationIsADuplicate()
        throws ApiGatewayException, IOException {
        var duplicateOfIdentifier = UriWrapper.fromUri(randomUri())
                                        .addChild(SortableIdentifier.next().toString())
                                        .getUri();
        var publication = createDeletedPublicationWithDuplicate(duplicateOfIdentifier);
        var headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        var queryParams = Map.of(DO_NOT_REDIRECT_QUERY_PARAM, "true");
        var handlerRequest = generateHandlerRequest(publication.getIdentifier().toString(), headers, queryParams);
        fetchPublicationHandler.handleRequest(handlerRequest, output, context);
        var expectedTombstone = PublicationResponseElevatedUser.fromPublication(
            publication.copy().withAssociatedArtifacts(List.of()).build());
        var gatewayResponse = parseFailureResponse();
        var problem = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(), Problem.class);
        var actualPublication = JsonUtils.dtoObjectMapper.convertValue(problem.getParameters().get(RESOURCE),
                                                                       PublicationResponseElevatedUser.class);
        assertThat(actualPublication, is(equalTo(expectedTombstone)));
    }

    @Test
    void handlerReturnsNotFoundWhenRequestingPublicationWithPublicationStatusNotSupportedByHandler()
        throws ApiGatewayException, IOException {
        var publication = createDraftForDeletion();
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var gatewayResponse = parseFailureResponse();

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    @Test
    void curatorWithTheSameCustomerShouldHaveAccessToDraftPublication() throws ApiGatewayException, IOException {
        var publication = createPublication();
        fetchPublicationHandler.handleRequest(generateCuratorRequest(publication), output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void publicationOwnerShouldHaveAccessToDraftPublication() throws ApiGatewayException, IOException {
        var publication = createPublication();
        fetchPublicationHandler.handleRequest(generateOwnerRequest(publication), output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void shouldReturnOkAllowedOperationWhenReturningTombstoneAndUserHasAccessRightToRepublish(
        WireMockRuntimeInfo wireMockRuntimeInfo) throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublication(wireMockRuntimeInfo);
        createCustomerMock(publication.getPublisher());
        fetchPublicationHandler.handleRequest(editorRequestsPublication(publication), output, context);
        var gatewayResponse = parseHandlerResponse();
        var publicationResponse = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(),
                                                                      PublicationResponse.class);

        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(publicationResponse.getAllowedOperations(), hasItem(PublicationOperation.REPUBLISH));
    }

    @Test
    void shouldReturnPublicationWithInternalFilesWhenUserIsOwner() throws ApiGatewayException, IOException {
        var publication = createPublicationWithNonPublicFilesOnly(false);
        fetchPublicationHandler.handleRequest(generateOwnerRequest(publication), output, context);
        var gatewayResponse = parseHandlerResponse();

        var publicationResponse = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(),
                                                                      PublicationResponseElevatedUser.class);

        var artifacts = publicationResponse.getAssociatedArtifacts();

        assertFalse(artifacts.isEmpty());
        assertFalse(artifacts.stream().anyMatch(artifact -> artifact.getArtifactType().equals(HiddenFile.TYPE)));
        assertTrue(artifacts.stream().anyMatch(artifact -> artifact.getArtifactType().equals(InternalFile.TYPE)));
    }

    @Test
    void shouldReturnPublicationWithoutNonPublicFilesWhenNoAccess() throws ApiGatewayException, IOException {
        var publication = createPublicationWithNonPublicFilesOnly(false);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var gatewayResponse = parseHandlerResponse();

        var publicationResponse = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(),
                                                                      PublicationResponse.class);

        assertTrue(publicationResponse.getAssociatedArtifacts().isEmpty());
    }

    @Test
    void shouldReturnPublicationWithHiddenFilesWhenUserIsCuratorAndPublicationIsNonDegree()
        throws ApiGatewayException, IOException {
        var publication = createPublicationWithNonPublicFilesOnly(false);
        fetchPublicationHandler.handleRequest(generateCuratorRequest(publication), output, context);
        var gatewayResponse = parseHandlerResponse();

        var publicationResponse = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(),
                                                                      PublicationResponseElevatedUser.class);

        var artifacts = publicationResponse.getAssociatedArtifacts().stream().toList();

        assertTrue(artifacts.stream().anyMatch(artifact -> artifact.getArtifactType().equals(InternalFile.TYPE)));
        assertTrue(artifacts.stream().anyMatch(artifact -> artifact.getArtifactType().equals(HiddenFile.TYPE)));
    }

    @Test
    void shouldReturnPublicationWithHiddenFilesWhenUserIsThesisCuratorAndPublicationIsDegree()
        throws ApiGatewayException, IOException {
        var publication = createPublicationWithNonPublicFilesOnly(true);
        fetchPublicationHandler.handleRequest(generateCuratorRequest(publication), output, context);
        var gatewayResponse = parseHandlerResponse();

        var publicationResponse = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(),
                                                                      PublicationResponseElevatedUser.class);

        var artifacts = publicationResponse.getAssociatedArtifacts().stream().toList();

        assertTrue(artifacts.stream().anyMatch(artifact -> artifact.getArtifactType().equals(InternalFile.TYPE)));
        assertTrue(artifacts.stream().anyMatch(artifact -> artifact.getArtifactType().equals(HiddenFile.TYPE)));
    }

    @Test
    void handlerReturnsGoneWithPublicationDetailWhenPublicationIsUnpublishedAndDuplicateOfValueIsNotPresent()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublicationWithDuplicate(null);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var gatewayResponse = parseFailureResponse();
        var expectedTombstone = PublicationResponseElevatedUser.fromPublication(
            publication.copy().withAssociatedArtifacts(List.of()).build());
        var problem = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(), Problem.class);
        var actualPublication = JsonUtils.dtoObjectMapper.convertValue(problem.getParameters().get(RESOURCE),
                                                                       PublicationResponseElevatedUser.class);
        assertThat(actualPublication, is(equalTo(expectedTombstone)));
    }

    @Test
    void shouldRedirectToDuplicateWhenPublicationIsUnpublishedAndHasRightsToUpdateUnpublishedResource()
        throws IOException, ApiGatewayException {
        var publication = createUnpublishedPublicationWithDuplicate(randomUri());
        fetchPublicationHandler.handleRequest(generateCuratorRequest(publication), output, context);
        var gatewayResponse = parseHandlerResponse();

        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_MOVED_PERM)));
    }

    @Test
    void shouldReturnUnpublishedPublicationWithFilesWhenUserHasRightsToUpdatedPublicationAndRedirectIsNotProvided()
        throws IOException, ApiGatewayException {
        var publication = createUnpublishedPublicationWithDuplicate(null);

        var userName = randomString();
        var request = generateCuratorRequestWithShouldNotRedirectPathParam(publication, userName);

        fetchPublicationHandler.handleRequest(request, output, context);

        var gatewayResponse = parseHandlerResponse();

        var publicationResponse = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(),
                                                                      PublicationResponseElevatedUser.class);

        var expectedEtag =
            ETag.create(userName,
                        Resource.fromPublication(publication).fetch(publicationService).orElseThrow().getVersion().toString());

        assertEquals(gatewayResponse.getHeaders().get(ETAG), expectedEtag.toString());
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(publicationResponse.getAssociatedArtifacts(), is(not(emptyIterable())));
    }

    @Test
    void shouldReturnPublicationVersionAsETagHeader() throws ApiGatewayException, IOException {
        var publication = createPublication();
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var gatewayResponse = parseHandlerResponse();
        var expectedEtag = Resource.fromPublication(publication).fetch(publicationService).orElseThrow().getVersion();

        assertEquals(ETAG, gatewayResponse.getHeaders().get(ACCESS_CONTROL_EXPOSE_HEADERS));
        assertEquals(String.valueOf(expectedEtag), gatewayResponse.getHeaders().get(ETAG));
    }

    @Test
    void shouldReturnNotModifiedWhenProvidingIfNoneMatchHeaderWithETagMatchingCurrentVersionOfPublication()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var version = Resource.fromPublication(publication).fetch(publicationService).orElseThrow().getVersion();

        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString(),
                                                                     Map.of(HttpHeaders.IF_NONE_MATCH,
                                                                            version.toString()), Map.of()),
                                              output,
                                              context);
        var gatewayResponse = parseHandlerResponse();

        assertNull(gatewayResponse.getBody());
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_MODIFIED)));
    }

    @Test
    void shouldReturnOkWithBodyWhenProvidingIfNoneMatchHeaderWithETagNotMatchingCurrentVersionOfPublication()
        throws ApiGatewayException, IOException {
        var publication = createPublication();

        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString(),
                                                                     Map.of(HttpHeaders.IF_NONE_MATCH,
                                                                            randomString(), ACCEPT, "application/json"),
                                                                     Map.of()),
                                              output,
                                              context);
        var gatewayResponse = parseHandlerResponse();

        assertNotNull(gatewayResponse.getBody());
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @Test
    void shouldReturnOkWithBodyWhenProvidingIfNoneMatchHeaderWithETagMatchingCurrentVersionOfPublicationIfUserIsAuthenticated()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var version = Resource.fromPublication(publication).fetch(publicationService).orElseThrow().getVersion();

        fetchPublicationHandler.handleRequest(generateAuthenticatedRequest(publication, Map.of(HttpHeaders.IF_NONE_MATCH, version.toString(), ACCEPT, "application/json")),
                                              output,
                                              context);
        var gatewayResponse = parseHandlerResponse();

        assertNotNull(gatewayResponse.getBody());
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    private static Organization createExpectedPublisher(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return new Organization.Builder().withId(
            URI.create(wireMockRuntimeInfo.getHttpsBaseUrl() + "/customer/" + randomUUID())).build();
    }

    private Publication createUnpublishedPublication(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws ApiGatewayException {
        var publication = fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES);
        publication.setPublisher(createExpectedPublisher(wireMockRuntimeInfo));
        publication.setDuplicateOf(null);
        var peristedPublication = publicationService.createPublication(UserInstance.fromPublication(publication),
                                                                       publication);
        Resource.fromPublication(peristedPublication).publish(publicationService, UserInstance.fromPublication(publication));
        var userInstance = UserInstance.fromPublication(publication);
        publicationService.unpublishPublication(peristedPublication, userInstance);
        return peristedPublication;
    }

    private Publication createPublicationWithPublisher(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        publication.setPublisher(createExpectedPublisher(wireMockRuntimeInfo));
        publication.setDuplicateOf(null);
        publication.setCuratingInstitutions(
            Set.of(new CuratingInstitution(RandomDataGenerator.randomUri(), Set.of(RandomDataGenerator.randomUri()))));
        return persistNewPublication(publication);
    }

    private Publication createDeletedPublicationWithDuplicate(URI duplicateOf) throws ApiGatewayException {
        var publication = createPublication();
        publicationService.updatePublication(publication.copy().withDuplicateOf(duplicateOf).build());
        publicationService.updatePublishedStatusToDeleted(publication.getIdentifier());
        return publicationService.getPublicationByIdentifier(publication.getIdentifier());
    }

    private InputStream generateCuratorRequestWithShouldNotRedirectPathParam(Publication publication, String userName) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper).withHeaders(
                Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType()))
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString(),
                                              "shouldNotRedirect", "true"))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.MANAGE_DOI,
                                     AccessRight.MANAGE_RESOURCES_STANDARD, AccessRight.MANAGE_PUBLISHING_REQUESTS,
                                     AccessRight.MANAGE_RESOURCE_FILES, AccessRight.MANAGE_DEGREE,
                                     AccessRight.MANAGE_DEGREE_EMBARGO)
                   .withUserName(userName)
                   .withTopLevelCristinOrgId(publication.getCuratingInstitutions().iterator().next().id())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream generateCuratorRequest(Publication publication) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper).withHeaders(
                Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType()))
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.MANAGE_DOI,
                                     AccessRight.MANAGE_RESOURCES_STANDARD, AccessRight.MANAGE_PUBLISHING_REQUESTS,
                                     AccessRight.MANAGE_RESOURCE_FILES, AccessRight.MANAGE_DEGREE,
                                     AccessRight.MANAGE_DEGREE_EMBARGO)
                   .withUserName(randomString())
                   .withTopLevelCristinOrgId(publication.getCuratingInstitutions().iterator().next().id())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream generateOwnerRequest(Publication publication) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper).withHeaders(
                Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType()))
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(publication.getResourceOwner().getOwner().toString())
                   .build();
    }

    private InputStream generateAuthenticatedRequest(Publication publication, Map<String, String> headers) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withHeaders(headers)
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .withAccessRights(randomUri(), AccessRight.MANAGE_OWN_RESOURCES)
                   .build();
    }

    private Publication createUnpublishedPublicationWithDuplicate(URI duplicateOf) throws ApiGatewayException {
        var publication = createNondegreePublication();
        publicationService.updatePublication(publication.copy().withDuplicateOf(duplicateOf).build());
        Resource.fromPublication(publication).publish(publicationService, UserInstance.fromPublication(publication));
        var publishedPublication = publicationService.getPublicationByIdentifier(publication.getIdentifier());
        var userInstance = UserInstance.fromPublication(publication);
        publicationService.unpublishPublication(publishedPublication, userInstance);
        return publicationService.getPublicationByIdentifier(publication.getIdentifier());
    }

    private GatewayResponse<PublicationResponse> parseHandlerResponse() throws JsonProcessingException {
        return restApiMapper.readValue(output.toString(), PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
    }

    private InputStream generateHandlerRequest(String publicationIdentifier, Map<String, String> headers,
                                               Map<String, String> queryParams) throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier);
        return new HandlerRequestBuilder<InputStream>(restApiMapper).withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .withQueryParameters(queryParams)
                   .build();
    }

    private InputStream editorRequestsPublication(Publication publication) throws JsonProcessingException {
        var publicationIdentifier = publication.getIdentifier().toString();
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier);
        return new HandlerRequestBuilder<InputStream>(restApiMapper).withAccessRights(
                publication.getPublisher().getId(), AccessRight.MANAGE_RESOURCES_ALL)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withTopLevelCristinOrgId(publication.getCuratingInstitutions().iterator().next().id())
                   .withUserName(randomString())
                   .withPersonCristinId(randomUri())
                   .withHeaders(Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType()))
                   .build();
    }

    private InputStream generateHandlerRequest(String publicationIdentifier) throws JsonProcessingException {
        Map<String, String> headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        return generateHandlerRequest(publicationIdentifier, headers, NO_QUERY_PARAMS);
    }

    private InputStream generateHandlerRequestWithMissingPathParameter() throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper).withHeaders(
            Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType())).build();
    }

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    private GatewayResponse<Problem> parseFailureResponse() throws JsonProcessingException {
        JavaType responseWithProblemType = restApiMapper.getTypeFactory()
                                               .constructParametricType(GatewayResponse.class, Problem.class);
        return restApiMapper.readValue(output.toString(), responseWithProblemType);
    }

    private Publication createPublication() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        return persistNewPublication(publication);
    }

    private Publication persistNewPublication(Publication publication) throws BadRequestException, NotFoundException {
        var userInstance = UserInstance.fromPublication(publication);
        var publicationIdentifier =
            Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createNondegreePublication() throws ApiGatewayException {
        var publication = PublicationGenerator.randomNonDegreePublication();
        return persistNewPublication(publication);
    }

    private Publication createPublicationWithNonPublicFilesOnly(boolean isDegree) throws ApiGatewayException {
        var publication = isDegree ? randomDegreePublication() : randomNonDegreePublication();
        publication.setAssociatedArtifacts(
            new AssociatedArtifactList(randomPendingInternalFile(), randomInternalFile(), randomHiddenFile()));
        return persistNewPublication(publication);
    }

    private Publication createPublication(Class<? extends PublicationInstance<?>> instance) throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication(instance);
        return persistNewPublication(publication);
    }

    private Publication createDraftForDeletion() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = Resource.fromPublication(publication).persistNew(publicationService, userInstance);
        return publicationService.markPublicationForDeletion(userInstance, persistedPublication.getIdentifier());
    }

    private void createCustomerMock(Organization organization) {
        var customer = new Customer(organization.getId(), PUBLISHER_NAME);
        var response = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(customer)).orElseThrow();
        var id = UriWrapper.fromUri(organization.getId()).getLastPathElement();
        stubFor(WireMock.get(urlPathEqualTo("/customer/" + id))
                    .willReturn(WireMock.ok().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }
}

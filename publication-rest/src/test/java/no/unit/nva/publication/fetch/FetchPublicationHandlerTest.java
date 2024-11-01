package no.unit.nva.publication.fetch;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.LOCATION;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.util.UUID.randomUUID;
import static no.unit.nva.PublicationUtil.PROTECTED_DEGREE_INSTANCE_TYPES;
import static no.unit.nva.model.testing.PublicationGenerator.fromInstanceClassesExcluding;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomInternalFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.DO_NOT_REDIRECT_QUERY_PARAM;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ENV_NAME_NVA_FRONTEND_DOMAIN;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static nva.commons.apigateway.ApiGatewayHandler.RESOURCE;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.Customer;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ReadResourceService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
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
    public static final String DATACITE_XML_RESOURCE_ELEMENT = "<resource xmlns=\"http://datacite"
                                                               + ".org/schema/kernel-4\">";
    public static final String PUBLISHER_NAME = "publisher name";
    private static final Map<String, String> NO_QUERY_PARAMS = Map.of();
    private static final String TEXT_ANY = "text/*";
    private static final String TEXT_HTML = "text/html";
    private static final String APPLICATION_XHTML = "application/xhtml+xml";
    private static final String FIREFOX_DEFAULT_ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0"
                                                                + ".9,image/avif,image/webp,*/*;q=0.8";
    private static final String WEBKIT_DEFAULT_ACCEPT_HEADER = "application/xml,application/xhtml+xml,text/html;q=0"
                                                               + ".9,text/plain;q=0.8,image/png,*/*;q=0.5";
    private static final String IDENTIFIER_NULL_ERROR = "Identifier is not a valid UUID: null";
    private final Context context = new FakeContext();
    private ResourceService publicationService;
    private UriRetriever uriRetriever;
    private ByteArrayOutputStream output;
    private FetchPublicationHandler fetchPublicationHandler;
    private Environment environment;
    private IdentityServiceClient identityServiceClient;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp(@Mock Environment environment, @Mock IdentityServiceClient identityServiceClient) {
        super.init();
        this.environment = environment;
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        lenient().when(environment.readEnv(ENV_NAME_NVA_FRONTEND_DOMAIN)).thenReturn("localhost");

        publicationService = getResourceServiceBuilder().build();
        output = new ByteArrayOutputStream();
        this.uriRetriever = new UriRetriever(WiremockHttpClient.create());
        fetchPublicationHandler = new FetchPublicationHandler(publicationService,
                                                              uriRetriever,
                                                              environment,
                                                              identityServiceClient,
                                                              mock(HttpClient.class));
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    void handlerReturnsOkResponseOnValidInput() throws IOException, ApiGatewayException {
        var publication = createPublication();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var publicationIdentifier = publication.getIdentifier().toString();

        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier), output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void shouldReturnOkResponseWithDataCiteXmlBodyOnValidInput(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws IOException, ApiGatewayException {
        var publication = createPublicationWithPublisher(wireMockRuntimeInfo);
        var publicationIdentifier = publication.getIdentifier().toString();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var headers = Map.of(HttpHeaders.ACCEPT, MediaTypes.APPLICATION_DATACITE_XML.toString());
        createCustomerMock(publication.getPublisher());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier, headers, NO_QUERY_PARAMS),
                                              output,
                                              context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertEquals(MediaTypes.APPLICATION_DATACITE_XML.toString(), gatewayResponse.getHeaders().get(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertTrue(gatewayResponse.getBody().contains(DATACITE_XML_RESOURCE_ELEMENT));
    }

    // TODO: Extend beyond JournalArticle
    @Test
    void shouldReturnSchemaOrgProfileWhenSchemaOrgMediaTypeIsRequested() throws IOException,
                                                                                ApiGatewayException {
        var publication = createPublication(JournalArticle.class);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var identifier = publication.getIdentifier().toString();
        var headers = Map.of(ACCEPT, MediaTypes.SCHEMA_ORG.toString());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(identifier, headers, NO_QUERY_PARAMS), output,
                                              context);
        var gatewayResponse = parseHandlerResponse();
        var contentType = gatewayResponse.getHeaders().get(CONTENT_TYPE);
        assertThat(contentType, is(equalTo(MediaTypes.SCHEMA_ORG.toString())));
        assertThat(gatewayResponse.getBody(), containsString("\"@vocab\" : \"https://schema.org/\""));
    }

    @Test
    void shouldReturnAllowedOperations(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws ApiGatewayException, IOException {
        var publication = createPublicationWithPublisher(wireMockRuntimeInfo);
        var publicationIdentifier = publication.getIdentifier().toString();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        createCustomerMock(publication.getPublisher());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier),
                                              output,
                                              context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getBody(), containsString("allowedOperations"));
    }

    @ParameterizedTest(name = "should redirect to frontend landing page when accept header is {0}")
    @ValueSource(strings = {
        TEXT_HTML,
        APPLICATION_XHTML,
        TEXT_ANY,
        FIREFOX_DEFAULT_ACCEPT_HEADER,
        WEBKIT_DEFAULT_ACCEPT_HEADER
    })
    void shouldRedirectToFrontendLandingPageIfPreferredContentTypeIsHtml(String acceptHeaderValue)
        throws ApiGatewayException, IOException {
        var publication = createPublication(JournalArticle.class);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var identifier = publication.getIdentifier().toString();
        var headers = Map.of(ACCEPT, acceptHeaderValue);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(identifier, headers, NO_QUERY_PARAMS), output,
                                              context);

        var valueType = restApiMapper.getTypeFactory()
                            .constructParametricType(
                                GatewayResponse.class,
                                Void.class);

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
        var inputStream = new HandlerRequestBuilder<InputStream>(restApiMapper)
                              .withBody(null)
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
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        var serviceThrowingException = spy(publicationService);
        doThrow(new NullPointerException())
            .when(serviceThrowingException)
            .getPublicationByIdentifier(any(SortableIdentifier.class));

        fetchPublicationHandler = new FetchPublicationHandler(serviceThrowingException,
                                                              uriRetriever,
                                                              environment,
                                                              identityServiceClient,
                                                              mock(HttpClient.class));
        fetchPublicationHandler.handleRequest(generateHandlerRequest(IDENTIFIER_VALUE), output, context);

        var gatewayResponse = parseFailureResponse();
        var actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    void handlerReturnsGoneWithPublicationDetailWhenPublicationIsUnpublishedAndDuplicateOfValueIsNotPresent()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublicationWithDuplicate(null);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var gatewayResponse = parseFailureResponse();
        var expectedTombstone =
            PublicationResponseElevatedUser.fromPublication(publication.copy()
                                                                .withAssociatedArtifacts(List.of()).build());
        var problem = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(), Problem.class);
        var actualPublication = JsonUtils.dtoObjectMapper.convertValue(problem.getParameters().get(RESOURCE),
                                                                       PublicationResponseElevatedUser.class);
        assertThat(actualPublication, is(equalTo(expectedTombstone)));
    }

    @Test
    void handlerReturnsGoneWithPublicationDetailWhenPublicationIsDeletedAndDuplicateOfValueIsNotPresent()
        throws IOException, ApiGatewayException {
        var publication = createDeletedPublicationWithDuplicate(null);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var gatewayResponse = parseFailureResponse();
        var expectedTombstone =
            PublicationResponseElevatedUser.fromPublication(publication.copy()
                                                                .withAssociatedArtifacts(List.of()).build());

        var problem = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(), Problem.class);
        var actualPublication = JsonUtils.dtoObjectMapper.convertValue(problem.getParameters().get(RESOURCE),
                                                                       PublicationResponseElevatedUser.class);

        assertThat(actualPublication, is(equalTo(expectedTombstone)));
    }

    @Test
    void handlerRedirectToDuplicatePublicationWhenDeletedPublicationHasDuplicate()
        throws ApiGatewayException, IOException {
        var duplicateOfIdentifier =
            UriWrapper.fromUri(randomUri()).addChild(SortableIdentifier.next().toString()).getUri();
        var publication = createDeletedPublicationWithDuplicate(duplicateOfIdentifier);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output,
                                              context);
        var valueType = restApiMapper.getTypeFactory()
                            .constructParametricType(
                                GatewayResponse.class,
                                Void.class);

        GatewayResponse<Void> response = restApiMapper.readValue(output.toString(), valueType);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_MOVED_PERM)));

        assertThat(response.getHeaders().get(LOCATION), is(equalTo(publication.getDuplicateOf().toString())));
        assertEquals("no-cache", response.getHeaders().get(CACHE_CONTROL));
    }

    @Test
    void handlerReturnsGoneExceptionWhenQueryParameterDoNotRedirectIsSuppliedAndThePublicationIsADuplicate()
        throws ApiGatewayException, IOException {
        var duplicateOfIdentifier =
            UriWrapper.fromUri(randomUri()).addChild(SortableIdentifier.next().toString()).getUri();
        var publication = createDeletedPublicationWithDuplicate(duplicateOfIdentifier);
        var headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        var queryParams = Map.of(DO_NOT_REDIRECT_QUERY_PARAM, "true");
        var handlerRequest = generateHandlerRequest(publication.getIdentifier().toString(),
                                                    headers,
                                                    queryParams);
        fetchPublicationHandler.handleRequest(handlerRequest,
                                              output,
                                              context);
        var expectedTombstone =
            PublicationResponseElevatedUser.fromPublication(publication.copy()
                                                                .withAssociatedArtifacts(List.of()).build());
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
    void shouldReturnRepublishAllowedOperationWhenReturningTombstoneAndUserHasAccessRightToRepublish(
        WireMockRuntimeInfo wireMockRuntimeInfo)
        throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublication(wireMockRuntimeInfo);
        createCustomerMock(publication.getPublisher());
        fetchPublicationHandler.handleRequest(editorRequestsPublication(publication), output, context);
        var gatewayResponse = parseFailureResponse();
        var problem = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(), Problem.class);
        var actualPublication = JsonUtils.dtoObjectMapper.convertValue(problem.getParameters().get(RESOURCE),
                                                                       PublicationResponseElevatedUser.class);

        assertThat(actualPublication.getAllowedOperations(), hasItem(PublicationOperation.REPUBLISH));
    }

    @Test
    void shouldReturnPublicationWithInternalFilesWhenUserIsAllowedToUpdatePublication()
        throws ApiGatewayException, IOException {
        var publication = createPublicationWithInternalFilesOnly();
        fetchPublicationHandler.handleRequest(generateOwnerRequest(publication), output, context);
        var gatewayResponse = parseHandlerResponse();

        var publicationResponse = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(), PublicationResponseElevatedUser.class);

        assertFalse(publicationResponse.getAssociatedArtifacts().isEmpty());
    }

    @Test
    void shouldReturnPublicationWithOutInternalFilesWhenUserIsNotAllowedToUpdatePublication()
        throws ApiGatewayException, IOException {
        var publication = createPublicationWithInternalFilesOnly();
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication.getIdentifier().toString()), output, context);
        var gatewayResponse = parseHandlerResponse();

        var publicationResponse = JsonUtils.dtoObjectMapper.readValue(gatewayResponse.getBody(), PublicationResponse.class);

        assertTrue(publicationResponse.getAssociatedArtifacts().isEmpty());
    }

    private Publication createUnpublishedPublication(WireMockRuntimeInfo wireMockRuntimeInfo) throws ApiGatewayException {
        var publication = fromInstanceClassesExcluding(PROTECTED_DEGREE_INSTANCE_TYPES);
        publication.setPublisher(createExpectedPublisher(wireMockRuntimeInfo));
        publication.setDuplicateOf(null);
        var peristedPublication = publicationService.createPublication(UserInstance.fromPublication(publication),
                                                                 publication);
        publicationService.publishPublication(UserInstance.fromPublication(publication), peristedPublication.getIdentifier());
        publicationService.unpublishPublication(peristedPublication);
        return peristedPublication;
    }

    private static Organization createExpectedPublisher(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return new Organization.Builder().withId(
            URI.create(wireMockRuntimeInfo.getHttpsBaseUrl() + "/customer/" + randomUUID())).build();
    }

    private Publication createPublicationWithPublisher(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        publication.setPublisher(createExpectedPublisher(wireMockRuntimeInfo));
        publication.setDuplicateOf(null);
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(RandomDataGenerator.randomUri(), Set.of(
            RandomDataGenerator.randomUri()))));
        var userInstance = UserInstance.fromPublication(publication);
        var publicationIdentifier =
            Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createDeletedPublicationWithDuplicate(URI duplicateOf) throws ApiGatewayException {
        var publication = createPublication();
        publicationService.updatePublication(publication.copy().withDuplicateOf(duplicateOf).build());
        publicationService.updatePublishedStatusToDeleted(publication.getIdentifier());
        return publicationService.getPublication(publication);
    }

    private InputStream generateCuratorRequest(Publication publication) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withHeaders(Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType()))
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.MANAGE_DOI)
                   .build();
    }

    private InputStream generateOwnerRequest(Publication publication) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withHeaders(Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType()))
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(publication.getResourceOwner().getOwner().toString())
                   .build();
    }

    private Publication createUnpublishedPublicationWithDuplicate(URI duplicateOf) throws ApiGatewayException {
        var publication = createPublication();
        publicationService.updatePublication(publication.copy().withDuplicateOf(duplicateOf).build());
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var publishedPublication = publicationService.getPublication(publication);
        publicationService.unpublishPublication(publishedPublication);
        return publicationService.getPublication(publication);
    }

    private GatewayResponse<PublicationResponse> parseHandlerResponse() throws JsonProcessingException {
        return restApiMapper.readValue(output.toString(), PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
    }

    private InputStream generateHandlerRequest(String publicationIdentifier,
                                               Map<String, String> headers,
                                               Map<String, String> queryParams)
        throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier);
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .withQueryParameters(queryParams)
                   .build();
    }

    private InputStream editorRequestsPublication(Publication publication)
        throws JsonProcessingException {
        var publicationIdentifier = publication.getIdentifier().toString();
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier);
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.MANAGE_RESOURCES_ALL)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withTopLevelCristinOrgId(publication.getCuratingInstitutions().iterator().next().id())
                   .withUserName(randomString())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream generateHandlerRequest(String publicationIdentifier) throws JsonProcessingException {
        Map<String, String> headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        return generateHandlerRequest(publicationIdentifier, headers, NO_QUERY_PARAMS);
    }

    private InputStream generateHandlerRequestWithMissingPathParameter() throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withHeaders(Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType()))
                   .build();
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
        Publication publication = PublicationGenerator.randomPublication();
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
            Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createPublicationWithInternalFilesOnly() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(randomPendingInternalFile(), randomInternalFile()));
        var userInstance = UserInstance.fromPublication(publication);
        var publicationIdentifier = Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createPublication(Class<? extends PublicationInstance<?>> instance) throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication(instance);
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
            Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
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

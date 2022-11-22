package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.util.Collections.singletonMap;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.testutils.HandlerRequestBuilder.SCOPE_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static nva.commons.apigateway.RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ReadResourceService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.RequestInfoConstants;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

class FetchPublicationHandlerTest extends ResourcesLocalTest {

    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_TYPE = restApiMapper.getTypeFactory()
                                                                           .constructParametricType(
                                                                               GatewayResponse.class,
                                                                               PublicationResponse.class);
    public static final String DATACITE_XML_RESOURCE_ELEMENT = "<resource xmlns=\"http://datacite"
                                                               + ".org/schema/kernel-4\">";
    private static final String IDENTIFIER_NULL_ERROR = "Identifier is not a valid UUID: null";
    private ResourceService publicationService;
    private Context context;

    private ByteArrayOutputStream output;
    private FetchPublicationHandler fetchPublicationHandler;
    private Environment environment;

    public static Stream<String> mediaTypeProvider() {
        return Stream.of(
            MediaTypes.APPLICATION_JSON_LD.toString(),
            MediaTypes.APPLICATION_DATACITE_XML.toString(),
            ContentType.APPLICATION_JSON.getMimeType(),
            MediaTypes.SCHEMA_ORG.toString()
        );
    }

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        super.init();
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");

        publicationService = new ResourceService(client, Clock.systemDefaultZone());
        context = mock(Context.class);
        output = new ByteArrayOutputStream();
        fetchPublicationHandler = new FetchPublicationHandler(publicationService, environment);
    }

    // TODO: Draft for deletion should probably not be returned.
    @ParameterizedTest(name = "Should return publication when requester is owner and status is {0}")
    @EnumSource(value = PublicationStatus.class)
    void shouldReturnPublicationWhenUserIsOwner(PublicationStatus status) throws IOException, ApiGatewayException {
        var publication = createPublication(status);
        fetchPublicationHandler.handleRequest(generateFetchPublicationRequestForOwner(publication), output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // TODO: Draft for deletion should probably not be returned.
    @ParameterizedTest(name = "Should return publication when requester is curator and status is {0}")
    @EnumSource(value = PublicationStatus.class)
    void shouldReturnPublicationWhenUserIsCurator(PublicationStatus status) throws IOException, ApiGatewayException {
        var publication = createPublication(status);
        var input = generateFetchPublicationRequestForCuratorForSameInstitutionAsOwner(publication);
        fetchPublicationHandler.handleRequest(input, output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void shouldReturnOkResponseWhenOwnerRequestsOwnDraftPublication()
        throws IOException, ApiGatewayException {
        var publication = createDraftPublication();
        var request = generateFetchPublicationRequestForOwner(publication);
        fetchPublicationHandler.handleRequest(request, output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenNonOwnerRequestsDraftPublicationBelongingToAnotherUser()
        throws IOException, ApiGatewayException {
        var publication = createDraftPublication();
        var request = generateFetchPublicationRequestForRandomUser(publication);
        fetchPublicationHandler.handleRequest(request, output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldReturnOkResponseWithDataCiteXmlBodyWhenUserIsAuthenticated() throws IOException, ApiGatewayException {
        var publication = createPublication(PublicationStatus.DRAFT);

        var headers = Map.of(HttpHeaders.ACCEPT, MediaTypes.APPLICATION_DATACITE_XML.toString());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication, headers), output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertEquals(MediaTypes.APPLICATION_DATACITE_XML.toString(), gatewayResponse.getHeaders().get(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertTrue(gatewayResponse.getBody().contains(DATACITE_XML_RESOURCE_ELEMENT));
    }

    @ParameterizedTest
    @MethodSource("mediaTypeProvider")
    void shouldReturnResponseWhenBackendUserRequestsResource(String mediaType) throws ApiGatewayException, IOException {
        var publication = createPublication(PublicationStatus.PUBLISHED);
        var input = attempt(() -> new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withAuthorizerClaim(SCOPE_CLAIM, BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)
                                      .withPathParameters(
                                          singletonMap(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                                      .withHeaders(Map.of(ACCEPT, mediaType))
                                      .build()).orElseThrow();
        fetchPublicationHandler.handleRequest(input, output, context);
        var gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
    }

    // TODO: Extend beyond JournalArticle
    @Test
    void shouldReturnSchemaOrgProfileWhenSchemaOrgMediaTypeIsRequested() throws IOException,
                                                                                ApiGatewayException {
        var publication = createPublication(JournalArticle.class);
        var headers = Map.of(ACCEPT, MediaTypes.SCHEMA_ORG.toString());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication, headers), output, context);
        var gatewayResponse = parseHandlerResponse();
        var contentType = gatewayResponse.getHeaders().get(CONTENT_TYPE);
        assertThat(contentType, is(equalTo(MediaTypes.SCHEMA_ORG.toString())));
        assertThat(gatewayResponse.getBody(), containsString("\"@vocab\" : \"https://schema.org/\""));
    }

    @Test
    void shouldReturnNotFoundResponseWhenPublicationDoesNotExist() throws IOException {
        var publication = createUnpersistedDraftPublication();
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publication), output, context);
        var gatewayResponse = parseFailureResponse();

        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        var actualDetail = getProblemDetail(gatewayResponse);
        assertThat(actualDetail, containsString(ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE));
        assertThat(actualDetail, containsString(publication.getIdentifier().toString()));
    }

    @Test
    void shouldReturnBadRequestResponseWhenInputIsEmpty() throws IOException {
        var input = new HandlerRequestBuilder<InputStream>(restApiMapper)
                        .withBody(null)
                        .withHeaders(null)
                        .withPathParameters(null)
                        .build();
        fetchPublicationHandler.handleRequest(input, output, context);
        var gatewayResponse = parseFailureResponse();
        var actualDetail = gatewayResponse.getBodyObject(Problem.class).getDetail();
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(IDENTIFIER_NULL_ERROR));
    }

    @Test
    void shouldReturnBadRequestResponseWhenPathParamIsEmpty() throws IOException {
        var inputStream = generateHandlerRequestWithMissingPathParameter();
        fetchPublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseFailureResponse();
        var actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(IDENTIFIER_NULL_ERROR));
    }

    @Test
    void shouldReturnInternalServerErrorResponseOnUnexpectedExceptionWhenRequesterIsAuthenticated()
        throws IOException, ApiGatewayException {
        var serviceThrowingException = spy(publicationService);
        doThrow(new NullPointerException())
            .when(serviceThrowingException)
            .getPublicationByIdentifier(any(SortableIdentifier.class));

        fetchPublicationHandler = new FetchPublicationHandler(serviceThrowingException, environment);
        var input = generateFetchPublicationRequestForOwner(createUnpersistedDraftPublication());
        fetchPublicationHandler.handleRequest(input, output, context);

        var gatewayResponse = parseFailureResponse();
        var actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    void shouldReturnInternalServerErrorResponseOnUnexpectedExceptionWhenRequesterIsUnauthenticated()
        throws IOException, ApiGatewayException {
        var serviceThrowingException = spy(publicationService);
        doThrow(new NullPointerException())
            .when(serviceThrowingException)
            .getPublicationByIdentifier(any(SortableIdentifier.class));

        fetchPublicationHandler = new FetchPublicationHandler(serviceThrowingException, environment);
        var identifier = createUnpersistedDraftPublication().getIdentifier();
        var input = generateFetchPublicationRequestForAnonymousUser(identifier);
        fetchPublicationHandler.handleRequest(input, output, context);

        var gatewayResponse = parseFailureResponse();
        var actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    private InputStream generateFetchPublicationRequestForRandomUser(Publication publication) {
        var user = UserInstance.create(randomString(), randomUri());
        return generateFetchPublicationRequestForUser(user, publication.getIdentifier());
    }

    private InputStream generateFetchPublicationRequestForAnonymousUser(SortableIdentifier identifier) {
        return attempt(() -> new HandlerRequestBuilder<Publication>(restApiMapper)
                                 .withHeaders(TestHeaders.getRequestHeaders())
                                 .withPathParameters(
                                     singletonMap(PUBLICATION_IDENTIFIER, identifier.toString()))
                                 .build()).orElseThrow();
    }

    private static InputStream generateFetchPublicationRequestForOwner(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        var identifier = publication.getIdentifier();

        return generateFetchPublicationRequestForUser(userInstance, identifier);
    }

    private InputStream generateFetchPublicationRequestForCuratorForSameInstitutionAsOwner(Publication publication) {
        var ownerOrganization = UserInstance.fromPublication(publication).getOrganizationUri();
        return attempt(() -> new HandlerRequestBuilder<Publication>(restApiMapper)
                                 .withHeaders(TestHeaders.getRequestHeaders())
                                 .withPathParameters(
                                     singletonMap(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                                 .withUserName(randomString())
                                 .withCurrentCustomer(ownerOrganization)
                                 .withAccessRights(ownerOrganization,
                                                   AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.toString())
                                 .build()).orElseThrow();
    }

    private static InputStream generateFetchPublicationRequestForUser(UserInstance user,
                                                                      SortableIdentifier identifier) {
        return attempt(() -> new HandlerRequestBuilder<Publication>(restApiMapper)
                                 .withHeaders(TestHeaders.getRequestHeaders())
                                 .withPathParameters(
                                     singletonMap(PUBLICATION_IDENTIFIER, identifier.toString()))
                                 .withUserName(user.getUsername())
                                 .withCurrentCustomer(user.getOrganizationUri())
                                 .build()).orElseThrow();
    }

    private GatewayResponse<PublicationResponse> parseHandlerResponse() throws JsonProcessingException {
        return restApiMapper.readValue(output.toString(), PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
    }

    private InputStream generateHandlerRequest(Publication publication, Map<String, String> headers)
        throws JsonProcessingException {
        var currentCustomer = UserInstance.fromPublication(publication).getOrganizationUri();
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString());
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withUserName(randomString())
                   .withCurrentCustomer(currentCustomer)
                   .withAccessRights(currentCustomer, AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.toString())
                   .withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .build();
    }

    private InputStream generateHandlerRequest(Publication publication) throws JsonProcessingException {
        var headers = Map.of(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return generateHandlerRequest(publication, headers);
    }

    private InputStream generateHandlerRequestWithMissingPathParameter() throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withHeaders(Map.of(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()))
                   .build();
    }

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    private GatewayResponse<Problem> parseFailureResponse() throws JsonProcessingException {
        var responseWithProblemType = restApiMapper.getTypeFactory()
                                          .constructParametricType(GatewayResponse.class, Problem.class);
        return restApiMapper.readValue(output.toString(), responseWithProblemType);
    }

    private Publication createUnpersistedDraftPublication() {
        return randomPublication().copy().withDoi(null).withStatus(PublicationStatus.DRAFT).build();
    }

    private Publication createPublication(PublicationStatus status) throws ApiGatewayException {
        var publication = randomPublication().copy().withStatus(status).build();
        var userInstance = UserInstance.fromPublication(publication);
        var publicationIdentifier =
            Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createDraftPublication() throws ApiGatewayException {
        var publication = randomPublication().copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .build();
        var userInstance = UserInstance.fromPublication(publication);
        var publicationIdentifier =
            Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createPublication(Class<? extends PublicationInstance<?>> instance) throws ApiGatewayException {
        var publication = randomPublication(instance);
        var userInstance = UserInstance.fromPublication(publication);
        var publicationIdentifier =
            Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }
}

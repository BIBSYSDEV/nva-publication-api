package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.GONE_MESSAFE;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_GONE;
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
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ReadResourceService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class FetchPublicationHandlerTest extends ResourcesLocalTest {
    
    public static final String IDENTIFIER_VALUE = "0ea0dd31-c202-4bff-8521-afd42b1ad8db";
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

    // TODO: replace test with tests that assert unauth/non-owner/non-curator users do not receive unpublished files
    //  with published metadata and authenticated users do
    @Test
    void shouldReturnStatusPublishedForPublicationsWithStatusPublishedMetadata()
        throws ApiGatewayException, IOException {
        var publication = createPersistedPublicationWithStatusPublishedMetadata();
        assertThat(publication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED_METADATA)));
        var identifier = publication.getIdentifier().toString();
        fetchPublicationHandler.handleRequest(generateHandlerRequest(identifier), output, context);
        var response = parseHandlerResponse();
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));
        var body = response.getBodyObject(PublicationResponse.class);
        assertThat(body.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    void handlerReturnsOkResponseOnValidInput() throws IOException, ApiGatewayException {
        Publication createdPublication = createPublication();
        String publicationIdentifier = createdPublication.getIdentifier().toString();
        
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier), output, context);
        GatewayResponse<PublicationResponse> gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }
    
    @Test
    void shouldReturnOkResponseWithDataCiteXmlBodyOnValidInput() throws IOException, ApiGatewayException {
        var createdPublication = createPublication();
        var publicationIdentifier = createdPublication.getIdentifier().toString();
        
        var headers = Map.of(HttpHeaders.ACCEPT, MediaTypes.APPLICATION_DATACITE_XML.toString());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier, headers), output, context);
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
        var identifier = publication.getIdentifier().toString();
        var headers = Map.of(ACCEPT, MediaTypes.SCHEMA_ORG.toString());
        fetchPublicationHandler.handleRequest(generateHandlerRequest(identifier, headers), output, context);
        var gatewayResponse = parseHandlerResponse();
        var contentType = gatewayResponse.getHeaders().get(CONTENT_TYPE);
        assertThat(contentType, is(equalTo(MediaTypes.SCHEMA_ORG.toString())));
        assertThat(gatewayResponse.getBody(), containsString("\"@vocab\" : \"https://schema.org/\""));
    }
    
    @Test
    @DisplayName("handler Returns NotFound Response On Publication Missing")
    void handlerReturnsNotFoundResponseOnPublicationMissing() throws IOException {
        
        fetchPublicationHandler.handleRequest(generateHandlerRequest(IDENTIFIER_VALUE), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse();
        
        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        
        String actualDetail = getProblemDetail(gatewayResponse);
        assertThat(actualDetail, containsString(ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE));
        assertThat(actualDetail, containsString(IDENTIFIER_VALUE));
    }
    
    @Test
    @DisplayName("handler Returns BadRequest Response On Empty Input")
    void handlerReturnsBadRequestResponseOnEmptyInput() throws IOException {
        InputStream inputStream = new HandlerRequestBuilder<InputStream>(restApiMapper)
                                      .withBody(null)
                                      .withHeaders(null)
                                      .withPathParameters(null)
                                      .build();
        fetchPublicationHandler.handleRequest(inputStream, output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse();
        String actualDetail = gatewayResponse.getBodyObject(Problem.class).getDetail();
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(IDENTIFIER_NULL_ERROR));
    }
    
    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        InputStream inputStream = generateHandlerRequestWithMissingPathParameter();
        fetchPublicationHandler.handleRequest(inputStream, output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse();
        String actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(IDENTIFIER_NULL_ERROR));
    }
    
    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        ResourceService serviceThrowingException = spy(publicationService);
        doThrow(new NullPointerException())
            .when(serviceThrowingException)
            .getPublicationByIdentifier(any(SortableIdentifier.class));
        
        fetchPublicationHandler = new FetchPublicationHandler(serviceThrowingException, environment);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(IDENTIFIER_VALUE), output, context);
        
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse();
        String actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("Handler returns Gone Response when when publication has status Deleted")
    void handlerReturnsGoneErrorResponseWhenPublicationHasStatusDeleted()
        throws ApiGatewayException, IOException {
        var publicationIdentifier = createDeletedPublication();
        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse();
        String actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_GONE, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(GONE_MESSAFE));

    }

    private String createDeletedPublication() throws ApiGatewayException {
        var createdPublication = createPublication();
        var publicationIdentifier = createdPublication.getIdentifier();
        publicationService.updatePublishedStatusToDeleted(publicationIdentifier);
        return publicationIdentifier.toString();
    }

    private GatewayResponse<PublicationResponse> parseHandlerResponse() throws JsonProcessingException {
        return restApiMapper.readValue(output.toString(), PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
    }
    
    private InputStream generateHandlerRequest(String publicationIdentifier, Map<String, String> headers)
        throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier);
        return new HandlerRequestBuilder<InputStream>(restApiMapper)
                   .withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .build();
    }
    
    private InputStream generateHandlerRequest(String publicationIdentifier) throws JsonProcessingException {
        Map<String, String> headers = Map.of(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return generateHandlerRequest(publicationIdentifier, headers);
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

    private Publication createPersistedPublicationWithStatusPublishedMetadata() throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
            Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        publicationService.publishPublicationMetadata(userInstance, publicationIdentifier);
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createPublication(Class<? extends PublicationInstance<?>> instance) throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication(instance);
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
                Resource.fromPublication(publication).persistNew(publicationService, userInstance).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }
}

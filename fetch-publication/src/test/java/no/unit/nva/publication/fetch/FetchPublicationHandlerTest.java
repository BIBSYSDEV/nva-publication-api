package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.ReadResourceService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class FetchPublicationHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_VALUE = "0ea0dd31-c202-4bff-8521-afd42b1ad8db";
    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_TYPE = objectMapper.getTypeFactory()
                                                                           .constructParametricType(
                                                                               GatewayResponse.class,
                                                                               PublicationResponse.class);
    private static final String IDENTIFIER_NULL_ERROR = "Identifier is not a valid UUID: null";

    private ResourceService publicationService;
    private Context context;

    private ByteArrayOutputStream output;
    private FetchPublicationHandler fetchPublicationHandler;
    private Environment environment;
    private DoiRequestService doiRequestService;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        super.init();
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");

        publicationService = new ResourceService(client, Clock.systemDefaultZone());
        doiRequestService = new DoiRequestService(client, Clock.systemDefaultZone());
        context = mock(Context.class);
        output = new ByteArrayOutputStream();
        fetchPublicationHandler = new FetchPublicationHandler(publicationService, doiRequestService, environment);
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    public void handlerReturnsOkResponseOnValidInput() throws IOException, ApiGatewayException {
        Publication createdPublication = createPublication();
        String publicationIdentifier = createdPublication.getIdentifier().toString();

        fetchPublicationHandler.handleRequest(generateHandlerRequest(publicationIdentifier), output, context);
        GatewayResponse<PublicationResponse> gatewayResponse = parseHandlerResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("handler Returns NotFound Response On Publication Missing")
    public void handlerReturnsNotFoundResponseOnPublicationMissing() throws IOException {

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
    public void handlerReturnsBadRequestResponseOnEmptyInput() throws IOException {
        InputStream inputStream = new HandlerRequestBuilder<InputStream>(objectMapper)
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
    public void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        InputStream inputStream = generateHandlerRequestWithMissingPathParameter();
        fetchPublicationHandler.handleRequest(inputStream, output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse();
        String actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(IDENTIFIER_NULL_ERROR));
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        ResourceService serviceThrowingException = spy(publicationService);
        doThrow(new NullPointerException())
            .when(serviceThrowingException)
            .getPublicationByIdentifier(any(SortableIdentifier.class));

        fetchPublicationHandler = new FetchPublicationHandler(serviceThrowingException, doiRequestService, environment);
        fetchPublicationHandler.handleRequest(generateHandlerRequest(IDENTIFIER_VALUE), output, context);

        GatewayResponse<Problem> gatewayResponse = parseFailureResponse();
        String actualDetail = getProblemDetail(gatewayResponse);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(actualDetail, containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    public void handlerReturnsPublicationWithDoiRequestWhenDoiRequestIsPresent()
        throws NotFoundException, TransactionFailedException, BadRequestException, IOException {
        Publication createdPublication = createPublication();
        UserInstance resourceOwner = extractOwner(createdPublication);
        SortableIdentifier doiRequestIdentifier =
            doiRequestService.createDoiRequest(resourceOwner, createdPublication.getIdentifier());
        InputStream input = generateHandlerRequest(createdPublication.getIdentifier().toString());
        fetchPublicationHandler.handleRequest(input, output, context);
        GatewayResponse<PublicationResponse> response = GatewayResponse.fromOutputStream(output);
        PublicationResponse publicationDto = response.getBodyObject(PublicationResponse.class);

        DoiRequest actualDoiRequest = publicationDto.getDoiRequest();
        DoiRequest expectedDoiRequest = doiRequestService.getDoiRequest(resourceOwner, doiRequestIdentifier)
                                            .toPublication()
                                            .getDoiRequest();
        assertThat(actualDoiRequest, is(equalTo(expectedDoiRequest)));
    }

    private GatewayResponse<PublicationResponse> parseHandlerResponse() throws JsonProcessingException {
        return objectMapper.readValue(output.toString(), PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
    }

    private InputStream generateHandlerRequest(String publicationIdentifier) throws JsonProcessingException {
        Map<String, String> headers = Map.of(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        Map<String, String> pathParameters = Map.of(IDENTIFIER, publicationIdentifier);
        return new HandlerRequestBuilder<InputStream>(objectMapper)
                   .withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .build();
    }

    private InputStream generateHandlerRequestWithMissingPathParameter() throws JsonProcessingException {
        return new HandlerRequestBuilder<InputStream>(objectMapper)
            .withHeaders(Map.of(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()))
            .build();
    }

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    private GatewayResponse<Problem> parseFailureResponse() throws JsonProcessingException {
        JavaType responseWithProblemType = objectMapper.getTypeFactory()
                                               .constructParametricType(GatewayResponse.class, Problem.class);
        return objectMapper.readValue(output.toString(), responseWithProblemType);
    }

    private Publication createPublication() throws TransactionFailedException, NotFoundException {
        Publication publication = PublicationGenerator.publicationWithoutIdentifier();
        SortableIdentifier publicationIdentifier = publicationService.createPublication(publication).getIdentifier();
        return publicationService.getPublicationByIdentifier(publicationIdentifier);
    }
}

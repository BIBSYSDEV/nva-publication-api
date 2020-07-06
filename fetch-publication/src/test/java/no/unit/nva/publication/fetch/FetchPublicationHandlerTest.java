package no.unit.nva.publication.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.ErrorResponseException;
import no.unit.nva.publication.exception.NotFoundException;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static no.unit.nva.publication.fetch.FetchPublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.publication.fetch.FetchPublicationHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FetchPublicationHandlerTest {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_VALUE = "0ea0dd31-c202-4bff-8521-afd42b1ad8db";
    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_TYPE = objectMapper.getTypeFactory()
            .constructParametricType(GatewayResponse.class, PublicationResponse.class);
    private static final String IDENTIFIER_NULL_ERROR = "Identifier is not a valid UUID: null";

    private PublicationService publicationService;
    private Context context;

    private OutputStream output;
    private FetchPublicationHandler fetchPublicationHandler;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");

        publicationService = mock(PublicationService.class);
        context = mock(Context.class);

        output = new ByteArrayOutputStream();
        fetchPublicationHandler =
            new FetchPublicationHandler(publicationService, environment);
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    public void handlerReturnsOkResponseOnValidInput() throws IOException, ApiGatewayException {
        Publication publication = createPublication();
        when(publicationService.getPublication(any(UUID.class))).thenReturn(publication);

        fetchPublicationHandler.handleRequest(generateHandlerRequest(), output, context);

        GatewayResponse<PublicationResponse> gatewayResponse = objectMapper.readValue(output.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("handler Returns NotFound Response On Publication Missing")
    public void handlerReturnsNotFoundResponseOnPublicationMissing() throws IOException, ApiGatewayException {
        when(publicationService.getPublication(any(UUID.class))).thenThrow(new NotFoundException("Error"));

        fetchPublicationHandler.handleRequest(generateHandlerRequest(), output, context);

        GatewayResponse<PublicationResponse> gatewayResponse = objectMapper.readValue(output.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
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

        GatewayResponse<PublicationResponse> gatewayResponse = objectMapper.readValue(output.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getBody(), containsString(IDENTIFIER_NULL_ERROR));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    public void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        InputStream inputStream = new HandlerRequestBuilder<InputStream>(objectMapper)
                .withHeaders(Map.of(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()))
                .build();
        fetchPublicationHandler.handleRequest(inputStream, output, context);
        GatewayResponse<PublicationResponse> gatewayResponse = objectMapper.readValue(output.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getBody(), containsString(IDENTIFIER_NULL_ERROR));
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        when(publicationService.getPublication(any(UUID.class)))
            .thenThrow(new NullPointerException());

        fetchPublicationHandler.handleRequest(generateHandlerRequest(), output, context);

        GatewayResponse<PublicationResponse> gatewayResponse = objectMapper.readValue(output.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns BadGateway Response On Communication Problems")
    public void handlerReturnsBadGatewayResponseOnCommunicationProblems()
        throws IOException, ApiGatewayException {
        when(publicationService.getPublication(any(UUID.class)))
            .thenThrow(new ErrorResponseException("Error"));

        fetchPublicationHandler.handleRequest(generateHandlerRequest(), output, context);

        GatewayResponse<PublicationResponse> gatewayResponse = objectMapper.readValue(output.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    private InputStream generateHandlerRequest() throws JsonProcessingException {
        Map<String, String> headers = Map.of(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        Map<String, String> pathParameters = Map.of(IDENTIFIER, IDENTIFIER_VALUE);
        return new HandlerRequestBuilder<InputStream>(objectMapper)
                .withHeaders(headers)
                .withPathParameters(pathParameters)
                .build();
    }

    private Publication createPublication() {
        return new Publication.Builder()
            .withIdentifier(UUID.randomUUID())
            .withModifiedDate(Instant.now())
            .withOwner("owner")
            .withPublisher(new Organization.Builder()
                .withId(URI.create("http://example.org/publisher/1"))
                .build()
            )
            .build();
    }
}

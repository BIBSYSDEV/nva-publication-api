package no.unit.nva.publication.modify;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
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
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singletonMap;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.RequestUtil.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.publication.modify.ModifyPublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.publication.modify.ModifyPublicationHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.handlers.ApiGatewayHandler.DEFAULT_ERROR_MESSAGE;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public class ModifyPublicationHandlerTest {

    public static final String IDENTIFIER = "identifier";
    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_PUBLICATION_RESPONSE_TYPE = objectMapper
            .getTypeFactory()
            .constructParametricType(GatewayResponse.class, PublicationResponse.class);
    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE = objectMapper
            .getTypeFactory()
            .constructParametricType(GatewayResponse.class, Problem.class);
    public static final String OWNER = "owner";
    public static final URI ANY_URI = URI.create("http://example.org/publisher/1");
    public static final String RESOURCE_NOT_FOUND_ERROR_TEMPLATE = "Resource not found: %s";

    private PublicationService publicationService;
    private Context context;

    private OutputStream output;
    private ModifyPublicationHandler modifyPublicationHandler;

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
        modifyPublicationHandler =
            new ModifyPublicationHandler(publicationService, environment);
    }

    @Test
    @DisplayName("handler Returns OK Response On Valid Input")
    public void handlerReturnsOKResponseOnValidInput() throws IOException, ApiGatewayException {
        PublicationStatus expectedStatus = PUBLISHED;
        Publication publication = createPublication();
        Publication modifiedPublication = cloneAndUpdateStatus(publication, expectedStatus);
        when(publicationService.getPublication(any(UUID.class)))
            .thenReturn(publication);
        when(publicationService.updatePublication(any(UUID.class), any(Publication.class)))
            .thenReturn(modifiedPublication);

        InputStream inputStream = generateInputStreamWithValidBodyAndHeadersAndPathParameters(
                modifiedPublication.getIdentifier());
        modifyPublicationHandler.handleRequest(inputStream, output, context);
        GatewayResponse<PublicationResponse> gatewayResponse = toGatewayResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(getGatewayResponseBodyStatus(gatewayResponse), is(equalTo(expectedStatus)));
    }

    private PublicationStatus getGatewayResponseBodyStatus(GatewayResponse<PublicationResponse> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(PublicationResponse.class).getStatus();
    }

    @Test
    @DisplayName("Handler returns NotFound response when resource does not exist")
    void handlerReturnsNotFoundResponseWhenResourceDoesNotExist() throws IOException, ApiGatewayException {
        UUID identifier = UUID.randomUUID();
        String expectedDetail = String.format(RESOURCE_NOT_FOUND_ERROR_TEMPLATE, identifier.toString());
        when(publicationService.getPublication(any(UUID.class)))
            .thenThrow(new NotFoundException(expectedDetail));

        modifyPublicationHandler.handleRequest(
            generateInputStreamWithValidBodyAndHeadersAndPathParameters(identifier),
            output,
            context
        );
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        String problemDetail = gatewayResponse.getBodyObject(Problem.class).getDetail();
        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        assertThat(problemDetail, is(equalTo(expectedDetail)));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    public void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        modifyPublicationHandler.handleRequest(generateInputStreamMissingPathParameters(), output, context);

        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        String problemDetail = gatewayResponse.getBodyObject(Problem.class).getDetail();
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(problemDetail, containsString(IDENTIFIER_IS_NOT_A_VALID_UUID));
    }

    @Test
    @DisplayName("handler Returns BadGateway Response On Communication Problems")
    public void handlerReturnsBadGatewayResponseOnCommunicationProblems()
        throws IOException, ApiGatewayException {

        Publication publication = createPublication();
        when(publicationService.getPublication(any(UUID.class)))
            .thenReturn(publication);
        when(publicationService.updatePublication(any(UUID.class), any(Publication.class)))
            .thenThrow(ErrorResponseException.class);

        modifyPublicationHandler.handleRequest(
            generateInputStreamWithValidBodyAndHeadersAndPathParameters(publication.getIdentifier()), output, context);

        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        Problem problem = gatewayResponse.getBodyObject(Problem.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
        assertThat(problem.getDetail(), containsString(DEFAULT_ERROR_MESSAGE));
    }


    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        Publication publication = createPublication();
        when(publicationService.updatePublication(any(UUID.class), any(Publication.class)))
            .thenThrow(RuntimeException.class);

        modifyPublicationHandler.handleRequest(
            generateInputStreamWithValidBodyAndHeadersAndPathParameters(publication.getIdentifier()), output, context);

        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        Problem problem = gatewayResponse.getBodyObject(Problem.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(problem.getDetail(), containsString(DEFAULT_ERROR_MESSAGE));
    }

    private InputStream generateInputStreamWithValidBodyAndHeadersAndPathParameters(UUID identifier) throws
            IOException {
        return new HandlerRequestBuilder<Publication>(objectMapper)
            .withBody(createPublication(identifier))
            .withHeaders(generateHeaders())
            .withPathParameters(singletonMap(IDENTIFIER, identifier.toString()))
            .build();
    }

    private InputStream generateInputStreamMissingPathParameters() throws IOException {
        return new HandlerRequestBuilder<Publication>(objectMapper)
            .withBody(createPublication())
            .withHeaders(generateHeaders())
            .build();
    }

    private Map<String, String> generateHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return headers;
    }

    private Publication createPublication() {
        return createPublication(UUID.randomUUID());
    }

    private Publication createPublication(UUID identifier) {
        return new Publication.Builder()
            .withIdentifier(identifier)
            .withModifiedDate(Instant.now())
            .withOwner(OWNER)
            .withPublisher(new Organization.Builder()
                .withId(ANY_URI)
                .build()
            )
            .build();
    }

    private Publication cloneAndUpdateStatus(Publication publication, PublicationStatus status) throws
            JsonProcessingException {
        Publication modifiedPublication = clonePublication(publication);
        modifiedPublication.setStatus(status);
        return modifiedPublication;
    }

    private Publication clonePublication(Publication publication) throws JsonProcessingException {
        String that = objectMapper.writeValueAsString(publication);
        return objectMapper.readValue(that, Publication.class);
    }

    private GatewayResponse<PublicationResponse> toGatewayResponse() throws JsonProcessingException {
        return objectMapper.readValue(output.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_PUBLICATION_RESPONSE_TYPE);
    }

    private GatewayResponse<Problem> toGatewayResponseProblem() throws JsonProcessingException {
        return objectMapper.readValue(output.toString(),
                PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE);
    }
}

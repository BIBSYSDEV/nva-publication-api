package no.unit.nva.publication.modify;

import static java.util.Collections.singletonMap;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.RequestUtil.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.publication.modify.ModifyPublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.publication.modify.ModifyPublicationHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.apigateway.ApiGatewayHandler.DEFAULT_ERROR_MESSAGE;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static nva.commons.core.JsonUtils.objectMapper;
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
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.ErrorResponseException;
import no.unit.nva.publication.exception.NotFoundException;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.mockito.stubbing.Answer;
import org.zalando.problem.Problem;

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
    public static final String SOME_MESSAGE = "SomeMessage";

    private PublicationService publicationService;
    private Context context;

    private ByteArrayOutputStream output;
    private ModifyPublicationHandler modifyPublicationHandler;

    private Publication publication;

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
        publication = createPublication();
    }

    @Test
    @DisplayName("handler returns OK Response on ReportBasic input")
    public void handlerReturnsOkResponseOnReportBasicInput() throws IOException, ApiGatewayException {
        Path reportBasic = Path.of("ReportBasicTest.json");
        Publication modifiedPublication = objectMapper
            .readValue(IoUtils.stringFromResources(reportBasic), Publication.class);
        serviceSucceedsAndReturnsModifiedPublication(modifiedPublication);
        InputStream inputStream = generateInputStreamWithValidBodyAndHeadersAndPathParameters(
            modifiedPublication.getIdentifier());
        modifyPublicationHandler.handleRequest(inputStream, output, context);
        GatewayResponse<PublicationResponse> gatewayResponse = toGatewayResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns OK Response On Valid Input")
    public void handlerReturnsOKResponseOnValidInput() throws IOException, ApiGatewayException {
        PublicationStatus expectedStatus = PUBLISHED;
        Publication modifiedPublication = clonePublicationAndUpdateStatus(expectedStatus);
        serviceSucceedsAndReturnsModifiedPublication(modifiedPublication);
        InputStream inputStream = generateInputStreamWithValidBodyAndHeadersAndPathParameters(
            modifiedPublication.getIdentifier());
        modifyPublicationHandler.handleRequest(inputStream, output, context);
        GatewayResponse<PublicationResponse> gatewayResponse = toGatewayResponse();
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(getGatewayResponseBodyStatus(gatewayResponse), is(equalTo(expectedStatus)));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    public void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        modifyPublicationHandler.handleRequest(generateInputStreamMissingPathParameters(), output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(IDENTIFIER_IS_NOT_A_VALID_UUID));
    }

    @Test
    @DisplayName("handler Returns BadGateway Response On Communication Problems")
    public void handlerReturnsBadGatewayResponseOnCommunicationProblems()
        throws IOException, ApiGatewayException {
        serviceSucceedsOnGetRequestAndFailsOnUpdate();
        modifyPublicationHandler.handleRequest(
            generateInputStreamWithValidBodyAndHeadersAndPathParameters(publication.getIdentifier()), output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(DEFAULT_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        serviceFailsOnModifyRequestWithRuntimeError();
        modifyPublicationHandler.handleRequest(
            generateInputStreamWithValidBodyAndHeadersAndPathParameters(publication.getIdentifier()), output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("handler logs error details on unexpected exception")
    public void handlerLogsErrorDetailsOnUnexpectedException()
        throws IOException, ApiGatewayException {
        final TestAppender appender = createAppenderForLogMonitoring();
        publicationServiceThrowsException();
        modifyPublicationHandler.handleRequest(generateInputStreamWithValidBodyAndHeadersAndPathParameters(
            publication.getIdentifier()), output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(appender.getMessages(), containsString(SOME_MESSAGE));
    }

    @Test
    @DisplayName("Handler returns NotFound response when resource does not exist")
    void handlerReturnsNotFoundResponseWhenResourceDoesNotExist() throws IOException, ApiGatewayException {
        SortableIdentifier identifier=  SortableIdentifier.next();
        String expectedDetail = serviceFailsOnGetRequestWithNotFoundError(identifier);
        modifyPublicationHandler.handleRequest(
            generateInputStreamWithValidBodyAndHeadersAndPathParameters(identifier),
            output,
            context
        );
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), is(equalTo(expectedDetail)));
    }

    private TestAppender createAppenderForLogMonitoring() {
        return LogUtils.getTestingAppender(ModifyPublicationHandler.class);
    }

    private void publicationServiceThrowsException() throws ApiGatewayException {
        serviceSucceedsOnGetRequest(publication);
        when(publicationService.updatePublication(any(SortableIdentifier.class), any(Publication.class)))
            .then((Answer<Publication>) invocation -> {
                throw new RuntimeException(ModifyPublicationHandlerTest.SOME_MESSAGE);
            });
    }

    private void serviceFailsOnModifyRequestWithRuntimeError() throws ApiGatewayException {
        serviceSucceedsOnGetRequest(publication);
        when(publicationService.updatePublication(any(SortableIdentifier.class), any(Publication.class)))
            .thenThrow(RuntimeException.class);
    }

    private String serviceFailsOnGetRequestWithNotFoundError(SortableIdentifier identifier) throws ApiGatewayException {
        String expectedDetail = String.format(RESOURCE_NOT_FOUND_ERROR_TEMPLATE, identifier.toString());
        when(publicationService.getPublication(any(SortableIdentifier.class))).thenThrow(new NotFoundException(expectedDetail));
        return expectedDetail;
    }

    private void serviceSucceedsOnGetRequest(Publication publication) throws ApiGatewayException {
        when(publicationService.getPublication(any(SortableIdentifier.class))).thenReturn(publication);
    }

    private void serviceSucceedsAndReturnsModifiedPublication(Publication modifiedPublication)
        throws ApiGatewayException {
        serviceSucceedsOnGetRequest(publication);
        when(publicationService.updatePublication(any(SortableIdentifier.class), any(Publication.class)))
            .thenReturn(modifiedPublication);
    }

    private void serviceSucceedsOnGetRequestAndFailsOnUpdate() throws ApiGatewayException {
        serviceSucceedsOnGetRequest(publication);
        when(publicationService.updatePublication(any(SortableIdentifier.class), any(Publication.class)))
            .thenThrow(ErrorResponseException.class);
    }

    private InputStream generateInputStreamWithValidBodyAndHeadersAndPathParameters(SortableIdentifier identifier)
        throws IOException {
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
        return createPublication(SortableIdentifier.next());
    }

    private Publication createPublication(SortableIdentifier identifier) {
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

    private Publication clonePublicationAndUpdateStatus(PublicationStatus status) throws
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

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    private PublicationStatus getGatewayResponseBodyStatus(GatewayResponse<PublicationResponse> gatewayResponse)
        throws JsonProcessingException {
        return gatewayResponse.getBodyObject(PublicationResponse.class).getStatus();
    }
}

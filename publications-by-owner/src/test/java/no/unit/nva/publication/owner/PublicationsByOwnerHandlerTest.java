package no.unit.nva.publication.owner;

import static java.util.Collections.singletonMap;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static nva.commons.handlers.ApiGatewayHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.exception.ErrorResponseException;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.testutils.HandlerUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.handlers.GatewayResponse;
import nva.commons.core.Environment;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PublicationsByOwnerHandlerTest {

    public static final String OWNER = "junit";
    public static final String VALID_ORG_NUMBER = "NO919477822";

    private Environment environment;
    private PublicationService publicationService;
    private Context context;

    private OutputStream output;
    private PublicationsByOwnerHandler publicationsByOwnerHandler;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn("*");

        publicationService = mock(PublicationService.class);
        context = mock(Context.class);

        output = new ByteArrayOutputStream();
        publicationsByOwnerHandler =
            new PublicationsByOwnerHandler(publicationService, environment);
    }

    @Test
    @DisplayName("default Constructor Throws Exception When Envs Are Not Set")
    public void defaultConstructorThrowsExceptionWhenEnvsAreNotSet() {
        assertThrows(Exception.class, () -> new PublicationsByOwnerHandler());
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    public void handlerReturnsOkResponseOnValidInput() throws IOException, ApiGatewayException {
        when(publicationService.getPublicationsByOwner(anyString(), any(URI.class)))
            .thenReturn(publicationSummaries());

        publicationsByOwnerHandler.handleRequest(
            inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Empty Input")
    public void handlerReturnsBadRequestResponseOnEmptyInput() throws IOException {
        InputStream input = new HandlerUtils(objectMapper)
            .requestObjectToApiGatewayRequestInputSteam(null, null);
        publicationsByOwnerHandler.handleRequest(input, output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns BadGateway Response On Communication Problems")
    public void handlerReturnsBadGatewayResponseOnCommunicationProblems()
        throws IOException, ApiGatewayException {
        when(publicationService.getPublicationsByOwner(anyString(), any(URI.class)))
            .thenThrow(ErrorResponseException.class);

        publicationsByOwnerHandler.handleRequest(
            inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        when(publicationService.getPublicationsByOwner(anyString(), any(URI.class)))
            .thenThrow(NullPointerException.class);

        publicationsByOwnerHandler.handleRequest(
            inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    @Deprecated
    private InputStream inputStream() throws IOException {
        Map<String, Object> event = new HashMap<>();
        event.put("requestContext",
            singletonMap("authorizer",
                singletonMap("claims",
                    Map.of(RequestUtil.CUSTOM_FEIDE_ID, OWNER, RequestUtil.CUSTOM_CUSTOMER_ID, VALID_ORG_NUMBER))));
        event.put("headers", singletonMap(HttpHeaders.CONTENT_TYPE,
            ContentType.APPLICATION_JSON.getMimeType()));
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private List<PublicationSummary> publicationSummaries() {
        List<PublicationSummary> publicationSummaries = new ArrayList<>();
        publicationSummaries.add(new PublicationSummary.Builder()
            .withIdentifier(UUID.randomUUID())
            .withModifiedDate(Instant.now())
            .withCreatedDate(Instant.now())
            .withOwner("junit")
            .withMainTitle("Some main title")
            .withStatus(DRAFT)
            .build()
        );
        publicationSummaries.add(new PublicationSummary.Builder()
            .withIdentifier(UUID.randomUUID())
            .withModifiedDate(Instant.now())
            .withCreatedDate(Instant.now())
            .withOwner(OWNER)
            .withMainTitle("A complete different title")
            .withStatus(DRAFT)
            .build()
        );
        return publicationSummaries;
    }
}

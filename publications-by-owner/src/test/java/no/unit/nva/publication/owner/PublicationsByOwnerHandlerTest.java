package no.unit.nva.publication.owner;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.publication.Environment;
import no.unit.publication.GatewayResponse;
import no.unit.publication.PublicationHandler;
import no.unit.publication.model.PublicationSummary;
import no.unit.publication.service.PublicationService;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonMap;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.publication.PublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.publication.PublicationHandler.ALLOWED_ORIGIN_ENV;
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

public class PublicationsByOwnerHandlerTest {

    public static final String OWNER = "junit";
    public static final String VALID_ORG_NUMBER = "NO919477822";
    private ObjectMapper objectMapper = PublicationHandler.createObjectMapper();

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
        when(environment.get(ALLOWED_ORIGIN_ENV)).thenReturn(Optional.of("*"));

        publicationService = mock(PublicationService.class);
        context = mock(Context.class);

        output = new ByteArrayOutputStream();
        publicationsByOwnerHandler =
                new PublicationsByOwnerHandler(objectMapper, publicationService, environment);

    }

    @Test
    @DisplayName("default Constructor Throws Exception When Envs Are Not Set")
    public void defaultConstructorThrowsExceptionWhenEnvsAreNotSet() {
        assertThrows(Exception.class, () -> new PublicationsByOwnerHandler());
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    public void handlerReturnsOkResponseOnValidInput() throws IOException, InterruptedException {
        when(publicationService.getPublicationsByOwner(anyString(), any(URI.class), any()))
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
        publicationsByOwnerHandler.handleRequest(
                new ByteArrayInputStream(new byte[]{}), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns BadGateway Response On Communication Problems")
    public void handlerReturnsBadGatewayResponseOnCommunicationProblems()
            throws IOException, InterruptedException {
        when(publicationService.getPublicationsByOwner(anyString(), any(URI.class), any()))
                .thenThrow(IOException.class);

        publicationsByOwnerHandler.handleRequest(
                inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public  void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
            throws IOException, InterruptedException {
        when(publicationService.getPublicationsByOwner(anyString(), any(URI.class), any()))
                .thenThrow(NullPointerException.class);

        publicationsByOwnerHandler.handleRequest(
                inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    private InputStream inputStream() throws IOException {
        Map<String, Object> event = new HashMap<>();
        event.put("requestContext",
                singletonMap("authorizer",
                        singletonMap("claims",
                                Map.of("custom:feideId", OWNER, "custom:orgNumber", VALID_ORG_NUMBER))));
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

package no.unit.nva.publication.modify;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Publication;
import no.unit.publication.Environment;
import no.unit.publication.GatewayResponse;
import no.unit.publication.service.PublicationService;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singletonMap;
import static no.unit.nva.publication.modify.ModifyPublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.publication.modify.ModifyPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.publication.service.impl.RestPublicationService.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public class ModifyPublicationHandlerTest {

    public static final String SOME_API_KEY = "some api key";
    public static final String HEADERS = "headers";
    public static final String BODY = "body";
    public static final String PUBLICATION_JSON = "src/test/resources/publication.json";
    public static final String IDENTIFIER = "identifier";
    public static final String PATH_PARAMETERS = "pathParameters";

    private ObjectMapper objectMapper = ModifyPublicationHandler.createObjectMapper();

    private Environment environment;

    private PublicationService publicationService;
    private Context context;

    private OutputStream output;
    private ModifyPublicationHandler modifyPublicationHandler;

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
        modifyPublicationHandler =
                new ModifyPublicationHandler(objectMapper, publicationService, environment);

    }

    @Test
    @DisplayName("default Constructor Throws Exception When Envs Are Not Set")
    public void defaultConstructorThrowsExceptionWhenEnvsAreNotSet() {
        assertThrows(IllegalStateException.class, () -> new ModifyPublicationHandler());
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    public void handlerReturnsOkResponseOnValidInput() throws IOException, InterruptedException {
        Publication publication = objectMapper.readValue(publicationFile(), Publication.class);
        when(publicationService.updatePublication(any(Publication.class), anyString()))
                .thenReturn(publication);

        modifyPublicationHandler.handleRequest(
                inputStream(publication.getIdentifier().toString()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_ACCEPTED, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("handler Returns BadRequest On Identifier Not The Same In Body And PathParam")
    public void handlerReturnsBadRequestOnIdentifierNotTheSameInBodyAndPathParam() throws IOException {
        modifyPublicationHandler.handleRequest(inputStream(UUID.randomUUID().toString()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    public void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        modifyPublicationHandler.handleRequest(inputStreamMissingPathParameters(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Headers")
    public void handlerReturnsBadRequestResponseOnMissingHeaders() throws IOException {
        Map<String, Object> event = Map.of(HEADERS, Collections.emptyMap());
        InputStream inputStream = new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));

        modifyPublicationHandler.handleRequest(inputStream, output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns BadGateway Response On Communication Problems")
    public void handlerReturnsBadGatewayResponseOnCommunicationProblems()
            throws IOException, InterruptedException {
        Publication publication = objectMapper.readValue(publicationFile(), Publication.class);
        when(publicationService.updatePublication(any(Publication.class), anyString()))
                .thenThrow(IOException.class);

        modifyPublicationHandler.handleRequest(
                inputStream(publication.getIdentifier().toString()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public  void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
            throws IOException, InterruptedException {
        Publication publication = objectMapper.readValue(publicationFile(), Publication.class);
        when(publicationService.updatePublication(any(Publication.class), anyString()))
                .thenThrow(NullPointerException.class);

        modifyPublicationHandler.handleRequest(
                inputStream(publication.getIdentifier().toString()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    private InputStream inputStream(String identifier) throws IOException {
        Map<String, Object> event = new ConcurrentHashMap<>();
        String body = new String(publicationFile());
        event.put(BODY, body);
        Map<String,String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, SOME_API_KEY);
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        event.put(HEADERS, headers);
        event.put(PATH_PARAMETERS, singletonMap(IDENTIFIER, identifier));
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private InputStream inputStreamMissingPathParameters() throws IOException {
        Map<String, Object> event = new ConcurrentHashMap<>();
        String body = new String(publicationFile());
        event.put(BODY, body);
        Map<String,String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, SOME_API_KEY);
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        event.put(HEADERS, headers);
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private byte[] publicationFile() throws IOException {
        return Files.readAllBytes(Paths.get(PUBLICATION_JSON));
    }

}

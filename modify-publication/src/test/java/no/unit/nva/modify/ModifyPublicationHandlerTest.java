package no.unit.nva.modify;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.Environment;
import no.unit.nva.GatewayResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.service.PublicationService;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
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
import static no.unit.nva.modify.ModifyPublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.modify.ModifyPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.service.impl.RestPublicationService.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
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
                new ModifyPublicationHandler(() -> objectMapper, () -> publicationService, () -> environment);

    }

    @Test
    public void testDefaultConstructor() {
        assertThrows(IllegalStateException.class, () -> new ModifyPublicationHandler());
    }

    @Test
    public void testOkResponse() throws IOException, InterruptedException {
        Publication publication = objectMapper.readValue(getExampleFile(), Publication.class);
        when(publicationService.updatePublication(any(Publication.class), anyString()))
                .thenReturn(publication);

        modifyPublicationHandler.handleRequest(
                inputStream(publication.getIdentifier().toString()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testIdentifiersInPathParametersAndBodyAreNotTheSame() throws IOException {
        modifyPublicationHandler.handleRequest(inputStream(UUID.randomUUID().toString()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    public void testBadRequestMissingPathParameters() throws IOException {
        modifyPublicationHandler.handleRequest(inputStreamMissingPathParameters(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    public void testMissingAuthorizationHeaderBadRequestResponse() throws IOException {
        Map<String, Object> event = Map.of(HEADERS, Collections.emptyMap());
        InputStream inputStream = new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));

        modifyPublicationHandler.handleRequest(inputStream, output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    public void testBadGateWayResponse() throws IOException, InterruptedException {
        Publication publication = objectMapper.readValue(getExampleFile(), Publication.class);
        when(publicationService.updatePublication(any(Publication.class), anyString()))
                .thenThrow(IOException.class);

        modifyPublicationHandler.handleRequest(
                inputStream(publication.getIdentifier().toString()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    @Test
    public void testInternalServerErrorResponse() throws IOException, InterruptedException {
        Publication publication = objectMapper.readValue(getExampleFile(), Publication.class);
        when(publicationService.updatePublication(any(Publication.class), anyString()))
                .thenThrow(NullPointerException.class);

        modifyPublicationHandler.handleRequest(
                inputStream(publication.getIdentifier().toString()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    private InputStream inputStream(String identifier) throws IOException {
        Map<String, Object> event = new ConcurrentHashMap<>();
        String body = new String(getExampleFile());
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
        String body = new String(getExampleFile());
        event.put(BODY, body);
        Map<String,String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, SOME_API_KEY);
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        event.put(HEADERS, headers);
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private byte[] getExampleFile() throws IOException {
        return Files.readAllBytes(Paths.get(PUBLICATION_JSON));
    }

}

package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.Environment;
import no.unit.nva.GatewayResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.ModifyResourceService;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singletonMap;
import static no.unit.nva.publication.ModifyPublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.publication.ModifyPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.publication.ModifyPublicationHandler.API_HOST_ENV;
import static no.unit.nva.publication.ModifyPublicationHandler.API_SCHEME_ENV;
import static no.unit.nva.publication.service.ModifyResourceService.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ModifyPublicationHandlerTest {

    public static final String SOME_API_KEY = "some api key";
    public static final String HEADERS = "headers";
    public static final String BODY = "body";
    public static final String PUBLICATION_JSON = "src/test/resources/publication.json";
    public static final String IDENTIFIER = "identifier";
    public static final String PATH_PARAMETERS = "pathParameters";
    public static final String IDENTIFIER_JSON_POINTER = "/identifier";

    private ObjectMapper objectMapper = ModifyPublicationHandler.createObjectMapper();

    @Mock
    private Environment environment;

    @Mock
    private ModifyResourceService modifyResourceService;

    @Mock
    private Context context;

    private OutputStream output;
    private ModifyPublicationHandler modifyPublicationHandler;

    /**
     * Set up environment.
     */
    @Before
    public void setUp() {
        when(environment.get(ALLOWED_ORIGIN_ENV)).thenReturn(Optional.of("*"));
        when(environment.get(API_HOST_ENV)).thenReturn(Optional.of("localhost:3000"));
        when(environment.get(API_SCHEME_ENV)).thenReturn(Optional.of("http"));

        output = new ByteArrayOutputStream();
        modifyPublicationHandler =
                new ModifyPublicationHandler(objectMapper, modifyResourceService, environment);

    }

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_ENV, "*");
        environmentVariables.set(API_HOST_ENV, "localhost:3000");
        environmentVariables.set(API_SCHEME_ENV, "http");
        ModifyPublicationHandler modifyPublicationHandler = new ModifyPublicationHandler();
        assertNotNull(modifyPublicationHandler);
    }

    @Test
    public void testOkResponse() throws IOException, InterruptedException {
        JsonNode jsonNode = objectMapper.readTree(getExampleFile());
        when(modifyResourceService.modifyResource(any(UUID.class), any(Publication.class), anyString(), anyString(),
                anyString()))
                .thenReturn(jsonNode);

        modifyPublicationHandler.handleRequest(
                inputStream(jsonNode.at(IDENTIFIER_JSON_POINTER).textValue()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
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
    public void testBadGateWayResponse() throws IOException, InterruptedException {
        JsonNode jsonNode = objectMapper.readTree(getExampleFile());
        when(modifyResourceService.modifyResource(
                any(UUID.class), any(Publication.class), anyString(), anyString(), anyString()))
                .thenThrow(IOException.class);

        modifyPublicationHandler.handleRequest(
                inputStream(jsonNode.at(IDENTIFIER_JSON_POINTER).textValue()), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    @Test
    public void testInternalServerErrorResponse() throws IOException, InterruptedException {
        JsonNode jsonNode = objectMapper.readTree(getExampleFile());
        when(modifyResourceService.modifyResource(
                any(UUID.class), any(Publication.class), anyString(), anyString(), anyString()))
                .thenThrow(NullPointerException.class);

        modifyPublicationHandler.handleRequest(
                inputStream(jsonNode.at(IDENTIFIER_JSON_POINTER).textValue()), output, context);

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

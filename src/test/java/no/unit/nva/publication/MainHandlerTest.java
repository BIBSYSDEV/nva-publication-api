package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.publication.service.ResourcePersistenceService;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
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
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.unit.nva.publication.MainHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.publication.MainHandler.API_HOST_ENV;
import static no.unit.nva.publication.MainHandler.API_SCHEME_ENV;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MainHandlerTest {

    private ObjectMapper objectMapper = MainHandler.createObjectMapper();

    private Environment environment;

    /**
     * Set up environment.
     */
    @Before
    public void setUp() {
        environment = Mockito.mock(Environment.class);
        when(environment.get(ALLOWED_ORIGIN_ENV)).thenReturn(Optional.of("*"));
        when(environment.get(API_HOST_ENV)).thenReturn(Optional.of("localhost:3000"));
        when(environment.get(API_SCHEME_ENV)).thenReturn(Optional.of("http"));

    }

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_ENV, "*");
        environmentVariables.set(API_HOST_ENV, "localhost:3000");
        environmentVariables.set(API_SCHEME_ENV, "http");
        MainHandler mainHandler = new MainHandler();
        assertNotNull(mainHandler);
    }

    @Test
    public void testOkResponse() throws IOException {
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        JsonNode jsonNode = objectMapper.readTree(getExampleFile());
        when(resourcePersistenceService.fetchResource(any(UUID.class), anyString(), anyString())).thenReturn(jsonNode);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, resourcePersistenceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(MainHandler.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testNotFoundResponse() throws IOException {
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        JsonNode jsonNode = objectMapper.readTree(getNoItemsExampleFile());
        when(resourcePersistenceService.fetchResource(any(UUID.class), anyString(), anyString())).thenReturn(jsonNode);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, resourcePersistenceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(MainHandler.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testBadRequestResponse() throws IOException {
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, resourcePersistenceService, environment);

        OutputStream output = new ByteArrayOutputStream();


        mainHandler.handleRequest(new ByteArrayInputStream(new byte[0]), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    public  void testInternalServerErrorResponse() throws IOException {
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, resourcePersistenceService, environment);

        OutputStream output = new ByteArrayOutputStream();


        mainHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    @Test
    public  void testBadGatewayErrorResponse() throws IOException {
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        when(resourcePersistenceService.fetchResource(any(UUID.class), anyString(), anyString()))
                .thenThrow(new WebApplicationException());
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, resourcePersistenceService, environment);

        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    private Context getMockContext() {
        return mock(Context.class);
    }

    private InputStream inputStream() throws IOException {
        Map<String, Object> event = new ConcurrentHashMap<>();
        String body = new String(getExampleFile());
        event.put("body", body);
        Map<String,String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, "some api key");
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        event.put("headers", headers);
        event.put("pathParameters", singletonMap("identifier", "0ea0dd31-c202-4bff-8521-afd42b1ad8db"));
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private byte[] getExampleFile() throws IOException {
        return Files.readAllBytes(Paths.get("src/test/resources/resource_response.json"));
    }

    private byte[] getNoItemsExampleFile() throws IOException {
        return Files.readAllBytes(Paths.get("src/test/resources/empty_response.json"));
    }
}

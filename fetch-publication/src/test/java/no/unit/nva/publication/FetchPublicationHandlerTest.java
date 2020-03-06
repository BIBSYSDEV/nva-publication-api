package no.unit.nva.publication;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.Environment;
import no.unit.nva.GatewayResponse;
import no.unit.nva.publication.service.FetchResourceService;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

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
import static no.unit.nva.publication.FetchPublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.publication.FetchPublicationHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.publication.FetchPublicationHandler.API_HOST_ENV;
import static no.unit.nva.publication.FetchPublicationHandler.API_SCHEME_ENV;
import static no.unit.nva.publication.service.FetchResourceService.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FetchPublicationHandlerTest {

    public static final String SOME_API_KEY = "some api key";
    public static final String PATH_PARAMETERS = "pathParameters";
    public static final String HEADERS = "headers";
    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_VALUE = "0ea0dd31-c202-4bff-8521-afd42b1ad8db";
    public static final String RESOURCE_RESPONSE_JSON = "src/test/resources/resource_response.json";
    public static final String EMPTY_RESPONSE_JSON = "src/test/resources/empty_response.json";
    public static final String MISSING_FILE_JSON = "missing_file.json";
    private ObjectMapper objectMapper = FetchPublicationHandler.createObjectMapper();

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
        FetchPublicationHandler fetchPublicationHandler = new FetchPublicationHandler();
        assertNotNull(fetchPublicationHandler);
    }

    @Test
    public void testOkResponse() throws IOException, InterruptedException {
        FetchResourceService fetchResourceService = mock(FetchResourceService.class);
        JsonNode jsonNode = objectMapper.readTree(getExampleFile());
        when(fetchResourceService.fetchResource(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(jsonNode);
        Context context = getMockContext();
        FetchPublicationHandler fetchPublicationHandler = new FetchPublicationHandler(objectMapper,
                fetchResourceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        fetchPublicationHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testNotFoundResponse() throws IOException, InterruptedException {
        FetchResourceService fetchResourceService = mock(FetchResourceService.class);
        JsonNode jsonNode = objectMapper.readTree(getNoItemsExampleFile());
        when(fetchResourceService.fetchResource(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(jsonNode);
        Context context = getMockContext();
        FetchPublicationHandler fetchPublicationHandler = new FetchPublicationHandler(objectMapper,
                fetchResourceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        fetchPublicationHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testBadRequestResponse() throws IOException {
        FetchResourceService fetchResourceService = mock(FetchResourceService.class);
        Context context = getMockContext();
        FetchPublicationHandler fetchPublicationHandler = new FetchPublicationHandler(objectMapper,
                fetchResourceService, environment);

        OutputStream output = new ByteArrayOutputStream();


        fetchPublicationHandler.handleRequest(new ByteArrayInputStream(new byte[0]), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    public  void testInternalServerErrorResponse() throws IOException {
        FetchResourceService fetchResourceService = mock(FetchResourceService.class);
        Context context = getMockContext();
        FetchPublicationHandler fetchPublicationHandler = new FetchPublicationHandler(objectMapper,
                fetchResourceService, environment);

        OutputStream output = new ByteArrayOutputStream();


        fetchPublicationHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    @Test
    public void testBadGatewayErrorResponse() throws IOException, InterruptedException {
        FetchResourceService fetchResourceService = mock(FetchResourceService.class);
        when(fetchResourceService.fetchResource(any(UUID.class), anyString(), anyString(), anyString()))
                .thenThrow(new IOException());
        Context context = getMockContext();
        FetchPublicationHandler fetchPublicationHandler = new FetchPublicationHandler(objectMapper,
                fetchResourceService, environment);

        OutputStream output = new ByteArrayOutputStream();

        fetchPublicationHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    @Test
    public void testMissingPublicationContext() {
        FetchResourceService fetchResourceService = mock(FetchResourceService.class);
        FetchPublicationHandler fetchPublicationHandler = new FetchPublicationHandler(objectMapper,
                fetchResourceService, environment);
        Optional<JsonNode> publicationContext = fetchPublicationHandler
                .getPublicationContext(MISSING_FILE_JSON);
        assertTrue(publicationContext.isEmpty());
    }

    private Context getMockContext() {
        return mock(Context.class);
    }

    private InputStream inputStream() throws IOException {
        Map<String, Object> event = new ConcurrentHashMap<>();
        Map<String,String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, SOME_API_KEY);
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        event.put(HEADERS, headers);
        event.put(PATH_PARAMETERS, singletonMap(IDENTIFIER, IDENTIFIER_VALUE));
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private byte[] getExampleFile() throws IOException {
        return Files.readAllBytes(Paths.get(RESOURCE_RESPONSE_JSON));
    }

    private byte[] getNoItemsExampleFile() throws IOException {
        return Files.readAllBytes(Paths.get(EMPTY_RESPONSE_JSON));
    }
}

package no.unit.nva;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.zalando.problem.Status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;

import static no.unit.nva.PublicationHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.PublicationHandler.APPLICATION_JSON;
import static no.unit.nva.PublicationHandler.CONTENT_TYPE;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class PublicationHandlerTest {

    public static final String MISSING_FILE_JSON = "missing_file.json";
    public static final String WILDCARD = "*";

    private OutputStream outputStream;
    private ObjectMapper objectMapper = PublicationHandler.createObjectMapper();

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    private PublicationHandler publicationHandler;

    @Before
    public void setUp() {
        environmentVariables.set(PublicationHandler.ALLOWED_ORIGIN_ENV, WILDCARD);
        publicationHandler = new TestPublicationHandler(objectMapper, new Environment());
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void testMissingPublicationContext() {
        Optional<JsonNode> publicationContext = publicationHandler
                .getPublicationContext(MISSING_FILE_JSON);
        assertTrue(publicationContext.isEmpty());
    }

    @Test
    public void testHeaders() {
        Map<String, String> headers = publicationHandler.headers();
        Assertions.assertEquals(headers.get(ACCESS_CONTROL_ALLOW_ORIGIN), WILDCARD);
        Assertions.assertEquals(headers.get(CONTENT_TYPE), APPLICATION_JSON);
    }

    @Test
    public void testWriteErrorResponse() throws IOException {
        String message = "Test!";
        Exception exception = new Exception(message);
        publicationHandler.writeErrorResponse(outputStream, Status.INTERNAL_SERVER_ERROR, exception);

        GatewayResponse gatewayResponse = objectMapper.readValue(outputStream.toString(), GatewayResponse.class);

        Assertions.assertNotNull(gatewayResponse);

    }

    public static class TestPublicationHandler extends PublicationHandler {

        public TestPublicationHandler(ObjectMapper objectMapper, Environment environment) {
            super(objectMapper, environment);
        }

        @Override
        public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {

        }
    }
}

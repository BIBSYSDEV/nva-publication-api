package no.unit.nva.publication.publish;

import static no.unit.nva.publication.service.impl.DynamoDBPublicationService.PUBLISH_IN_PROGRESS;
import static nva.commons.handlers.ApiGatewayHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static nva.commons.handlers.ApiGatewayHandler.CONTENT_TYPE;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpHeaders.LOCATION;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.testutils.HandlerUtils;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PublishPublicationHandlerTest {

    public static final String HTTPS = "https";
    public static final String NVA_UNIT_NO = "nva.unit.no";
    public static final String WILDCARD = "*";

    private Environment environment;
    private PublicationService publicationService;
    private ByteArrayOutputStream output;
    private Context context;

    /**
     * Set up environment for test.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        when(environment.readEnv(PublishPublicationHandler.API_SCHEME)).thenReturn(HTTPS);
        when(environment.readEnv(PublishPublicationHandler.API_HOST)).thenReturn(NVA_UNIT_NO);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        publicationService = mock(PublicationService.class);
        output = new ByteArrayOutputStream();
        context = mock(Context.class);
    }

    @Test
    public void canPublishPublication() throws Exception {
        UUID identifier = UUID.randomUUID();
        PublishPublicationStatusResponse status = new PublishPublicationStatusResponse(
            PUBLISH_IN_PROGRESS, SC_ACCEPTED);
        when(publicationService.publishPublication(identifier)).thenReturn(status);

        PublishPublicationHandler handler = new PublishPublicationHandler(environment, publicationService);
        InputStream input = new HandlerUtils(objectMapper).requestObjectToApiGatewayRequestInputSteam(
            null,
            getRequestHeaders(),
            Map.of(RequestUtil.IDENTIFIER, identifier.toString()),
            Map.of());
        handler.handleRequest(input, output, context);

        GatewayResponse<PublishPublicationStatusResponse> actual = objectMapper.readValue(
            output.toByteArray(),
            GatewayResponse.class);

        GatewayResponse<PublishPublicationStatusResponse> expected = new GatewayResponse<>(
            objectMapper.writeValueAsString(status),
            getResponseHeaders(handler.getLocation(identifier).toString()),
            SC_ACCEPTED
        );

        assertEquals(expected, actual);
    }

    private Map<String, String> getResponseHeaders(String location) {
        return Map.of(
            CONTENT_TYPE, APPLICATION_JSON.getMimeType(),
            ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD,
            LOCATION, location
        );
    }

    private Map<String, String> getRequestHeaders() {
        return Map.of(
            CONTENT_TYPE, APPLICATION_JSON.getMimeType()
        );
    }

    @Test
    public void getLocationReturnsUri() {
        PublishPublicationHandler handler = new PublishPublicationHandler(environment, publicationService);
        URI location = handler.getLocation(UUID.randomUUID());

        assertNotNull(location);
    }
}

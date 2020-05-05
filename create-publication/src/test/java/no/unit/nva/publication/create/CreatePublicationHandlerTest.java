package no.unit.nva.publication.create;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.testutils.HandlerUtils;
import no.unit.nva.testutils.TestContext;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static no.unit.nva.publication.testing.TestHeaders.getRequestHeaders;
import static no.unit.nva.publication.testing.TestHeaders.getResponseHeaders;
import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public class CreatePublicationHandlerTest {

    public static final String WILDCARD = "*";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String TEST_FEIDE_ID = "junit";
    public static final String TEST_ORG_NUMBER = "919477822";
    public static final String REQUEST_CONTEXT = "requestContext";
    public static final String HEADERS = "headers";

    private PublicationService publicationServiceMock;
    private Environment environmentMock;
    private CreatePublicationHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        publicationServiceMock = mock(PublicationService.class);
        environmentMock = mock(Environment.class);
        when(environmentMock.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        handler = new CreatePublicationHandler(publicationServiceMock, environmentMock);
        outputStream = new ByteArrayOutputStream();
        context = new TestContext();
    }

    @Test
    public void requestToHandlerReturnsCustomerCreated() throws Exception {
        Publication publication = createPublication();
        when(publicationServiceMock.createPublication(publication)).thenReturn(publication);

        Map<String, String> headers = getRequestHeaders();
        InputStream inputStream = new HandlerUtils(objectMapper).requestObjectToApiGatewayRequestInputSteam(
                publication,
                headers);
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<JsonNode> actual = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);


        GatewayResponse<JsonNode> expected = new GatewayResponse<>(
                handler.toJsonNodeWithContext(publication),
                getResponseHeaders(),
                HttpStatus.SC_CREATED
        );

        assertEquals(expected, actual);
    }

    @Test
    public void canCreateNewPublication() throws Exception {
        Publication publication = createPublication();
        when(publicationServiceMock.createPublication(any(Publication.class))).thenReturn(publication);

        InputStream inputStream = emptyCreatePublicationRequest();
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Publication> actual = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertEquals(HttpStatus.SC_CREATED, actual.getStatusCode());
        assertNotNull(actual.getBodyObject(Publication.class));
    }

    private InputStream emptyCreatePublicationRequest() throws JsonProcessingException {
        Map requestContext = Map.of(
                REQUEST_CONTEXT, Map.of(
                        AUTHORIZER, Map.of(
                                CLAIMS, Map.of(
                                        RequestUtil.CUSTOM_FEIDE_ID, TEST_FEIDE_ID,
                                        RequestUtil.CUSTOM_ORG_NUMBER, TEST_ORG_NUMBER
                                )
                        )
                ),
                HEADERS, getRequestHeaders()
        );
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(requestContext));
    }

    private Publication createPublication() {
        return new Publication.Builder()
                .withIdentifier(UUID.randomUUID())
                .withModifiedDate(Instant.now())
                .withOwner("owner")
                .withPublisher(new Organization.Builder()
                        .withId(URI.create("http://example.org/publisher/1"))
                        .build()
                )
                .build();
    }

}

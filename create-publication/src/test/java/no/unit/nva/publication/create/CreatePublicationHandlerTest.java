package no.unit.nva.publication.create;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.publication.testing.TestHeaders;
import no.unit.nva.testutils.HandlerUtils;
import no.unit.nva.testutils.TestContext;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static no.unit.nva.publication.create.CreatePublicationHandler.INPUT_ERROR;
import static no.unit.nva.publication.testing.TestHeaders.getErrorResponseHeaders;
import static no.unit.nva.publication.testing.TestHeaders.getResponseHeaders;
import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_REQUEST;

@EnableRuleMigrationSupport
public class CreatePublicationHandlerTest {

    public static final String WILDCARD = "*";
    public static final String REQUEST_ID = "requestId";

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

        Map<String, String> headers = TestHeaders.getRequestHeaders();
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
    public void requestToHandlerWithInvalidInputReturnsBadRequest() throws Exception {
        Map<String, String> headers = TestHeaders.getRequestHeaders();
        InputStream inputStream = new HandlerUtils(objectMapper).requestObjectToApiGatewayRequestInputSteam(
                null,
                headers);
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> actual = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        GatewayResponse<Problem> expected = new GatewayResponse<>(
                Problem.builder()
                        .withStatus(BAD_REQUEST)
                        .withTitle(BAD_REQUEST.getReasonPhrase())
                        .withDetail(INPUT_ERROR)
                        .with(REQUEST_ID, null)
                        .build(),
                getErrorResponseHeaders(),
                SC_BAD_REQUEST
        );

        assertEquals(expected, actual);
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

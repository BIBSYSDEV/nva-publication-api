package no.unit.nva.publication.query;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.exception.ErrorResponseException;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.PublicationService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestContext;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonMap;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static nva.commons.handlers.ApiGatewayHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListPublishedPublicationsHandlerTest {

    public static final String OWNER = "junit";
    public static final String VALID_ORG_NUMBER = "NO919477822";

    private Environment environment;
    private PublicationService publicationService;
    private Context context;

    private OutputStream output;
    private ListPublishedPublicationsHandler listPublishedPublicationsHandler;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn("*");

        publicationService = mock(PublicationService.class);
        context = new TestContext();

        output = new ByteArrayOutputStream();
        listPublishedPublicationsHandler =
            new ListPublishedPublicationsHandler(publicationService, environment);
    }

    @Test
    @DisplayName("default Constructor Throws Exception When Envs Are Not Set")
    public void defaultConstructorThrowsExceptionWhenEnvsAreNotSet() {
        assertThrows(Exception.class, () -> new ListPublishedPublicationsHandler());
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    public void handlerReturnsOkResponseOnValidInput() throws IOException, ApiGatewayException {
        when(publicationService.listPublishedPublicationsByDate(anyInt()))
            .thenReturn(publicationSummaries());

        listPublishedPublicationsHandler.handleRequest(
            inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("handler Returns BadGateway Response On Communication Problems")
    public void handlerReturnsBadGatewayResponseOnCommunicationProblems()
        throws IOException, ApiGatewayException {
        when(publicationService.listPublishedPublicationsByDate(anyInt()))
            .thenThrow(ErrorResponseException.class);

        listPublishedPublicationsHandler.handleRequest(
            inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        when(publicationService.listPublishedPublicationsByDate(anyInt()))
            .thenThrow(NullPointerException.class);

        listPublishedPublicationsHandler.handleRequest(
            inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    @Deprecated
    private InputStream inputStream() throws IOException {

        InputStream request = new HandlerRequestBuilder<Void>(objectMapper)
                .withRequestContext(  singletonMap("authorizer",
                        singletonMap("claims",
                                Map.of("custom:feideId", OWNER, "custom:orgNumber", VALID_ORG_NUMBER))))
                .withHeaders(singletonMap(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()))
                .build();
            return  request;
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


    private PublishedPublicationsResponse publishedPublicationsResponse() {

        PublishedPublicationsResponse publishedPublicationsResponse =
                new PublishedPublicationsResponse(publicationSummaries());

        return publishedPublicationsResponse;
    }
}

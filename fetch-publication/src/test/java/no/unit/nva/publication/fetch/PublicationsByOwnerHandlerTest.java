package no.unit.nva.publication.fetch;

import static java.util.Collections.singletonMap;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static nva.commons.apigateway.ApiGatewayHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static nva.commons.apigateway.HttpHeaders.CONTENT_TYPE;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.core.Environment;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PublicationsByOwnerHandlerTest {

    public static final String OWNER = "junit";
    public static final String VALID_ORG_NUMBER = "NO919477822";

    private Environment environment;
    private ResourceService resourceService;
    private Context context;

    private ByteArrayOutputStream output;
    private PublicationsByOwnerHandler publicationsByOwnerHandler;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn("*");

        resourceService = mock(ResourceService.class);
        context = mock(Context.class);

        output = new ByteArrayOutputStream();
        publicationsByOwnerHandler =
            new PublicationsByOwnerHandler(resourceService, environment);
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    public void handlerReturnsOkResponseOnValidInput() throws IOException {
        when(resourceService.getPublicationsByOwner(any(UserInstance.class)))
            .thenReturn(publicationSummaries());

        publicationsByOwnerHandler.handleRequest(
            inputStream(), output, context);

        GatewayResponse<PublicationsByOwnerResponse> gatewayResponse = GatewayResponse.fromOutputStream(output);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Empty Input")
    public void handlerReturnsBadRequestResponseOnEmptyInput() throws IOException {
        InputStream input = new HandlerUtils(objectMapper)
                                .requestObjectToApiGatewayRequestInputSteam(null, null);
        publicationsByOwnerHandler.handleRequest(input, output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException {
        when(resourceService.getPublicationsByOwner(any(UserInstance.class)))
            .thenThrow(NullPointerException.class);

        publicationsByOwnerHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    @Deprecated
    private InputStream inputStream() throws IOException {
        Map<String, Object> event = new HashMap<>();
        event.put("requestContext",
                  singletonMap("authorizer",
                               singletonMap("claims",
                                            Map.of(RequestUtil.CUSTOM_FEIDE_ID, OWNER, RequestUtil.CUSTOM_CUSTOMER_ID,
                                                   VALID_ORG_NUMBER))));
        event.put("headers", singletonMap(HttpHeaders.CONTENT_TYPE,
                                          ContentType.APPLICATION_JSON.getMimeType()));
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private List<Publication> publicationSummaries() {
        List<Publication> publicationSummaries = new ArrayList<>();
        publicationSummaries.add(new Publication.Builder()
                                     .withIdentifier(SortableIdentifier.next())
                                     .withModifiedDate(Instant.now())
                                     .withCreatedDate(Instant.now())
                                     .withOwner("junit")
                                     .withEntityDescription(
                                         new EntityDescription.Builder().withMainTitle("Some main title").build())
                                     .withStatus(DRAFT)
                                     .build()
        );
        publicationSummaries.add(new Publication.Builder()
                                     .withIdentifier(SortableIdentifier.next())
                                     .withModifiedDate(Instant.now())
                                     .withCreatedDate(Instant.now())
                                     .withOwner(OWNER)
                                     .withEntityDescription(
                                         new EntityDescription.Builder().withMainTitle("A complete different title")
                                             .build())
                                     .withStatus(DRAFT)
                                     .build()
        );
        return publicationSummaries;
    }
}

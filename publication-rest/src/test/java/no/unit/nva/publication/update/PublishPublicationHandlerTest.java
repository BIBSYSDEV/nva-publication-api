package no.unit.nva.publication.update;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.LOCATION;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.service.impl.UpdateResourceService.PUBLISH_IN_PROGRESS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PublishPublicationHandlerTest {
    
    public static final String HTTPS = "https";
    public static final String NVA_UNIT_NO = "nva.unit.no";
    public static final String WILDCARD = "*";
    
    private Environment environment;
    private ResourceService publicationService;
    private ByteArrayOutputStream output;
    private Context context;
    
    /**
     * Set up environment for test.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        when(environment.readEnv(PublishPublicationHandler.API_HOST)).thenReturn(NVA_UNIT_NO);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        publicationService = mock(ResourceService.class);
        output = new ByteArrayOutputStream();
        context = mock(Context.class);
    }
    
    @Test
    void publishPublicationHandlerReturnsGatewayResponseWhenInputIsValid() throws Exception {
        var identifier = SortableIdentifier.next();
        var status = mockPublishPublicationStatusResponse();
        
        var handler = callPublishPublicationHandler(identifier);
        
        var actualResponse =
            GatewayResponse.fromOutputStream(output, PublishPublicationStatusResponse.class);
        
        GatewayResponse<PublishPublicationStatusResponse> expected = new GatewayResponse<>(
            restApiMapper.writeValueAsString(status),
            getResponseHeaders(handler.getLocation(identifier).toString()),
            SC_ACCEPTED
        );
        
        assertEquals(expected, actualResponse);
    }
    
    @Test
    void getLocationReturnsUri() {
        PublishPublicationHandler handler = new PublishPublicationHandler(environment, publicationService);
        URI location = handler.getLocation(SortableIdentifier.next());
        
        assertNotNull(location);
    }
    
    private PublishPublicationHandler callPublishPublicationHandler(SortableIdentifier identifier) throws IOException {
        PublishPublicationHandler handler = new PublishPublicationHandler(environment, publicationService);
        InputStream input = new HandlerRequestBuilder<InputStream>(restApiMapper)
            .withHeaders(getRequestHeaders())
            .withNvaUsername(randomString())
            .withCustomerId(randomUri())
            .withPathParameters(Map.of(RequestUtil.PUBLICATION_IDENTIFIER, identifier.toString()))
            .withQueryParameters(Collections.emptyMap())
            .build();
        handler.handleRequest(input, output, context);
        return handler;
    }
    
    private PublishPublicationStatusResponse mockPublishPublicationStatusResponse() throws ApiGatewayException {
        PublishPublicationStatusResponse status = new PublishPublicationStatusResponse(
            PUBLISH_IN_PROGRESS, SC_ACCEPTED);
        when(publicationService.publishPublication(any(UserInstance.class), any(SortableIdentifier.class)))
            .thenReturn(status);
        return status;
    }
    
    private Map<String, String> getResponseHeaders(String location) {
        return Map.of(
            CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
            ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD,
            LOCATION, location
        );
    }
    
    private Map<String, String> getRequestHeaders() {
        return Map.of(
            CONTENT_TYPE, APPLICATION_JSON.getMimeType()
        );
    }
}

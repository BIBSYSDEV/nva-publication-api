package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
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
    void handlerReturnsOkResponseOnValidInput() throws IOException {
        when(resourceService.getPublicationsByOwner(any(UserInstance.class)))
            .thenReturn(publicationSummaries());

        InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
            .withNvaUsername(randomString())
            .withCustomerId(randomUri().toString())
            .build();
        publicationsByOwnerHandler.handleRequest(input, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationsByOwnerResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserCannotBeIdentified() throws IOException {
        InputStream input = new HandlerRequestBuilder<Void>(restApiMapper).build();
        publicationsByOwnerHandler.handleRequest(input, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Void.class);
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, gatewayResponse.getStatusCode());
    }

    private List<Publication> publicationSummaries() {
        return List.of(randomPublication(), randomPublication(), randomPublication());
    }
}

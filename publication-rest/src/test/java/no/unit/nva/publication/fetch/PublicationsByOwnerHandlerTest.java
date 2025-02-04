package no.unit.nva.publication.fetch;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicationsByOwnerHandlerTest {

    private ResourceService resourceService;
    private final Context context = new FakeContext();

    private ByteArrayOutputStream output;
    private PublicationsByOwnerHandler publicationsByOwnerHandler;
    private GetExternalClientResponse getExternalClientResponse;
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");

    @BeforeEach
    public void setUp(@Mock Environment environment,
                      @Mock ResourceService resourceService,
                      @Mock IdentityServiceClient identityServiceClient) throws NotFoundException {
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn("*");

        this.resourceService = resourceService;
        getExternalClientResponse = new GetExternalClientResponse(EXTERNAL_CLIENT_ID,
                                                                  "someone@123",
                                                                  randomUri(),
                                                                  randomUri());
        lenient().when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);

        output = new ByteArrayOutputStream();
        publicationsByOwnerHandler =
            new PublicationsByOwnerHandler(resourceService, environment, identityServiceClient, mock(HttpClient.class));
    }

    @Test
    @DisplayName("handler Returns Ok Response On Valid Input")
    void handlerReturnsOkResponseOnValidInput() throws IOException {
        when(resourceService.getPublicationSummaryByOwner(any(UserInstance.class)))
            .thenReturn(publicationSummaries());

        InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
                                .withNvaUsername(randomString())
                                .withCurrentCustomer(randomUri())
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

    @Test
    void returnsOkWhenIssuedByExternalClient() throws IOException {
        when(resourceService.getPublicationSummaryByOwner(any(UserInstance.class)))
            .thenReturn(publicationSummaries());

        InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
                                .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                                .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                                .build();
        publicationsByOwnerHandler.handleRequest(input, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationsByOwnerResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private List<PublicationSummary> publicationSummaries() {
        return Stream.of(randomPublication(), randomPublication(), randomPublication())
                   .map(PublicationSummary::create)
                   .toList();
    }
}

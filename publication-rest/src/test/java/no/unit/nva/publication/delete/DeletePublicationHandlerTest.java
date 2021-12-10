package no.unit.nva.publication.delete;

import static java.util.Collections.singletonMap;
import static no.unit.nva.model.testing.PublicationGenerator.publicationWithoutIdentifier;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.UUID;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

public class DeletePublicationHandlerTest extends ResourcesLocalTest {

    public static final String IDENTIFIER = "identifier";
    public static final String WILDCARD = "*";
    public static final String SOME_USER = "some_other_user";
    public static final URI SOME_CUSTOMER = URI.create("https://www.example.org");
    private DeletePublicationHandler handler;
    private ResourceService publicationService;
    private Environment environment;
    private ByteArrayOutputStream outputStream;
    private Context context;

    @BeforeEach
    public void setUp() {
        init();
        prepareEnvironment();
        var httpClient = new FakeHttpClient();
        publicationService = new ResourceService(client, httpClient, Clock.systemDefaultZone());
        handler = new DeletePublicationHandler(publicationService, environment);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    private void prepareEnvironment() {
        environment = Mockito.mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
    }

    @Test
    void handleRequestReturnsAcceptedWhenOnDraftPublication() throws IOException, ApiGatewayException {
        Publication publication = publicationService.createPublication(publicationWithoutIdentifier());

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
            .withHeaders(TestHeaders.getRequestHeaders())
            .withPathParameters(singletonMap(IDENTIFIER, publication.getIdentifier().toString()))
            .withFeideId(publication.getOwner())
            .withCustomerId(publication.getPublisher().getId().toString())
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Void> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(HttpStatus.SC_ACCEPTED, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsBadRequestWhenOnPublishedPublication() throws IOException, ApiGatewayException {
        Publication publication = publicationService.createPublication(publicationWithoutIdentifier());

        publicationService.publishPublication(createUserInstance(publication), publication.getIdentifier());

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
            .withHeaders(TestHeaders.getRequestHeaders())
            .withPathParameters(singletonMap(IDENTIFIER, publication.getIdentifier().toString()))
            .withFeideId(publication.getOwner())
            .withCustomerId(publication.getPublisher().getId().toString())
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo((HttpURLConnection.HTTP_BAD_REQUEST))));
    }

    private UserInstance createUserInstance(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }

    @Test
    void handleRequestReturnsErrorWhenNonExistingPublication() throws IOException {
        UUID identifier = UUID.randomUUID();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
            .withHeaders(TestHeaders.getRequestHeaders())
            .withPathParameters(singletonMap(IDENTIFIER, identifier.toString()))
            .withCustomerId(SOME_CUSTOMER.toString())
            .withFeideId(SOME_USER)
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsErrorWhenCallerIsNotOwnerOfPublication() throws IOException, ApiGatewayException {
        Publication createdPublication = publicationService.createPublication(publicationWithoutIdentifier());

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
            .withHeaders(TestHeaders.getRequestHeaders())
            .withPathParameters(singletonMap(IDENTIFIER, createdPublication.getIdentifier().toString()))
            .withFeideId(SOME_USER)
            .withCustomerId(createdPublication.getPublisher().getId().toString())
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        // Return BadRequest because Dynamo cannot distinguish between the primary key (containing the user info)
        // being wrong or the status of the resource not being "DRAFT"
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsBadRequestdWhenAlreadyMarkedForDeletionPublication()
        throws IOException, ApiGatewayException {
        Publication publication = publicationService.createPublication(publicationWithoutIdentifier());
        markForDeletion(publication);

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
            .withHeaders(TestHeaders.getRequestHeaders())
            .withPathParameters(singletonMap(IDENTIFIER, publication.getIdentifier().toString()))
            .withFeideId(publication.getOwner())
            .withCustomerId(publication.getPublisher().getId().toString())
            .build();

        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(HttpStatus.SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    private void markForDeletion(Publication publication) throws ApiGatewayException {
        UserInstance userInstance = new UserInstance(publication.getOwner(), publication.getPublisher().getId());
        publicationService.markPublicationForDeletion(userInstance, publication.getIdentifier());
    }
}

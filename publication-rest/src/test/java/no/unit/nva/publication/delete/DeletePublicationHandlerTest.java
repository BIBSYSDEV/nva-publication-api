package no.unit.nva.publication.delete;

import static java.util.Collections.singletonMap;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.util.UUID;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

class DeletePublicationHandlerTest extends ResourcesLocalTest {
    
    public static final String WILDCARD = "*";
    public static final String SOME_USER = "some_other_user";
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    public static final URI SOME_CUSTOMER = URI.create("https://www.example.org");
    private DeletePublicationHandler handler;
    private ResourceService publicationService;
    private IdentityServiceClient identityServiceClient;
    private Environment environment;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private GetExternalClientResponse getExternalClientResponse;

    @BeforeEach
    public void setUp() throws NotFoundException {
        init();
        prepareEnvironment();
        prepareIdentityServiceClient();
        publicationService = new ResourceService(client, Clock.systemDefaultZone());
        handler = new DeletePublicationHandler(publicationService, environment, identityServiceClient);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }
    
    @Test
    void handleRequestReturnsAcceptedWhenOnDraftPublication() throws IOException, BadRequestException {
        
        Publication publication = createPublication();
    
        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(
                                          singletonMap(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                                      .withUserName(publication.getResourceOwner().getOwner().getValue())
                                      .withCurrentCustomer(publication.getPublisher().getId())
                                      .build();
        
        handler.handleRequest(inputStream, outputStream, context);
        
        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertEquals(SC_ACCEPTED, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsAcceptedWhenOnDraftPublicationAndClientIsExternal() throws IOException,
                                                                                        BadRequestException {
        Publication createdPublication = createPublicationWithExternalOwner();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER,
                                                                       createdPublication.getIdentifier().toString()))
                                      .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                                      .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertEquals(SC_ACCEPTED, gatewayResponse.getStatusCode());
    }
    
    @Test
    void handleRequestReturnsBadRequestWhenOnPublishedPublication() throws IOException, ApiGatewayException {
        Publication publication = createPublication();
        
        publicationService.publishPublication(createUserInstance(publication), publication.getIdentifier());
    
        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(
                                          singletonMap(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                                      .withUserName(publication.getResourceOwner().getOwner().getValue())
                                      .withCurrentCustomer(publication.getPublisher().getId())
                                      .build();
        
        handler.handleRequest(inputStream, outputStream, context);
        
        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo((SC_BAD_REQUEST))));
    }
    
    @Test
    void handleRequestReturnsErrorWhenNonExistingPublication() throws IOException {
        UUID identifier = UUID.randomUUID();
    
        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER, identifier.toString()))
                                      .withCurrentCustomer(SOME_CUSTOMER)
                                      .withUserName(SOME_USER)
                                      .build();
        
        handler.handleRequest(inputStream, outputStream, context);
        
        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }
    
    @Test
    void handleRequestReturnsErrorWhenCallerIsNotOwnerOfPublication() throws IOException, ApiGatewayException {
        Publication createdPublication = createPublication();
    
        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER,
                                          createdPublication.getIdentifier().toString()))
                                      .withUserName(SOME_USER)
                                      .withCurrentCustomer(createdPublication.getPublisher().getId())
                                      .build();
        
        handler.handleRequest(inputStream, outputStream, context);
        
        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        // Return BadRequest because Dynamo cannot distinguish between the primary key (containing the user info)
        // being wrong or the status of the resource not being "DRAFT"
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsErrorWhenCallerIsNotOwnerOfPublicationAndCalledIsExternalClient()
        throws IOException, BadRequestException {
        Publication createdPublication = createPublication();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER,
                                                                       createdPublication.getIdentifier().toString()))
                                      .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                                      .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        // Return BadRequest because Dynamo cannot distinguish between the primary key (containing the user info)
        // being wrong or the status of the resource not being "DRAFT"
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsUnauthorizedWhenCallerIsMissingClientId()
        throws IOException, NotFoundException, BadRequestException {
        prepareIdentityServiceClientForNotFound();
        Publication createdPublication = createPublication();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER,
                                                                       createdPublication.getIdentifier().toString()))
                                      .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(SC_UNAUTHORIZED, gatewayResponse.getStatusCode());
    }


    @Test
    void handleRequestReturnsBadRequestWhenAlreadyMarkedForDeletionPublication()
        throws IOException, ApiGatewayException {
        Publication publication = createPublication();
        markForDeletion(publication);
    
        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(
                                          singletonMap(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                                      .withUserName(publication.getResourceOwner().getOwner().getValue())
                                      .withCurrentCustomer(publication.getPublisher().getId())
                                      .build();
        
        handler.handleRequest(inputStream, outputStream, context);
        
        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpStatus.SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    private void prepareIdentityServiceClient() throws NotFoundException {
        identityServiceClient = Mockito.mock(IdentityServiceClient.class);

        getExternalClientResponse = new GetExternalClientResponse(
            EXTERNAL_CLIENT_ID,
            "someone@123",
            randomUri(),
            randomUri()
        );
        when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);
    }

    private void prepareIdentityServiceClientForNotFound() throws NotFoundException {
        identityServiceClient = Mockito.mock(IdentityServiceClient.class);
        when(identityServiceClient.getExternalClient(any())).thenThrow(NotFoundException.class);
    }
    
    private void prepareEnvironment() {
        environment = Mockito.mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
    }
    
    private Publication createPublication() throws BadRequestException {
        var publication = PublicationGenerator.randomPublication();
        var userInstance =
            UserInstance.create(publication.getResourceOwner().getOwner().getValue(), publication.getPublisher().getId());
        return Resource.fromPublication(publication).persistNew(publicationService, userInstance);
    }

    private Publication createPublicationWithExternalOwner() throws BadRequestException {
        var publication = PublicationGenerator.randomPublication();
        var owner = new ResourceOwner(
            new Username(getExternalClientResponse.getActingUser()),
            getExternalClientResponse.getCristinUrgUri()
        );
        var userInstance =
            UserInstance.create(owner, getExternalClientResponse.getCustomerUri());
        return Resource.fromPublication(publication).persistNew(publicationService, userInstance);
    }
    
    private UserInstance createUserInstance(Publication publication) {
        return UserInstance.create(publication.getResourceOwner().getOwner().getValue(), publication.getPublisher().getId());
    }
    
    private void markForDeletion(Publication publication) throws ApiGatewayException {
        UserInstance userInstance = UserInstance.create(publication.getResourceOwner().getOwner().getValue(),
            publication.getPublisher().getId());
        publicationService.markPublicationForDeletion(userInstance, publication.getIdentifier());
    }
}

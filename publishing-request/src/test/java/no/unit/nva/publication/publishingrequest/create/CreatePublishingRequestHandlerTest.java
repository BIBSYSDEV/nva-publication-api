package no.unit.nva.publication.publishingrequest.create;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.SUPPORT_MESSAGE_PATH;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class CreatePublishingRequestHandlerTest extends ResourcesLocalTest {

    PublishingRequestService requestService;
    Clock mockClock;
    private CreatePublishingRequestHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private ResourceService resourceService;

    @BeforeEach
    public void initialize() {
        init();
        mockClock = setupMockClock();
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        resourceService = new ResourceService(client, mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        handler = new CreatePublishingRequestHandler(requestService);
    }

    @Test
    void shouldAcceptTypedSupportRequest() throws ApiGatewayException, IOException {
        var existingPublication =
            createAndPersistPublication(resourceService);

        var apiRequest = ownerRequestsToPublishOwnPublication(existingPublication);
        handler.handleRequest(apiRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    void shouldPersistThePublishPublicationRequestWhenPublicationExistsAndTheOwnerIsDoingTheRequest()
        throws ApiGatewayException, IOException {
        var existingPublication = createAndPersistPublication(resourceService);
        var apiRequest = ownerRequestsToPublishOwnPublication(existingPublication);
        handler.handleRequest(apiRequest, outputStream, context);
        var resourceOwner = UserInstance.fromPublication(existingPublication);
        var persistedRequest =
            requestService.getPublishingRequest(resourceOwner, existingPublication.getIdentifier());
        assertThat(persistedRequest.getResourceIdentifier(), is(equalTo(existingPublication.getIdentifier())));
    }

    @Test
    void shouldReturnLocationHeaderWithUriOfPersistedPublishingRequest()
        throws ApiGatewayException, IOException {
        var existingPublication =
            createAndPersistPublication(resourceService);
        var apiRequest = ownerRequestsToPublishOwnPublication(existingPublication);
        handler.handleRequest(apiRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        var actualLocationHeader = URI.create(response.getHeaders().get(HttpHeaders.LOCATION));
        var publishingRequest = fetchPersistedPublishingRequest(existingPublication);
        var expectedLocationHeader = createExpectedLocationHeader(existingPublication, publishingRequest);
        assertThat(actualLocationHeader, is(equalTo(expectedLocationHeader)));
    }

    @Test
    void shouldNotRevealThatPublicationExistsWhenRequesterIsNotThePublicationOwner()
        throws IOException, ApiGatewayException {
        var existingPublication = createAndPersistPublication(resourceService);
        var notOwner = randomString();
        var apiRequest = requestToPublishPublication(existingPublication, notOwner);
        handler.handleRequest(apiRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    private URI createExpectedLocationHeader(Publication existingPublication, PublishingRequest publishingRequest) {
        return UriWrapper.fromHost(API_HOST)
            .addChild(PUBLICATION_PATH)
            .addChild(existingPublication.getIdentifier().toString())
            .addChild(SUPPORT_MESSAGE_PATH)
            .addChild(publishingRequest.getIdentifier().toString())
            .getUri();
    }

    private PublishingRequest fetchPersistedPublishingRequest(Publication existingPublication)
        throws NotFoundException {
        var resourceOwner = UserInstance.fromPublication(existingPublication);
        return requestService.getPublishingRequest(resourceOwner, existingPublication.getIdentifier());
    }

    private InputStream requestToPublishPublication(Publication existingPublication, String requester)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<PublicationPublishRequest>(JsonUtils.dtoObjectMapper)
            .withBody(new PublicationPublishRequest())
            .withNvaUsername(requester)
            .withCustomerId(existingPublication.getPublisher().getId())
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, existingPublication.getIdentifier().toString()))
            .build();
    }

    private InputStream ownerRequestsToPublishOwnPublication(Publication existingPublication)
        throws JsonProcessingException {
        return requestToPublishPublication(existingPublication, existingPublication.getResourceOwner().getOwner());
    }
}

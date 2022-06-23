package no.unit.nva.publication.publishingrequest.update;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;

import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createUpdatePublishingRequestMissingAccessRight;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createUpdatePublishingRequestWithAccessRight;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createUpdateRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.mockEnvironment;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.randomResourceOwner;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

public class UpdatePublishingRequestHandlerTest extends ResourcesLocalTest {

    public static final String ILLEGAL_PUBLICATION_IDENTIFIER = "asdasøldksaøkdsaøk";
    private UpdatePublishingRequestHandler updatePublishingRequestHandler;

    private Context context;
    private ResourceService resourceService;
    private Clock mockClock;
    PublishingRequestService requestService;

    @BeforeEach
    public void initialize() {
        init();
        mockClock = setupMockClock();
        context = mock(Context.class);
        resourceService = new ResourceService(client,  mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        updatePublishingRequestHandler = new UpdatePublishingRequestHandler(requestService, mockEnvironment());
    }

    @Test
    public void shouldReturnAcceptedWhenPublishingRequestIsApproved() throws IOException, ApiGatewayException {
        var publication =
                createAndPersistPublication(resourceService, randomOrganization(), randomResourceOwner());
        createAndPersistPublishingRequest(requestService, publication, mockEnvironment(), context);
        var updateRequest = createUpdateRequest();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        updatePublishingRequestHandler.handleRequest(
                createUpdatePublishingRequestWithAccessRight(publication,
                        updateRequest,
                        publication.getPublisher().getId(),
                        publication.getIdentifier().toString()),
                outputStream,
                context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_ACCEPTED));
    }

    @Test
    public void shouldReturnUnauthorizedWhenUserHasNoAccessRight() throws IOException, ApiGatewayException {
        var publication =
                createAndPersistPublication(resourceService, randomOrganization(), randomResourceOwner());
        createAndPersistPublishingRequest(requestService, publication, mockEnvironment(), context);
        var updateRequest = createUpdateRequest();
        var outputStream = new ByteArrayOutputStream();
        var customerId = randomUri();
        var updatePublishingRequest =
                createUpdatePublishingRequestMissingAccessRight(publication,
                        updateRequest,
                        customerId,
                        publication.getIdentifier().toString());
        updatePublishingRequestHandler.handleRequest(updatePublishingRequest,
                outputStream,
                context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));
    }

    @Test
    public void shouldReturnBadRequestWhenIllegalIdentifier() throws IOException, ApiGatewayException {
        var publication =
                createAndPersistPublication(resourceService, randomOrganization(), randomResourceOwner());
        createAndPersistPublishingRequest(requestService, publication, mockEnvironment(), context);
        var updateRequest = createUpdateRequest();
        var outputStream = new ByteArrayOutputStream();
        var customerId = randomUri();
        var updatePublishingRequest =
                createUpdatePublishingRequestWithAccessRight(publication,
                        updateRequest,
                        customerId,
                        ILLEGAL_PUBLICATION_IDENTIFIER);
        updatePublishingRequestHandler.handleRequest(updatePublishingRequest,
                outputStream,
                context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_BAD_REQUEST));
    }
}

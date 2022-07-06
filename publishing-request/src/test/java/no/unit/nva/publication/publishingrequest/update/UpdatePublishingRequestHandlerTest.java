package no.unit.nva.publication.publishingrequest.update;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createUpdatePublishingRequestMissingAccessRight;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createUpdatePublishingRequestWithAccessRight;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createUpdateRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdatePublishingRequestHandlerTest extends ResourcesLocalTest {

    private UpdatePublishingRequestHandler updatePublishingRequestHandler;

    private Context context;
    private ResourceService resourceService;
    PublishingRequestService requestService;

    @BeforeEach
    public void initialize() {
        init();
        Clock mockClock = setupMockClock();
        context = mock(Context.class);
        resourceService = new ResourceService(client, mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        updatePublishingRequestHandler = new UpdatePublishingRequestHandler(requestService);
    }

    @Test
    void shouldReturnAcceptedWhenPublishingRequestIsApproved() throws IOException, ApiGatewayException {
        var publication =
            createAndPersistPublication(resourceService);
        var existingPublishingRequest= createAndPersistPublishingRequest(requestService, publication);
        var updateRequest = createUpdateRequest();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        var httpRequest = createUpdatePublishingRequestWithAccessRight(publication, updateRequest,
                                                                       publication.getPublisher().getId(),
                                                                       existingPublishingRequest.getIdentifier());
        updatePublishingRequestHandler.handleRequest(httpRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_ACCEPTED));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserHasNoAccessRight() throws IOException, ApiGatewayException {
        var publication =
            createAndPersistPublication(resourceService);
        var existingPublishingRequest=createAndPersistPublishingRequest(requestService, publication);
        var updateRequest = createUpdateRequest();
        var outputStream = new ByteArrayOutputStream();
        var customerId = randomUri();
        var updatePublishingRequest =
            createUpdatePublishingRequestMissingAccessRight(publication,
                                                            updateRequest,
                                                            customerId,
                                                            existingPublishingRequest.getIdentifier());
        updatePublishingRequestHandler.handleRequest(updatePublishingRequest,
                                                     outputStream,
                                                     context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));
    }

    @Test
    void shouldReturnNotFoundWhenIdentifierIsUnknown() throws IOException, ApiGatewayException {
        var publication =
            createAndPersistPublication(resourceService);
        createAndPersistPublishingRequest(requestService, publication);
        var updateRequest = createUpdateRequest();
        var outputStream = new ByteArrayOutputStream();
        var customerId = randomUri();
        var updatePublishingRequest =
            createUpdatePublishingRequestWithAccessRight(publication,
                                                         updateRequest,
                                                         customerId,
                                                         SortableIdentifier.next());
        updatePublishingRequestHandler.handleRequest(updatePublishingRequest,
                                                     outputStream,
                                                     context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_NOT_FOUND));
    }
}

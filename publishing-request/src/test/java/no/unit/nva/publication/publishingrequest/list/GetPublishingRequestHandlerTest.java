package no.unit.nva.publication.publishingrequest.list;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createGetPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createGetPublishingRequestMissingAccessRightApprove;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class GetPublishingRequestHandlerTest extends ResourcesLocalTest {

    private GetPublishingRequestHandler handler;
    private Context context;
    private ResourceService resourceService;
    private Clock mockClock;
    PublishingRequestService requestService;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void initialize() {
        init();
        mockClock = setupMockClock();
        context = mock(Context.class);
        resourceService = new ResourceService(client, mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        handler = new GetPublishingRequestHandler(requestService);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldAcceptGetRequestWithPublicationIdentifierAndPublishingRequestIdentiferAsPathParameters()
        throws ApiGatewayException, IOException {
        var publication = createAndPersistPublication(resourceService);
        var publishingRequestId = createAndPersistPublishingRequest(requestService, publication, context);
        var getRequest = createGetPublishingRequest(publishingRequestId, publication);
        handler.handleRequest(getRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, PublishingRequestDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldReturnPublishingRequestWithStatusPendingForNewPublishingRequest()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistPublication(resourceService);
        var publishingRequestId= createAndPersistPublishingRequest(requestService, publication, context);
        var getRequest = createGetPublishingRequest(publishingRequestId,publication);
        handler.handleRequest(getRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, PublishingRequest.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_OK));
        var responseObject = response.getBodyObject(PublishingRequest.class);
        assertThat(responseObject.getStatus(), equalTo(PublishingRequestStatus.PENDING));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsMissingAccessRight()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistPublication(resourceService);
        createAndPersistPublishingRequest(requestService, publication, context);
        var outputStream = new ByteArrayOutputStream();
        var getRequest =
            createGetPublishingRequestMissingAccessRightApprove(publication, publication.getPublisher().getId());
        handler.handleRequest(getRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));
    }
}

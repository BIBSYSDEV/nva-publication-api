package no.unit.nva.publication.publishingrequest.list;

import com.amazonaws.services.lambda.runtime.Context;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;

import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createGetPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createGetPublishingRequestMissingAccessRightApprove;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.mockEnvironment;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.randomResourceOwner;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

public class GetPublishingRequestHandlerTest extends ResourcesLocalTest {

    private GetPublishingRequestHandler handler;
    private Context context;
    private ResourceService resourceService;
    private Clock mockClock;
    PublishingRequestService requestService;

    @BeforeEach
    public void initialize() {
        init();
        mockClock = setupMockClock();
        context = mock(Context.class);
        resourceService = new ResourceService(client, mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        handler = new GetPublishingRequestHandler(requestService, mockEnvironment());
    }

    @Test
    public void shouldReturnPublishingRequestWithStatusPendingForNewPublishingRequest()
            throws IOException, ApiGatewayException {
        var publication = createAndPersistPublication(resourceService,
                randomOrganization(),
                randomResourceOwner());
        createAndPersistPublishingRequest(requestService, publication, mockEnvironment(), context);
        var outputStream = new ByteArrayOutputStream();
        var getRequest = createGetPublishingRequest(publication, publication.getPublisher().getId());
        handler.handleRequest(getRequest, outputStream, context);
        var response =
                GatewayResponse.fromOutputStream(outputStream, PublishingRequest.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_OK));
        assertThat(response.getBodyObject(PublishingRequest.class).getStatus(),
                equalTo(PublishingRequestStatus.PENDING));
    }

    @Test
    public void shouldReturnUnauthorizedWhenUserIsMissingAccessRight()
            throws IOException, ApiGatewayException {
        var publication =
                createAndPersistPublication(resourceService, randomOrganization(), randomResourceOwner());
        createAndPersistPublishingRequest(requestService, publication, mockEnvironment(), context);
        var outputStream = new ByteArrayOutputStream();
        var getRequest =
                createGetPublishingRequestMissingAccessRightApprove(publication, publication.getPublisher().getId());
        handler.handleRequest(getRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));
    }
}

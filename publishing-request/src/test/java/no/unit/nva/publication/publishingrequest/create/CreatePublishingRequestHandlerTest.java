package no.unit.nva.publication.publishingrequest.create;

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
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.mockEnvironment;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.randomResourceOwner;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

public class CreatePublishingRequestHandlerTest extends ResourcesLocalTest {

    private CreatePublishingRequestHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private ResourceService resourceService;
    PublishingRequestService requestService;
    Clock mockClock;

    @BeforeEach
    public void initialize() {
        init();
        mockClock = setupMockClock();
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        resourceService = new ResourceService(client,  mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        handler = new CreatePublishingRequestHandler(requestService, mockEnvironment());
    }

    @Test
    public void createPublishingRequestReturnsCreated() throws IOException, ApiGatewayException {
        var publication = createAndPersistPublication(resourceService,
                randomOrganization(),
                randomResourceOwner());
        handler.handleRequest(
                createPublishingRequest(publication, publication.getPublisher().getId()), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_CREATED));
    }
}

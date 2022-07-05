package no.unit.nva.publication.publishingrequest.list;

import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createListPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createListPublishingRequestWithMissingAccessRightToApprove;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.publishingrequest.SearchResponse;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListPublishingRequestHandlerTest extends ResourcesLocalTest {

    private ListPublishingRequestHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private ResourceService resourceService;
    private Clock mockClock;
    PublishingRequestService requestService;

    @BeforeEach
    public void initialize() {
        init();
        mockClock = setupMockClock();
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        resourceService = new ResourceService(client, mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        handler = new ListPublishingRequestHandler(requestService);
    }

    @Test
    public void shouldReturnSearchResponseWithListOfPublishingRequests() throws IOException, ApiGatewayException {
        var publication =
            createAndPersistPublication(resourceService);
        var publishingRequestId = createAndPersistPublishingRequest(requestService, publication, context);

        handler.handleRequest(createListPublishingRequest(publication), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, SearchResponse.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_OK));
        final SearchResponse searchResponse = response.getBodyObject(SearchResponse.class);
        assertNotNull(searchResponse.getId());
    }

    @Test
    public void shouldReturnUnauthorizedWhenUserHasNoAccessRight() throws IOException, ApiGatewayException {
        Publication publication = createAndPersistPublication(resourceService);

        createAndPersistPublishingRequest(requestService, publication, context);

        handler.handleRequest(
            createListPublishingRequestWithMissingAccessRightToApprove(publication,
                                                                       publication.getPublisher().getId()),
            outputStream,
            context);
        var response = GatewayResponse.fromOutputStream(outputStream, SearchResponse.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));
    }

    @Test
    void shouldImproveCodeCoverageGetRequestUri() {
        RequestInfo requestInfo = mock(RequestInfo.class);
        when(requestInfo.getDomainName()).thenReturn("localhost");
        assertNotNull(handler.getRequestUri(requestInfo));
    }
}

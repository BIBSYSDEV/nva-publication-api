package no.unit.nva.publication.publishingrequest.list;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createGetPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createGetPublishingRequestMissingAccessRightApprove;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class GetPublishingRequestHandlerTest extends ResourcesLocalTest {

    PublishingRequestService requestService;
    private GetPublishingRequestHandler handler;
    private Context context;
    private ResourceService resourceService;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void initialize() {
        init();
        Clock mockClock = setupMockClock();
        context = mock(Context.class);
        resourceService = new ResourceService(client, mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        handler = new GetPublishingRequestHandler(requestService);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldAcceptGetRequestWithPublicationIdentifierAndPublishingRequestIdentifierAsPathParameters()
        throws ApiGatewayException, IOException {
        var publication = PublishingRequestTestUtils.createAndPersistDraftPublication(resourceService);
        var publishingRequest = createAndPersistPublishingRequest(requestService, publication);
        var getRequest = createGetPublishingRequest(publishingRequest);
        handler.handleRequest(getRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, PublishingRequestDto.class);
        var actualResponseObject = response.getBodyObject(PublishingRequestDto.class);
        var expectedResponseObject = PublishingRequestDto.fromPublishingRequest(publishingRequest);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(actualResponseObject.getStatus(), equalTo(publishingRequest.getStatus()));
        assertThat(actualResponseObject, is(equalTo(expectedResponseObject)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsMissingAccessRight()
        throws IOException, ApiGatewayException {
        var publication = PublishingRequestTestUtils.createAndPersistDraftPublication(resourceService);
        createAndPersistPublishingRequest(requestService, publication);
        var outputStream = new ByteArrayOutputStream();
        var getRequest =
            createGetPublishingRequestMissingAccessRightApprove(publication, publication.getPublisher().getId());
        handler.handleRequest(getRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), equalTo(HTTP_UNAUTHORIZED));
    }

    @Test
    void shouldReturnNotFoundWhenSuppliedUriContainsNonExistentIdentifiers() throws IOException {
        var unsavedPublication = PublicationGenerator.randomPublication();
        var unsavedPublishingRequest = createPublishingRequest(unsavedPublication);
        unsavedPublishingRequest.setIdentifier(SortableIdentifier.next());
        var getRequest = createGetPublishingRequest(unsavedPublishingRequest);
        handler.handleRequest(getRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
    }

    @Test
    void shouldReturnNotFoundWhenSuppliedUriContainsInvalidIdentifier() throws IOException {
        var unsavedPublication = PublicationGenerator.randomPublication();
        var unsavedPublishingRequest = createPublishingRequest(unsavedPublication);
        var getRequest = createGetPublishingRequest(unsavedPublishingRequest, randomString());
        handler.handleRequest(getRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
    }
}

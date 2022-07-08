package no.unit.nva.publication.publishingrequest.update;

import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistDraftPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublishingRequest;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createUpdatePublishingRequestMissingAccessRight;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createUpdatePublishingRequestWithAccessRight;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.publishingrequest.PublishingRequestUpdate;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublishingRequestUpdateHandlerTest extends ResourcesLocalTest {

    public static final PublishingRequestStatus IRRELEVANT = null;
    PublishingRequestService requestService;
    private UpdatePublishingRequestHandler updatePublishingRequestHandler;
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
        outputStream = new ByteArrayOutputStream();
        updatePublishingRequestHandler = new UpdatePublishingRequestHandler(requestService);
    }

    @Test
    void shouldReturnAcceptedWhenUserIsAuthorizedToApproveAndPublishingRequestIsApproved()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistDraftPublication(resourceService);
        var existingPublishingRequest = createAndPersistPublishingRequest(requestService, publication);
        var updateRequest = PublishingRequestUpdate.createApproved();
        var httpRequest = createUpdatePublishingRequestWithAccessRight(updateRequest, existingPublishingRequest);
        updatePublishingRequestHandler.handleRequest(httpRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_ACCEPTED));
    }

    @Test
    void shouldPersistUpdatedPublishingRequest()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistDraftPublication(resourceService);
        var existingPublishingRequest = createAndPersistPublishingRequest(requestService, publication);
        var updateRequest = PublishingRequestUpdate.createApproved();
        var httpRequest = createUpdatePublishingRequestWithAccessRight(updateRequest, existingPublishingRequest);
        updatePublishingRequestHandler.handleRequest(httpRequest, outputStream, context);
        var updatedPublishingRequest = requestService.getPublishingRequest(existingPublishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(updateRequest.getPublishingRequestStatus())));
        assertThat(updatedPublishingRequest.getRowVersion(),
                   is(not(equalTo(existingPublishingRequest.getRowVersion()))));
    }

    @Test
    void shouldReturnUnauthorizedWhenUnauthorizedUserIsAttemptingToChangePublishingRequestStatus()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistDraftPublication(resourceService);
        var existingPublishingRequest = createAndPersistPublishingRequest(requestService, publication);
        var updateRequest = PublishingRequestUpdate.createApproved();
        var updatePublishingRequest =
            createUpdatePublishingRequestMissingAccessRight(updateRequest, existingPublishingRequest);
        updatePublishingRequestHandler.handleRequest(updatePublishingRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));
    }

    @Test
    void shouldAllowOwnerToSendMessageRelatedToAnExistingPublishingRequestWhenStatusIsNotSpecified()
        throws IOException, ApiGatewayException {
        fail();
    }

    @Test
    void shouldReturnNotFoundWhenIdentifierIsUnknown() throws IOException, ApiGatewayException {
        var publication = createAndPersistDraftPublication(resourceService);
        createAndPersistPublishingRequest(requestService, publication);
        var updateRequest = PublishingRequestUpdate.createApproved();
        var nonExistingIdentifier = SortableIdentifier.next();
        var requestWithUnknownIdentifier =
            PublishingRequest.create(UserInstance.fromPublication(publication),
                                     publication.getIdentifier(),
                                     nonExistingIdentifier,
                                     IRRELEVANT);
        var updatePublishingRequest =
            createUpdatePublishingRequestWithAccessRight(updateRequest, requestWithUnknownIdentifier);
        updatePublishingRequestHandler.handleRequest(updatePublishingRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpURLConnection.HTTP_NOT_FOUND));
    }
}

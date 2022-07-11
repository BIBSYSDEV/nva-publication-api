package no.unit.nva.publication.publishingrequest.update;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.fail;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdatePublishingRequestHandlerTest extends ResourcesLocalTest {

    private UpdatePublishingRequestHandler handler;
    private ResourceService resourceService;
    private PublishingRequestService requestService;
    private ByteArrayOutputStream outputStream;
    private static final Context CONTEXT = new FakeContext();

    @BeforeEach
    public void setup() {
        super.init();
        var clock = Clock.systemDefaultZone();
        this.resourceService = new ResourceService(super.client, clock);
        this.requestService = new PublishingRequestService(client, clock);
        this.outputStream = new ByteArrayOutputStream();
        this.handler = new UpdatePublishingRequestHandler();
    }

    @AfterEach
    public void tearDown() {
        super.shutdown();
    }

    @Test
    void shouldReturnApprovedPublishingRequestCaseWhenInputIsPublishingRequestApprovalAndUserIsAuthorizedToApprove()
        throws IOException, ApiGatewayException {
        var publication = createPersistedPublication();
        var publishingRequest = createPublishingRequest(publication);

        var httpRequest = createAuthorizedApprovalRequest(publication, publishingRequest);
        handler.handleRequest(httpRequest, outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream,
                                                        PublishingRequestCaseDto.class);
        var responseBody = response.getBodyObject(PublishingRequestCaseDto.class);

        var expectedPublishingRequest = publishingRequest.approve();
        var expectedResponseBody = PublishingRequestCaseDto.createResponseObject(expectedPublishingRequest);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(responseBody, is(equalTo(expectedResponseBody)));
        assertThat(responseBody.getStatus(), is(equalTo(PublishingRequestStatus.APPROVED)));
    }

    @Test
    void shouldPersistPublishingRequestApprovalWhenInputIsPublishingRequestApprovalAndUserIsAuthorizedToApprove() {
        fail();
    }

    private InputStream createAuthorizedApprovalRequest(Publication publication,
                                                        PublishingRequestCase publishingRequest)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<PublishingRequestApproval>(JsonUtils.dtoObjectMapper)
            .withBody(new PublishingRequestApproval())
            .withNvaUsername(randomString())
            .withCustomerId(publication.getPublisher().getId())
            .withAccessRights(publication.getPublisher().getId(), AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString(),
                                       PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER,
                                       publishingRequest.getIdentifier().toString()))
            .build();
    }

    private PublishingRequestCase createPublishingRequest(Publication publication) throws ApiGatewayException {
        var publishingRequest =
            PublishingRequestCase.createOpeningCaseObject(UserInstance.fromPublication(publication),
                                                          publication.getIdentifier());
        publishingRequest = requestService.createPublishingRequest(publishingRequest);
        return publishingRequest;
    }

    private Publication createPersistedPublication() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}
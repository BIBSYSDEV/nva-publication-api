package no.unit.nva.publication.publishingrequest.update;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.SUPPORT_CASE_PATH;
import static no.unit.nva.publication.model.business.PublishingRequestStatus.PENDING;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.publishingrequest.update.UpdatePublishingRequestHandler.AUTHORIZATION_ERROR;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingRequestStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

class UpdatePublishingRequestHandlerTest extends ResourcesLocalTest {
    
    private static final Context CONTEXT = new FakeContext();
    private UpdatePublishingRequestHandler handler;
    private ResourceService resourceService;
    private PublishingRequestService requestService;
    private ByteArrayOutputStream outputStream;
    
    public static Stream<InputStream> invalidHttpRequests() throws JsonProcessingException {
        return Stream.of(publishingRequestWithIdConflictingWithRequestUri(),
            publishingRequestWithInvalidPublishingRequestCaseStatus());
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        var clock = Clock.systemDefaultZone();
        this.resourceService = new ResourceService(super.client, clock);
        this.requestService = new PublishingRequestService(client, clock);
        this.outputStream = new ByteArrayOutputStream();
        this.handler = new UpdatePublishingRequestHandler(requestService);
    }
    
    @AfterEach
    public void tearDown() {
        super.shutdown();
    }
    
    @ParameterizedTest(name = "should return Approved PublishingRequestCaseDto when authorized user approves case and "
                              + "case has status {0}")
    @EnumSource(value = PublishingRequestStatus.class, names = {"PENDING", "COMPLETED"})
    void shouldReturnApprovedPublishingRequestCaseWhenInputIsPublishingRequestApprovalAndUserIsAuthorizedToApprove(
        PublishingRequestStatus status
    )
        throws IOException, ApiGatewayException {
        var publication = createPersistedPublication();
        var publishingRequest = createPersistedPublishingRequest(publication, status);
        
        var httpRequest = createAuthorizedApprovalRequest(publication, publishingRequest).build();
        handler.handleRequest(httpRequest, outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream,
            PublishingRequestCaseDto.class);
        var responseBody = response.getBodyObject(PublishingRequestCaseDto.class);
        
        var expectedPublishingRequest = publishingRequest.approve();
        var expectedResponseBody = PublishingRequestCaseDto.createResponseObject(expectedPublishingRequest);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(responseBody, is(equalTo(expectedResponseBody)));
        assertThat(responseBody.getStatus(), is(equalTo(PublishingRequestStatus.COMPLETED)));
    }
    
    @Test
    void shouldPersistPublishingRequestApprovalWhenInputIsPublishingRequestApprovalAndUserIsAuthorizedToApprove()
        throws ApiGatewayException, IOException {
        var publication = createPersistedPublication();
        var publishingRequest = createPendingPublishingRequest(publication);
        
        var httpRequest = createAuthorizedApprovalRequest(publication, publishingRequest).build();
        handler.handleRequest(httpRequest, outputStream, CONTEXT);
        var response =
            GatewayResponse.fromOutputStream(outputStream, PublishingRequestCaseDto.class);
        var responseBody = response.getBodyObject(PublishingRequestCaseDto.class);
        var actualIdentifierInResponseBody = extractIdentifierFromDto(responseBody);
        var persistedRequest = requestService.fetchTicket(publishingRequest,PublishingRequestCase.class);
        assertThat(persistedRequest.getIdentifier(), is(equalTo(actualIdentifierInResponseBody)));
        assertThat(persistedRequest.getStatus(), is(equalTo(PublishingRequestStatus.COMPLETED)));
    }
    
    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthorizedToPerformPublishingRequestApprovals()
        throws ApiGatewayException, IOException {
        var publication = createPersistedPublication();
        var publishingRequest = createPendingPublishingRequest(publication);
        var httpRequest = createUnauthorizedApprovalRequest(publication, publishingRequest).build();
        handler.handleRequest(httpRequest, outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_UNAUTHORIZED)));
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(equalTo(AUTHORIZATION_ERROR)));
    }
    
    @ParameterizedTest(name = "should return BadRequest when input is invalid")
    @MethodSource("invalidHttpRequests")
    void shouldReturnBadRequestWhenSubmittedRequestContainsInvalidData(InputStream httpRequest) throws IOException {
        handler.handleRequest(httpRequest, outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    private static InputStream publishingRequestWithInvalidPublishingRequestCaseStatus()
        throws JsonProcessingException {
        var publicationIdentifier = SortableIdentifier.next();
        var publishingRequestIdentifier = SortableIdentifier.next();
        var invalidBody = createInvalidBody(publicationIdentifier, publishingRequestIdentifier);
        var customerId = randomUri();
        return constructHttpRequest(publicationIdentifier, publishingRequestIdentifier, invalidBody, customerId);
    }
    
    private static InputStream publishingRequestWithIdConflictingWithRequestUri() throws JsonProcessingException {
        var publicationIdentifier = SortableIdentifier.next();
        var publishingRequestIdentifier = SortableIdentifier.next();
        var requestUri = createRequestUri(publicationIdentifier, publishingRequestIdentifier);
        var wrongCaseId = requestUri.getParent().orElseThrow()
            .addChild(SortableIdentifier.next().toString())
            .getUri();
        var dto = new PublishingRequestCaseDto(wrongCaseId, PublishingRequestStatus.COMPLETED);
        var customerId = randomUri();
        return constructHttpRequest(publicationIdentifier, publishingRequestIdentifier, dto, customerId);
    }
    
    private static InputStream constructHttpRequest(SortableIdentifier publicationIdentifier,
                                                    SortableIdentifier publishingRequestIdentifier,
                                                    Object dto,
                                                    URI customerId) throws JsonProcessingException {
        return new HandlerRequestBuilder<>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(randomString())
            .withCustomerId(customerId)
            .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString())
            .withBody(dto)
            .withPathParameters(Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER,
                publicationIdentifier.toString(),
                PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER,
                publishingRequestIdentifier.toString()))
            .build();
    }
    
    private static ObjectNode createInvalidBody(SortableIdentifier publicationIdentifier,
                                                SortableIdentifier publishingRequestIdentifier) {
        var uri = PublishingRequestCaseDto.calculateId(publicationIdentifier, publishingRequestIdentifier);
        var invalidBody = JsonUtils.dtoObjectMapper.createObjectNode();
        invalidBody.put(PublishingRequestCaseDto.ID, uri.toString());
        invalidBody.put(PublishingRequestCaseDto.STATUS, randomString());
        return invalidBody;
    }
    
    private static UriWrapper createRequestUri(SortableIdentifier publicationIdentifier,
                                               SortableIdentifier publishingRequestIdentifier) {
        return UriWrapper.fromHost(API_HOST)
            .addChild(PUBLICATION_PATH)
            .addChild(publicationIdentifier.toString())
            .addChild(SUPPORT_CASE_PATH)
            .addChild(publishingRequestIdentifier.toString());
    }
    
    private static Map<String, String> constructPathParameters(Publication publication,
                                                               PublishingRequestCase publishingRequest) {
        return Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString(),
            PUBLISHING_REQUEST_IDENTIFIER_PATH_PARAMETER,
            publishingRequest.getIdentifier().toString());
    }
    
    private PublishingRequestCase createPendingPublishingRequest(Publication publication) throws ApiGatewayException {
        return createPersistedPublishingRequest(publication, PENDING);
    }
    
    private SortableIdentifier extractIdentifierFromDto(PublishingRequestCaseDto responseBody) {
        return attempt(responseBody::getId)
            .map(UriWrapper::fromUri)
            .map(UriWrapper::getLastPathElement)
            .map(SortableIdentifier::new)
            .orElseThrow();
    }
    
    private HandlerRequestBuilder<PublishingRequestCaseDto> createUnauthorizedApprovalRequest(
        Publication publication,
        PublishingRequestCase publishingRequest
    ) throws JsonProcessingException {
        return new HandlerRequestBuilder<PublishingRequestCaseDto>(JsonUtils.dtoObjectMapper)
            .withBody(sampleApprovalDto(publishingRequest))
            .withNvaUsername(randomString())
            .withCustomerId(publication.getPublisher().getId())
            .withPathParameters(constructPathParameters(publication, publishingRequest));
    }
    
    private PublishingRequestCaseDto sampleApprovalDto(PublishingRequestCase publishingRequest) {
        var caseId =
            PublishingRequestCaseDto.calculateId(publishingRequest.getResourceIdentifier(),
                publishingRequest.getIdentifier());
        return new PublishingRequestCaseDto(caseId, PublishingRequestStatus.COMPLETED);
    }
    
    private HandlerRequestBuilder<PublishingRequestCaseDto> createAuthorizedApprovalRequest(
        Publication publication,
        PublishingRequestCase publishingRequest
    ) throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<PublishingRequestCaseDto>(JsonUtils.dtoObjectMapper)
            .withBody(sampleApprovalDto(publishingRequest))
            .withNvaUsername(randomString())
            .withCustomerId(customerId)
            .withPathParameters(constructPathParameters(publication, publishingRequest))
            .withAccessRights(customerId, AccessRight.APPROVE_PUBLISH_REQUEST.toString());
    }
    
    private PublishingRequestCase createPersistedPublishingRequest(Publication publication,
                                                                   PublishingRequestStatus status)
        throws ApiGatewayException {
        var publishingRequest =
            PublishingRequestCase.createOpeningCaseObject(UserInstance.fromPublication(publication),
                publication.getIdentifier());
        publishingRequest = requestService.createTicket(publishingRequest,PublishingRequestCase.class);
        
        if (PublishingRequestStatus.COMPLETED == status) {
            requestService.updatePublishingRequest(publishingRequest.approve());
        }
        return publishingRequest;
    }
    
    private Publication createPersistedPublication() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}
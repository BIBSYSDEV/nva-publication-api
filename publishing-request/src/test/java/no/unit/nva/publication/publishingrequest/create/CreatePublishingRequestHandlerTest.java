package no.unit.nva.publication.publishingrequest.create;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.SUPPORT_MESSAGE_PATH;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublicationAndMarkForDeletion;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createPersistAndPublishPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static no.unit.nva.publication.publishingrequest.PublishingRequestUtils.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.service.impl.PublishingRequestService.ALREADY_PUBLISHED_ERROR;
import static no.unit.nva.publication.service.impl.PublishingRequestService.MARKED_FOR_DELETION_ERROR;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class CreatePublishingRequestHandlerTest extends ResourcesLocalTest {

    PublishingRequestService requestService;
    Clock mockClock;
    private CreatePublishingRequestHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private ResourceService resourceService;

    @BeforeEach
    public void initialize() {
        init();
        mockClock = setupMockClock();
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        resourceService = new ResourceService(client, mockClock);
        requestService = new PublishingRequestService(client, mockClock);
        handler = new CreatePublishingRequestHandler(requestService);
    }

    @Test
    void shouldAcceptTypedSupportRequest() throws ApiGatewayException, IOException {
        var existingPublication = PublishingRequestTestUtils.createAndPersistDraftPublication(resourceService);
        var apiRequest = ownerRequestsToPublishOwnPublication(existingPublication);
        handler.handleRequest(apiRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    void shouldPersistThePublishingRequestWhenOwnerWantsToPublishExistingDraftPublication()
        throws ApiGatewayException, IOException {
        var draftPublication = PublishingRequestTestUtils.createAndPersistDraftPublication(resourceService);
        var httpResponse = ownerCreatesPublishingRequestForDraftPublication(draftPublication, Void.class);
        var persistedRequest = fetchPersistedPublishingRequest(draftPublication, httpResponse);

        assertThat(persistedRequest.getResourceIdentifier(), is(equalTo(draftPublication.getIdentifier())));
    }

    @Test
    void shouldReturnLocationHeaderWithUriOfPersistedPublishingRequest()
        throws ApiGatewayException, IOException {
        var draftPublication = PublishingRequestTestUtils.createAndPersistDraftPublication(resourceService);
        var httpResponse = ownerCreatesPublishingRequestForDraftPublication(draftPublication, Void.class);
        var actualLocationHeader = URI.create(httpResponse.getHeaders().get(HttpHeaders.LOCATION));
        var persistedRequest = fetchPersistedPublishingRequest(draftPublication, httpResponse);
        var expectedLocationHeader = createExpectedLocationHeader(draftPublication, persistedRequest);

        assertThat(actualLocationHeader, is(equalTo(expectedLocationHeader)));
    }

    @Test
    void shouldNotRevealThatPublicationExistsWhenRequesterIsNotThePublicationOwner()
        throws IOException, ApiGatewayException {
        var existingPublication = PublishingRequestTestUtils.createAndPersistDraftPublication(resourceService);
        var notOwner = randomString();
        var apiRequest = requestToPublishPublication(existingPublication, notOwner);
        handler.handleRequest(apiRequest, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnConflictWhenOwnerRequestsDuplicatePublishingRequest() throws ApiGatewayException, IOException {
        var draftPublication = PublishingRequestTestUtils.createAndPersistDraftPublication(resourceService);
        ownerCreatesPublishingRequestForDraftPublication(draftPublication, Void.class);
        var secondInvocation = ownerCreatesPublishingRequestForDraftPublication(draftPublication, Problem.class);
        var problem = secondInvocation.getBodyObject(Problem.class);
        assertThat(secondInvocation.getHeaders().get(HttpHeaders.CONTENT_TYPE),
                   is(equalTo(MediaTypes.APPLICATION_PROBLEM_JSON.toString())));
        assertThat(secondInvocation.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
        assertThat(problem.getDetail(), containsString(TransactionFailedException.ERROR_MESSAGE));
    }

    @Test
    void shouldReturnConflictWhenOwnerRequestsToPublishAlreadyPublishedPublication()
        throws ApiGatewayException, IOException {
        var publishedPublication = createPersistAndPublishPublication(resourceService);
        var response = ownerCreatesPublishingRequestForDraftPublication(publishedPublication, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
        assertThat(problem.getDetail(), containsString(ALREADY_PUBLISHED_ERROR));
    }

    @Test
    void shouldReturnConflictWhenOwnerRequestsToPublishPublicationMarkedForDeletion()
        throws ApiGatewayException, IOException {
        var publishedPublication = createAndPersistPublicationAndMarkForDeletion(resourceService);
        var response = ownerCreatesPublishingRequestForDraftPublication(publishedPublication, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
        assertThat(problem.getDetail(), containsString(MARKED_FOR_DELETION_ERROR));
    }

    @Test
    void shouldReturnNotFoundWhenTryingToPublishNonExistingPublication() throws IOException {
        var nonPersistedPublication = PublicationGenerator.randomPublication();
        var httpResponse =
            ownerCreatesPublishingRequestForDraftPublication(nonPersistedPublication, Void.class);
        assertThat(httpResponse.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    private <T> GatewayResponse<T> ownerCreatesPublishingRequestForDraftPublication(Publication draftPublication,
                                                                                    Class<T> responseType)
        throws IOException {
        var apiRequest = ownerRequestsToPublishOwnPublication(draftPublication);
        outputStream = new ByteArrayOutputStream();
        handler.handleRequest(apiRequest, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private PublishingRequest fetchPersistedPublishingRequest(Publication existingPublication,
                                                              GatewayResponse<Void> response)
        throws NotFoundException {
        var queryObject = createQueryObjectForCreatedPublishingRequest(existingPublication, response);
        return requestService.getPublishingRequest(queryObject);
    }

    private PublishingRequest createQueryObjectForCreatedPublishingRequest(Publication existingPublication,
                                                                           GatewayResponse<Void> response) {
        var requestIdentifier = attempt(() -> response.getHeaders().get(HttpHeaders.LOCATION))
            .map(UriWrapper::fromUri)
            .map(UriWrapper::getLastPathElement)
            .map(SortableIdentifier::new)
            .orElseThrow();

        var resourceOwner = UserInstance.fromPublication(existingPublication);
        return PublishingRequest.createQuery(resourceOwner, existingPublication.getIdentifier(), requestIdentifier);
    }

    private URI createExpectedLocationHeader(Publication existingPublication, PublishingRequest publishingRequest) {
        return UriWrapper.fromHost(API_HOST)
            .addChild(PUBLICATION_PATH)
            .addChild(existingPublication.getIdentifier().toString())
            .addChild(SUPPORT_MESSAGE_PATH)
            .addChild(publishingRequest.getIdentifier().toString())
            .getUri();
    }

    private InputStream ownerRequestsToPublishOwnPublication(Publication existingPublication)
        throws JsonProcessingException {
        return requestToPublishPublication(existingPublication, existingPublication.getResourceOwner().getOwner());
    }

    private InputStream requestToPublishPublication(Publication existingPublication, String requester)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<PublicationPublishRequest>(JsonUtils.dtoObjectMapper)
            .withBody(new PublicationPublishRequest())
            .withNvaUsername(requester)
            .withCustomerId(existingPublication.getPublisher().getId())
            .withPathParameters(
                Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER, existingPublication.getIdentifier().toString()))
            .build();
    }
}

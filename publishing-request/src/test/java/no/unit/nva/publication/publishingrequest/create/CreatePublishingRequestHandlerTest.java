package no.unit.nva.publication.publishingrequest.create;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistPublicationAndMarkForDeletion;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createPersistAndPublishPublication;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
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
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
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
    void shouldCreateNewCaseWhenUserCreatesNewCase()
        throws ApiGatewayException, IOException {
        var existingPublication = PublishingRequestTestUtils.createAndPersistDraftPublication(resourceService);
        var httpRequest = createHttpRequest(existingPublication);
        handler.handleRequest(httpRequest, outputStream, context);
        var httpResponse = GatewayResponse.fromOutputStream(outputStream, PublishingRequestCaseDto.class);
        var actualBody = httpResponse.getBodyObject(PublishingRequestCaseDto.class);
        var actualId = actualBody.getId();
        var persistedRequest = fetchCreatedRequestDirectlyFromService(actualId);
        var expectedBody = PublishingRequestCaseDto.createResponseObject(persistedRequest);
        assertThat(httpResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(actualBody, is(equalTo(expectedBody)));
    }
    
    @Test
    void shouldNotRevealThatPublicationExistsWhenRequesterIsThePublicationOwner()
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
    
    private PublishingRequestCase fetchCreatedRequestDirectlyFromService(URI publishingRequestId)
        throws NotFoundException {
        var caseIdentifier = extractPublishingIdentifierFromPublishingRequestId(publishingRequestId);
        var publicationIdentifier = extractPublicationIdentifierFromPublishingRequestId(publishingRequestId);
        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        var userInfo = UserInstance.fromPublication(publication);
        
        var queryObject = PublishingRequestCase.createQuery(userInfo, publicationIdentifier, caseIdentifier);
        return requestService.getPublishingRequest(queryObject);
    }
    
    private SortableIdentifier extractPublishingIdentifierFromPublishingRequestId(URI publishingRequestId) {
        return attempt(() -> UriWrapper.fromUri(publishingRequestId).getLastPathElement())
            .map(SortableIdentifier::new)
            .orElseThrow();
    }
    
    private SortableIdentifier extractPublicationIdentifierFromPublishingRequestId(URI publishingRequestId) {
        return UriWrapper.fromUri(publishingRequestId).getParent()
            .flatMap(UriWrapper::getParent)
            .map(UriWrapper::getLastPathElement)
            .map(SortableIdentifier::new)
            .orElseThrow();
    }
    
    private InputStream createHttpRequest(Publication existingPublication) throws JsonProcessingException {
        var requestBody = new PublishingRequestOpenCase();
        return new HandlerRequestBuilder<PublishingRequestOpenCase>(JsonUtils.dtoObjectMapper)
            .withBody(requestBody)
            .withNvaUsername(existingPublication.getResourceOwner().getOwner())
            .withCustomerId(existingPublication.getPublisher().getId())
            .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER,
                existingPublication.getIdentifier().toString()))
            .build();
    }
    
    private <T> GatewayResponse<T> ownerCreatesPublishingRequestForDraftPublication(Publication draftPublication,
                                                                                    Class<T> responseType)
        throws IOException {
        var apiRequest = ownerRequestsToPublishOwnPublication(draftPublication);
        outputStream = new ByteArrayOutputStream();
        handler.handleRequest(apiRequest, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }
    
    private InputStream ownerRequestsToPublishOwnPublication(Publication existingPublication)
        throws JsonProcessingException {
        return requestToPublishPublication(existingPublication, existingPublication.getResourceOwner().getOwner());
    }
    
    private InputStream requestToPublishPublication(Publication existingPublication, String requester)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<PublishingRequestOpenCase>(
            JsonUtils.dtoObjectMapper)
            .withBody(new PublishingRequestOpenCase())
            .withNvaUsername(requester)
            .withCustomerId(existingPublication.getPublisher().getId())
            .withPathParameters(
                Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER,
                    existingPublication.getIdentifier().toString()))
            .build();
    }
}

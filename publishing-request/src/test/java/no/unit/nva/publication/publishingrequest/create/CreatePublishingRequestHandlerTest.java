package no.unit.nva.publication.publishingrequest.create;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.model.business.PublishingRequestCase.ALREADY_PUBLISHED_ERROR;
import static no.unit.nva.publication.model.business.PublishingRequestCase.MARKED_FOR_DELETION_ERROR;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.setupMockClock;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.PublishingRequestCaseDto;
import no.unit.nva.publication.publishingrequest.TicketTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class CreatePublishingRequestHandlerTest extends TicketTest {
    
    private CreatePublishingRequestHandler handler;
    
    @BeforeEach
    public void initialize() {
        super.init();
        var mockClock = setupMockClock();
        this.resourceService = new ResourceService(client, mockClock);
        this.ticketService = new TicketService(client, mockClock);
        handler = new CreatePublishingRequestHandler(ticketService);
    }
    
    @Test
    void shouldCreateNewCaseWhenUserCreatesNewCase()
        throws ApiGatewayException, IOException {
        var existingPublication = createAndPersistDraftPublication();
        var httpRequest = createHttpRequest(existingPublication);
        handler.handleRequest(httpRequest, output, CONTEXT);
        var httpResponse = GatewayResponse.fromOutputStream(output, PublishingRequestCaseDto.class);
        var actualBody = httpResponse.getBodyObject(PublishingRequestCaseDto.class);
        var actualId = actualBody.getId();
        var persistedRequest = fetchCreatedRequestDirectlyFromService(actualId);
        var expectedBody = PublishingRequestCaseDto.createResponseObject(persistedRequest);
        assertThat(httpResponse.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(actualBody, is(equalTo(expectedBody)));
    }
    
    @Test
    void shouldNotRevealThatPublicationExistsWhenRequesterIsNotThePublicationOwner()
        throws IOException, ApiGatewayException {
        var existingPublication = createAndPersistDraftPublication();
        var notOwner = randomString();
        var apiRequest = requestToPublishPublication(existingPublication, notOwner);
        handler.handleRequest(apiRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }
    
    @Test
    void shouldReturnConflictWhenOwnerRequestsDuplicatePublishingRequest() throws ApiGatewayException, IOException {
        var draftPublication = createAndPersistDraftPublication();
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
        var publishedPublication = createPersistAndPublishPublication();
        var response = ownerCreatesPublishingRequestForDraftPublication(publishedPublication, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
        assertThat(problem.getDetail(), containsString(ALREADY_PUBLISHED_ERROR));
    }
    
    @Test
    void shouldReturnConflictWhenOwnerRequestsToPublishPublicationMarkedForDeletion()
        throws ApiGatewayException, IOException {
        var publishedPublication = createAndPersistPublicationAndMarkForDeletion();
        var response = ownerCreatesPublishingRequestForDraftPublication(publishedPublication, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
        assertThat(problem.getDetail(), containsString(MARKED_FOR_DELETION_ERROR));
    }
    
    @Test
    void shouldReturnForbiddenWhenTryingToPublishNonExistingPublication() throws IOException {
        var nonPersistedPublication = PublicationGenerator.randomPublication();
        var httpResponse =
            ownerCreatesPublishingRequestForDraftPublication(nonPersistedPublication, Void.class);
        assertThat(httpResponse.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    private PublishingRequestCase fetchCreatedRequestDirectlyFromService(URI publishingRequestId)
        throws NotFoundException {
        var caseIdentifier = extractPublishingIdentifierFromPublishingRequestId(publishingRequestId);
        var publicationIdentifier = extractPublicationIdentifierFromPublishingRequestId(publishingRequestId);
        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        var userInfo = UserInstance.fromPublication(publication);
    
        var queryObject = PublishingRequestCase.createQueryObject(userInfo, publicationIdentifier, caseIdentifier);
        return (PublishingRequestCase) ticketService.fetchTicket(queryObject);
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
        output = new ByteArrayOutputStream();
        handler.handleRequest(apiRequest, output, CONTEXT);
        return GatewayResponse.fromOutputStream(output, responseType);
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

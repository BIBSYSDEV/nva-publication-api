package no.unit.nva.doirequest.update;

import static no.unit.nva.doirequest.DoiRequestRelatedAccessRights.APPROVE_DOI_REQUEST;
import static no.unit.nva.doirequest.DoiRequestsTestConfig.doiRequestsObjectMapper;
import static no.unit.nva.doirequest.update.ApiUpdateDoiRequest.NO_CHANGE_REQUESTED_ERROR;
import static no.unit.nva.doirequest.update.UpdateDoiRequestStatusHandler.INVALID_PUBLICATION_ID_ERROR;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.model.business.DoiRequest.DOI_REQUEST_APPROVAL_FAILURE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class UpdateDoiRequestStatusHandlerTest extends ResourcesLocalTest {
    
    public static final String SOME_CURATOR = randomString();
    public static final String INVALID_IDENTIFIER = "invalidIdentifier";
    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    private TicketService ticketService;
    private UpdateDoiRequestStatusHandler handler;
    private ResourceService resourceService;
    private ByteArrayOutputStream outputStream;
    private Context context;
    
    @BeforeEach
    public void initialize() {
        super.init();
        Clock clock = mock(Clock.class);
        when(clock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME)
            .thenReturn(DOI_REQUEST_CREATION_TIME)
            .thenReturn(DOI_REQUEST_UPDATE_TIME);
        
        ticketService = new TicketService(client);
        handler = new UpdateDoiRequestStatusHandler(ticketService);
        resourceService = new ResourceService(client, clock);
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
    }
    
    @Test
    void handleRequestUpdatesDoiStatusOfDoiRequestInDatabase()
        throws ApiGatewayException, IOException {
        var publication = createPublishedPublicationAndDoiRequest();
        
        var request = createAuthorizedRestRequest(publication);
        handler.handleRequest(request, outputStream, context);
        
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        
        var updatedDoiRequest = fetchDoiRequestDirectlyFromService(publication);
        assertThat(updatedDoiRequest.getStatus(), is(equalTo(TicketStatus.COMPLETED)));
    }
    
    @Test
    void handleReturnsForbiddenWhenUserDoesNotHaveApproveOrRejectAccessRights()
        throws ApiGatewayException, IOException {
        var publication = createPublishedPublicationAndDoiRequest();
        
        var request = createUnauthorizedRestRequest(publication);
        handler.handleRequest(request, outputStream, context);
        
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }
    
    @Test
    void handlerReturnsBadRequestWhenPublicationIsDraft()
        throws ApiGatewayException, IOException {
        var publication = createDraftPublicationAndDoiRequest();
        var request = createAuthorizedRestRequest(publication);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(DOI_REQUEST_APPROVAL_FAILURE));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
    
    @Test
    void handlerReturnsBadRequestForInvalidPublicationIdentifier()
        throws ApiGatewayException, IOException {
        var publication = createDraftPublicationAndDoiRequest();
        publication.setIdentifier(SortableIdentifier.next());
        var request = createAuthorizedRestRequestWithInvalidIdentifier(publication);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(INVALID_PUBLICATION_ID_ERROR));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
    
    @Test
    void handlerReturnsBadRequestWhenNoChangeInDoiRequestStatusIsRequested()
        throws ApiGatewayException, IOException {
        var publication = createDraftPublicationAndDoiRequest();
        publication.setIdentifier(SortableIdentifier.next());
        var request = createAuthorizedRestRequestWithNoRequestedChange(publication);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(NO_CHANGE_REQUESTED_ERROR));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
    
    private InputStream createAuthorizedRestRequestWithNoRequestedChange(Publication publication)
        throws JsonProcessingException {
        return createAuthorizedRestRequest(publication, publication.getIdentifier().toString(), null);
    }
    
    private InputStream createAuthorizedRestRequestWithInvalidIdentifier(Publication publication)
        throws JsonProcessingException {
        return createAuthorizedRestRequest(publication, INVALID_IDENTIFIER, TicketStatus.COMPLETED);
    }
    
    private DoiRequest fetchDoiRequestDirectlyFromService(Publication publication) {
        return ticketService.fetchTicketByResourceIdentifier(
                publication.getPublisher().getId(),
                publication.getIdentifier(),
                DoiRequest.class)
            .orElseThrow();
    }
    
    private InputStream createAuthorizedRestRequest(Publication publication) throws JsonProcessingException {
        return createAuthorizedRestRequest(publication,
            publication.getIdentifier().toString(),
            TicketStatus.COMPLETED);
    }
    
    private InputStream createAuthorizedRestRequest(Publication publication,
                                                    String identifier,
                                                    TicketStatus ticketStatus)
        throws JsonProcessingException {
        var body = createUpdateRequest(ticketStatus);
        var pathParameters = createPathParameters(identifier);
        var customerId = publication.getPublisher().getId();
        return new HandlerRequestBuilder<ApiUpdateDoiRequest>(doiRequestsObjectMapper)
            .withCustomerId(customerId)
            .withNvaUsername(SOME_CURATOR)
            .withAccessRights(customerId, APPROVE_DOI_REQUEST.toString())
            .withPathParameters(pathParameters)
            .withBody(body)
            .build();
    }
    
    private InputStream createUnauthorizedRestRequest(Publication publication) throws JsonProcessingException {
        var body = createUpdateRequest(TicketStatus.COMPLETED);
        var pathParameters = createPathParameters(publication.getIdentifier().toString());
        var customerId = publication.getPublisher().getId();
        return new HandlerRequestBuilder<ApiUpdateDoiRequest>(doiRequestsObjectMapper)
            .withCustomerId(customerId)
            .withNvaUsername(SOME_CURATOR)
            .withPathParameters(pathParameters)
            .withBody(body)
            .build();
    }
    
    private Map<String, String> createPathParameters(String identifier) {
        return Map.of(
            PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, identifier
        );
    }
    
    private ApiUpdateDoiRequest createUpdateRequest(TicketStatus ticketStatus) {
        var body = new ApiUpdateDoiRequest();
        body.setDoiRequestStatus(ticketStatus);
        return body;
    }
    
    private Publication createPublishedPublicationAndDoiRequest() throws ApiGatewayException {
        var publication = createDraftPublicationAndDoiRequest();
        resourceService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        return publication;
    }
    
    private Publication createDraftPublicationAndDoiRequest() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication().copy().withDoi(null).build();
        var userInstance = UserInstance.fromPublication(publication);
        publication = resourceService.createPublication(userInstance, publication);
        var doiRequest = DoiRequest.fromPublication(publication);
        ticketService.createTicket(doiRequest, DoiRequest.class);
        return publication;
    }
}
package no.unit.nva.doi.requests.handlers;

import static no.unit.nva.doi.requests.handlers.ApiUpdateDoiRequest.NO_CHANGE_REQUESTED_ERROR;
import static no.unit.nva.doi.requests.handlers.UpdateDoiRequestStatusHandler.API_PUBLICATION_PATH_IDENTIFIER;
import static no.unit.nva.doi.requests.handlers.UpdateDoiRequestStatusHandler.INVALID_PUBLICATION_ID_ERROR;
import static no.unit.nva.publication.service.impl.DoiRequestService.UPDATE_DOI_REQUEST_STATUS_CONDITION_FAILURE_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.anyString;
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
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.useraccessserivce.accessrights.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class UpdateDoiRequestStatusHandlerTest extends ResourcesDynamoDbLocalTest {
    
    public static final String SOME_CURATOR = "some@curator.org";
    public static final String INVALID_IDENTIFIER = "invalidIdentifier";
    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    private DoiRequestService doiRequestService;
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
        doiRequestService = new DoiRequestService(client, clock);
        
        handler = new UpdateDoiRequestStatusHandler(setupEnvironment(), doiRequestService);
        resourceService = new ResourceService(client, clock);
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
    }
    
    @Test
    public void handleRequestUpdatesDoiStatusOfDoiRequestInDatabase()
        throws ApiGatewayException, IOException {
        Publication publication = createPublishedPublicationAndDoiRequest();
        
        var request = createAuthorizedRestRequest(publication);
        handler.handleRequest(request, outputStream, context);
        
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        
        DoiRequest updatedDoiRequest = fetchDoiRequestDirectlyFromService(publication);
        assertThat(updatedDoiRequest.getStatus(), is(equalTo(DoiRequestStatus.APPROVED)));
    }
    
    @Test
    public void handleReturnsForbiddenWhenUserDoesNotHaveApproveOrRejectAccessRights()
        throws ApiGatewayException, IOException {
        Publication publication = createPublishedPublicationAndDoiRequest();
        
        var request = createUnauthorizedRestRequest(publication);
        handler.handleRequest(request, outputStream, context);
        
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }
    
    @Test
    public void handlerReturnsBadRequestWhenPublicationIsDraft()
        throws ApiGatewayException, IOException {
        Publication publication = createDraftPublicationAndDoiRequest();
        var request = createAuthorizedRestRequest(publication);
        handler.handleRequest(request, outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(UPDATE_DOI_REQUEST_STATUS_CONDITION_FAILURE_MESSAGE));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
    
    @Test
    public void handlerReturnsBadRequestForInvalidPublicationIdentifier()
        throws ApiGatewayException, IOException {
        Publication publication = createDraftPublicationAndDoiRequest();
        publication.setIdentifier(SortableIdentifier.next());
        var request = createAuthorizedRestRequestWithInvalidIdentifier(publication);
        handler.handleRequest(request, outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(INVALID_PUBLICATION_ID_ERROR));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
    
    @Test
    public void handlerReturnsBadRequestWhenNoChangeInDoiRequestStatusIsRequested()
        throws ApiGatewayException, IOException {
        Publication publication = createDraftPublicationAndDoiRequest();
        publication.setIdentifier(SortableIdentifier.next());
        var request = createAuthorizedRestRequestWithNoRequestedChange(publication);
        handler.handleRequest(request, outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
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
        return createAuthorizedRestRequest(publication, INVALID_IDENTIFIER, DoiRequestStatus.APPROVED);
    }
    
    private Environment setupEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn("*");
        return environment;
    }
    
    private DoiRequest fetchDoiRequestDirectlyFromService(Publication publication) throws NotFoundException {
        return doiRequestService
                   .getDoiRequestByResourceIdentifier(createUserInstance(publication), publication.getIdentifier());
    }
    
    private InputStream createAuthorizedRestRequest(Publication publication) throws JsonProcessingException {
        return createAuthorizedRestRequest(publication,
            publication.getIdentifier().toString(),
            DoiRequestStatus.APPROVED);
    }
    
    private InputStream createAuthorizedRestRequest(Publication publication,
                                                    String identifier,
                                                    DoiRequestStatus doiRequestStatus)
        throws JsonProcessingException {
        ApiUpdateDoiRequest body = createUpdateRequest(doiRequestStatus);
        Map<String, String> pathParameters = createPathParameters(identifier);
        return new HandlerRequestBuilder<ApiUpdateDoiRequest>(JsonUtils.objectMapper)
                   .withCustomerId(publication.getPublisher().getId().toString())
                   .withFeideId(SOME_CURATOR)
                   .withAccessRight(AccessRight.APPROVE_DOI_REQUEST.toString())
                   .withAccessRight(AccessRight.REJECT_DOI_REQUEST.toString())
                   .withPathParameters(pathParameters)
                   .withBody(body)
                   .build();
    }
    
    private InputStream createUnauthorizedRestRequest(Publication publication) throws JsonProcessingException {
        ApiUpdateDoiRequest body = createUpdateRequest(DoiRequestStatus.APPROVED);
        Map<String, String> pathParameters = createPathParameters(publication.getIdentifier().toString());
        return new HandlerRequestBuilder<ApiUpdateDoiRequest>(JsonUtils.objectMapper)
                   .withCustomerId(publication.getPublisher().getId().toString())
                   .withFeideId(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withBody(body)
                   .build();
    }
    
    private Map<String, String> createPathParameters(String identifier) {
        return Map.of(
            API_PUBLICATION_PATH_IDENTIFIER, identifier
        );
    }
    
    private ApiUpdateDoiRequest createUpdateRequest(DoiRequestStatus doiRequestStatus) {
        ApiUpdateDoiRequest body = new ApiUpdateDoiRequest();
        body.setDoiRequestStatus(doiRequestStatus);
        return body;
    }
    
    private Publication createPublishedPublicationAndDoiRequest() throws ApiGatewayException {
        Publication publication = createDraftPublicationAndDoiRequest();
        resourceService.publishPublication(createUserInstance(publication), publication.getIdentifier());
        return publication;
    }
    
    private Publication createDraftPublicationAndDoiRequest() throws ApiGatewayException {
        Publication publication = resourceService.createPublication(
            PublicationGenerator.publicationWithoutIdentifier());
        UserInstance userInstance = createUserInstance(publication);
        doiRequestService.createDoiRequest(userInstance, publication.getIdentifier());
        return publication;
    }
    
    private UserInstance createUserInstance(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }
}
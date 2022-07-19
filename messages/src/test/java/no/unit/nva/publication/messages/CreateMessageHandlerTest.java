package no.unit.nva.publication.messages;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static no.unit.nva.publication.PublicationServiceConfig.PATH_SEPARATOR;
import static no.unit.nva.publication.PublicationServiceConfig.URI_EMPTY_FRAGMENT;
import static no.unit.nva.publication.messages.MessageTestsConfig.messageTestsObjectMapper;
import static no.unit.nva.publication.service.impl.ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.doirequest.list.ListDoiRequestsHandler;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.zalando.problem.Problem;

class CreateMessageHandlerTest extends ResourcesLocalTest {
    
    public static final String SOME_CURATOR = "some@curator";
    public static final Context CONTEXT = mock(Context.class);
    public static final String ALLOW_ALL_ORIGIN = "*";
    public static final String SOME_VALID_HOST = "localhost";
    public static final String HTTPS = "https";
    private ResourceService resourcesService;
    private MessageService messageService;
    private CreateMessageHandler handler;
    private ByteArrayOutputStream output;
    private InputStream input;
    private Publication samplePublication;
    private Environment environment;
    private DoiRequestService doiRequestService;
    
    @BeforeEach
    public void initialize() throws ApiGatewayException {
        super.init();
        
        resourcesService = new ResourceService(client, Clock.systemDefaultZone());
        messageService = new MessageService(client, Clock.systemDefaultZone());
        doiRequestService = new DoiRequestService(client, Clock.systemDefaultZone());
        environment = setupEnvironment();
        handler = new CreateMessageHandler(client, environment);
        output = new ByteArrayOutputStream();
        samplePublication = createSamplePublication();
    }
    
    public String extractTextFromOldestMessage(Publication doiRequest) {
        return doiRequest.getDoiRequest().getMessages().get(0).getText();
    }
    
    @Test
    void handlerStoresMessageWhenCreateRequestIsReceivedByAuthenticatedUser()
        throws IOException, NotFoundException {
        CreateMessageRequest requestBody = createSampleMessage(samplePublication, randomString());
        
        input = createInput(requestBody);
        handler.handleRequest(input, output, CONTEXT);
        URI messageId = extractLocationFromHttpHeaders();
        Message message = fetchMessageDirectlyFromDb(samplePublication, messageId);
        assertThat(message.getText(), is(equalTo(requestBody.getMessage())));
    }
    
    @Test
    void handlerReturnsLocationHeaderWithUriForGettingTheMessage()
        throws IOException, URISyntaxException, NotFoundException {
        CreateMessageRequest requestBody = createSampleMessage(samplePublication, randomString());
        
        input = createInput(requestBody);
        handler.handleRequest(input, output, CONTEXT);
        URI messageId = extractLocationFromHttpHeaders();
        
        Message message = fetchMessageDirectlyFromDb(samplePublication, messageId);
        URI expectedMessageId = constructExpectedMessageUri(message);
        assertThat(messageId, is(equalTo(expectedMessageId)));
        assertThat(message.getText(), is(equalTo(requestBody.getMessage())));
    }
    
    @ParameterizedTest(name = "handler returns bad request when CreateRequest contains message: \"{0}\"")
    @NullAndEmptySource
    void handlerReturnsBadRequestWhenCreateRequestContainsNoText(String emptyMessage)
        throws IOException {
        var requestBody = createSampleMessage(samplePublication, emptyMessage);
        input = createInput(requestBody);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        Problem problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(MessageService.EMPTY_MESSAGE_ERROR));
    }
    
    @Test
    void handlerReturnsBadRequestWhenCreateRequestContainsNonExistentPublicationIdentifier()
        throws IOException {
        SortableIdentifier invalidIdentifier = SortableIdentifier.next();
        var requestBody = createSampleMessage(invalidIdentifier, randomString());
        input = createInput(requestBody);
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(invalidIdentifier.toString()));
        assertThat(problem.getDetail(), containsString(PUBLICATION_NOT_FOUND_CLIENT_MESSAGE));
    }
    
    @Test
    void handlerCreatesDoiRequestMessageWhenClientMarksMessageAsDoiRequestRelated()
        throws IOException, BadRequestException {
        createDoiRequestForSamplePublication();
        CreateMessageRequest requestBody = createDoiRequestMessage();
        postDoiRequestMessage(requestBody);
        
        Publication[] doiRequests = listDoiRequestsAsPublicationOwner();
        String actualText = extractTextFromOldestMessage(doiRequests[0]);
        
        assertThat(actualText, is(equalTo(requestBody.getMessage())));
    }
    
    @Test
    void shouldReturnBadRequestWhenClientDoesNotProvideMessageType() throws IOException {
        var request = createSampleMessage(samplePublication, randomString());
        request.setMessageType(null);
        
        input = createInput(request);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    private URI constructExpectedMessageUri(Message message) throws URISyntaxException {
        String expectedPath = PublicationServiceConfig.MESSAGE_PATH + PATH_SEPARATOR + message.getIdentifier();
        return new URI(HTTPS, SOME_VALID_HOST, expectedPath, URI_EMPTY_FRAGMENT);
    }
    
    private Environment setupEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALLOW_ALL_ORIGIN);
        return environment;
    }
    
    private Publication[] listDoiRequestsAsPublicationOwner() throws IOException {
        ListDoiRequestsHandler listDoiRequestsHandler = new ListDoiRequestsHandler(
            environment, doiRequestService, messageService);
        InputStream listDoiRequestsRequest = createListDoiRequestsHttpQuery();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        listDoiRequestsHandler.handleRequest(listDoiRequestsRequest, output, CONTEXT);
        var listDoiRequestsResponse = GatewayResponse.fromOutputStream(output, Publication[].class);
        return listDoiRequestsResponse.getBodyObject(Publication[].class);
    }
    
    private InputStream createListDoiRequestsHttpQuery() throws JsonProcessingException {
        UserInstance publicationOwner = extractOwner(samplePublication);
        return new HandlerRequestBuilder<Void>(messageTestsObjectMapper)
            .withNvaUsername(publicationOwner.getUserIdentifier())
            .withCustomerId(publicationOwner.getOrganizationUri())
            .withQueryParameters(Map.of("role", "Creator"))
            .withRoles("Creator")
            .build();
    }
    
    private void postDoiRequestMessage(CreateMessageRequest requestBody) throws IOException {
        input = createInput(requestBody);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_CREATED));
    }
    
    private CreateMessageRequest createDoiRequestMessage() {
        CreateMessageRequest requestBody = createSampleMessage(samplePublication, randomString());
        requestBody.setMessageType(MessageType.DOI_REQUEST);
        return requestBody;
    }
    
    private void createDoiRequestForSamplePublication() throws BadRequestException {
        doiRequestService.createDoiRequest(extractOwner(samplePublication), samplePublication.getIdentifier());
    }
    
    private URI extractLocationFromHttpHeaders() throws JsonProcessingException {
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        String headerValue = response.getHeaders().get(HttpHeaders.LOCATION);
        return URI.create(headerValue);
    }
    
    private Message fetchMessageDirectlyFromDb(Publication samplePublication, URI messageId) throws NotFoundException {
        UserInstance owner = extractOwner(samplePublication);
        var identifier = SortableIdentifier.fromUri(messageId);
        return messageService.getMessage(owner, identifier);
    }
    
    private UserInstance extractOwner(Publication samplePublication) {
        return UserInstance.create(samplePublication.getResourceOwner().getOwner(),
            samplePublication.getPublisher().getId());
    }
    
    private InputStream createInput(CreateMessageRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(messageTestsObjectMapper)
            .withBody(requestBody)
            .withNvaUsername(SOME_CURATOR)
            .withCustomerId(samplePublication.getPublisher().getId())
            .build();
    }
    
    private CreateMessageRequest createSampleMessage(Publication savedPublication, String message) {
        return createSampleMessage(savedPublication.getIdentifier(), message);
    }
    
    private CreateMessageRequest createSampleMessage(SortableIdentifier identifier, String message) {
        CreateMessageRequest requestBody = new CreateMessageRequest();
        requestBody.setMessage(message);
        requestBody.setPublicationIdentifier(identifier);
        requestBody.setMessageType(MessageType.SUPPORT);
        return requestBody;
    }
    
    private Publication createSamplePublication() throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        UserInstance userInstance = UserInstance.fromPublication(publication);
        return resourcesService.createPublication(userInstance, publication);
    }
}
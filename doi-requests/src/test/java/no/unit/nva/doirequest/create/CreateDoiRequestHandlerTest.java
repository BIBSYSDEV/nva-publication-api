package no.unit.nva.doirequest.create;

import static no.unit.nva.doirequest.DoiRequestsTestConfig.doiRequestsObjectMapper;
import static no.unit.nva.doirequest.create.CreateDoiRequestHandler.PUBLICATION_HAS_DOI_ALREADY;
import static no.unit.nva.doirequest.create.CreateDoiRequestHandler.RECENT_DOI_REQUEST_ERROR;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class CreateDoiRequestHandlerTest extends ResourcesLocalTest {
    
    public static final String HTTP_PATH_SEPARATOR = "/";
    public static final ResourceOwner NOT_THE_RESOURCE_OWNER = new ResourceOwner(randomString(), randomUri());
    public static final URI SOME_PUBLISHER = URI.create("https://some-publicsher.com");
    public static final String ALLOW_ALL_ORIGINS = "*";
    
    private CreateDoiRequestHandler handler;
    private ResourceService resourceService;
    private Clock clock;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private TicketService ticketService;
    private MessageService messageService;
    
    @BeforeEach
    public void initialize() {
        init();
    
        clock = Clock.systemDefaultZone();
        resourceService = new ResourceService(client, clock);
        ticketService = new TicketService(client);
        messageService = new MessageService(client, clock);
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        Environment environment = mockEnvironment();
    
        handler = new CreateDoiRequestHandler(resourceService, ticketService, messageService, environment);
    }
    
    public void sendRequest(Publication publication, ResourceOwner owner, String message) throws IOException {
        InputStream inputStream = createRequest(publication, owner, message);
        handler.handleRequest(inputStream, outputStream, context);
    }
    
    @Test
    void createDoiRequestStoresNewDoiRequestForPublishedResource()
        throws ApiGatewayException, IOException {
        Publication publication = createPublicationWithoutDoi();
        sendRequest(publication, publication.getResourceOwner());
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        String doiRequestIdentifier = extractLocationHeader(response);
        DoiRequest doiRequest = readDoiRequestDirectlyFromService(new SortableIdentifier(doiRequestIdentifier));
        assertThat(doiRequest, is(not(nullValue())));
    }
    
    @Test
    void createDoiRequestReturnsErrorWhenUserTriesToCreateDoiRequestOnNotOwnedPublication()
        throws IOException {
        Publication publication = createPublicationWithoutDoi();
        
        sendRequest(publication, NOT_THE_RESOURCE_OWNER);
        
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        Problem problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat(problem.getDetail(), is(equalTo(CreateDoiRequestHandler.USER_IS_NOT_OWNER_ERROR)));
    }
    
    @Test
    void createDoiRequestReturnsBadRequestWhenPublicationIdIsEmpty() throws IOException {
        CreateDoiRequest request = new CreateDoiRequest(null, null);
        InputStream inputStream = new HandlerRequestBuilder<CreateDoiRequest>(doiRequestsObjectMapper)
                                      .withBody(request)
                                      .withNvaUsername(NOT_THE_RESOURCE_OWNER.getOwner())
                                      .withCustomerId(SOME_PUBLISHER)
                                      .build();
        
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }
    
    @Test
    void createDoiRequestReturnsBadRequestErrorWhenPublicationHasADoiAlready()
        throws IOException {
        Publication publication = createPublicationWithDoi();
        sendRequest(publication, publication.getResourceOwner());
        
        outputStream = new ByteArrayOutputStream();
        sendRequest(publication, publication.getResourceOwner());
        
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(PUBLICATION_HAS_DOI_ALREADY));
    }
    
    @Test
    void shouldUpdateDoiRequestIfDoiRequestExistsButPublicationHasNoDoiAndThereIsLowChanceOfCreatingHangingDraftDois()
        throws ApiGatewayException, IOException {
        var publication = createPublicationWithoutDoi();
        var newTicketRequest = TicketEntry.requestNewTicket(publication, DoiRequest.class);
        var doiRequest = ticketService.createTicket(newTicketRequest, DoiRequest.class);
        setModifiedDateToBeAtLeastOneDayOld(doiRequest);
        var request = createRequest(publication, publication.getResourceOwner(), randomString());
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    }
    
    @Test
    void shouldThrowBadRequestWhenDoiRequestExistsPublicationHasNoDoiAndThereIsHighChanceOfCreatingHangingDraftDois()
        throws ApiGatewayException, IOException {
        var publication = createPublicationWithoutDoi();
        var newTicketRequest = TicketEntry.requestNewTicket(publication, DoiRequest.class);
        ticketService.createTicket(newTicketRequest, DoiRequest.class);
        var request = createRequest(publication, publication.getResourceOwner(), randomString());
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        var problem = response.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(RECENT_DOI_REQUEST_ERROR));
    }
    
    private void setModifiedDateToBeAtLeastOneDayOld(DoiRequest doiRequest) {
        doiRequest.setModifiedDate(doiRequest.getModifiedDate().minus(Duration.ofDays(1)));
        PutItemRequest putItemRequest = doiRequest.toDao().createPutItemRequest();
        
        client.putItem(putItemRequest);
    }
    
    private void sendRequest(Publication publication, ResourceOwner owner) throws IOException {
        sendRequest(publication, owner, null);
    }
    
    private Environment mockEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALLOW_ALL_ORIGINS);
        return environment;
    }
    
    private InputStream createRequest(Publication publication, ResourceOwner owner, String message)
        throws JsonProcessingException {
        CreateDoiRequest request = new CreateDoiRequest(publication.getIdentifier(), message);
        return new HandlerRequestBuilder<CreateDoiRequest>(doiRequestsObjectMapper)
                   .withCustomerId(publication.getPublisher().getId())
                   .withNvaUsername(owner.getOwner())
                   .withPathParameters(
                       Map.of(RequestUtil.PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withBody(request)
                   .build();
    }
    
    private DoiRequest readDoiRequestDirectlyFromService(SortableIdentifier doiRequestIdentifier)
        throws NotFoundException {
        return (DoiRequest) ticketService.fetchTicketByIdentifier(doiRequestIdentifier);
    }
    
    private String extractLocationHeader(GatewayResponse<Void> response) {
        String locationHeader = response.getHeaders().get(HttpHeaders.LOCATION);
        String[] headerArray = locationHeader.split(HTTP_PATH_SEPARATOR);
        return headerArray[headerArray.length - 1];
    }
    
    private Publication createPublicationWithoutDoi() {
        Publication publication = PublicationGenerator.randomPublication().copy().withDoi(null).build();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private Publication createPublicationWithDoi() {
        Publication publication = PublicationGenerator.randomPublication().copy().withDoi(randomDoi()).build();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}

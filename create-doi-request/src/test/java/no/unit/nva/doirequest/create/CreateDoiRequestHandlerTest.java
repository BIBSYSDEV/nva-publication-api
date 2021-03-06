package no.unit.nva.doirequest.create;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceConversation;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class CreateDoiRequestHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String HTTP_PATH_SEPARATOR = "/";
    public static final String NOT_THE_RESOURCE_OWNER = "someOther@owner.org";
    public static final URI SOME_PUBLISHER = URI.create("https://some-publicsher.com");

    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    public static final int SINGLE_MESSAGE = 0;
    public static final String ALLOW_ALL_ORIGINS = "*";
    private CreateDoiRequestHandler handler;
    private ResourceService resourceService;
    private Clock mockClock;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private DoiRequestService doiRequestService;
    private MessageService messageService;

    @BeforeEach
    public void initialize() {
        init();
        setupClock();
        resourceService = new ResourceService(client, mockClock);
        doiRequestService = new DoiRequestService(client, mockClock);
        messageService = new MessageService(client, mockClock);
        outputStream = new ByteArrayOutputStream();
        context = mock(Context.class);
        Environment environment = mockEnvironment();

        handler = new CreateDoiRequestHandler(resourceService, doiRequestService, messageService, environment);
    }

    @Test
    public void createDoiRequestStoresNewDoiRequestForPublishedResource()
        throws TransactionFailedException, IOException, NotFoundException {
        Publication publication = createPublication();

        sendRequest(publication, publication.getOwner());

        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(outputStream);
        String doiRequestIdentifier = extractLocationHeader(response);

        DoiRequest doiRequest = readDoiRequestDirectlyFromService(publication, doiRequestIdentifier);

        assertThat(doiRequest, is(not(nullValue())));
    }

    @Test
    public void createDoiRequestReturnsErrorWhenUserTriesToCreateDoiRequestOnNotOwnedPublication()
        throws TransactionFailedException, IOException {
        Publication publication = createPublication();

        sendRequest(publication, NOT_THE_RESOURCE_OWNER);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        Problem problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        assertThat(problem.getDetail(), is(equalTo(CreateDoiRequestHandler.USER_IS_NOT_OWNER_ERROR)));
    }

    @Test
    public void createDoiRequestReturnsBadRequestWhenPublicationIdIsEmpty() throws IOException {
        CreateDoiRequest request = new CreateDoiRequest(null, null);
        InputStream inputStream = new HandlerRequestBuilder<CreateDoiRequest>(objectMapper)
                                      .withBody(request)
                                      .withFeideId(NOT_THE_RESOURCE_OWNER)
                                      .withCustomerId(SOME_PUBLISHER.toString())
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
    }

    @Test
    public void createDoiRequestReturnsBadRequestErrorWenDoiRequestAlreadyExists()
        throws TransactionFailedException, IOException {
        Publication publication = createPublication();

        sendRequest(publication, publication.getOwner());

        outputStream = new ByteArrayOutputStream();

        sendRequest(publication, publication.getOwner());

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    public void createDoiRequestStoresMessageAsDoiRelatedWhenMessageIsIncluded()
        throws TransactionFailedException, IOException {
        Publication publication = createPublication();
        String expectedMessageText = randomString();

        sendRequest(publication, publication.getOwner(), expectedMessageText);

        Optional<ResourceConversation> resourceMessages = messageService.getMessagesForResource(
            extractOwner(publication),
            publication.getIdentifier());

        MessageDto savedMessage = resourceMessages.orElseThrow().getMessages().get(SINGLE_MESSAGE);
        assertThat(savedMessage.getText(), is(equalTo(expectedMessageText)));
        assertThat(savedMessage.isDoiRequestRelated(), is(equalTo(true)));
    }

    public void sendRequest(Publication publication, String owner, String message) throws IOException {
        InputStream inputStream = createRequest(publication, owner, message);
        handler.handleRequest(inputStream, outputStream, context);
    }

    private void sendRequest(Publication publication, String owner) throws IOException {
        sendRequest(publication, owner, null);
    }


    private Environment mockEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALLOW_ALL_ORIGINS);
        return environment;
    }

    private InputStream createRequest(Publication publication, String user, String message)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        CreateDoiRequest request = new CreateDoiRequest(publication.getIdentifier(), message);
        return new HandlerRequestBuilder<CreateDoiRequest>(objectMapper)
                   .withCustomerId(publication.getPublisher().getId().toString())
                   .withFeideId(user)
                   .withPathParameters(Map.of(RequestUtil.IDENTIFIER, publication.getIdentifier().toString()))
                   .withBody(request)
                   .build();
    }

    private DoiRequest readDoiRequestDirectlyFromService(Publication publication, String doiRequestIdentifier)
        throws NotFoundException {
        UserInstance userInstance = new UserInstance(publication.getOwner(), publication.getPublisher().getId());

        return doiRequestService.getDoiRequest(userInstance, new SortableIdentifier(
            doiRequestIdentifier));
    }

    private String extractLocationHeader(GatewayResponse<Void> response) {
        String locationHeader = response.getHeaders().get(HttpHeaders.LOCATION);
        String[] headerArray = locationHeader.split(HTTP_PATH_SEPARATOR);
        return headerArray[headerArray.length - 1];
    }

    private void setupClock() {
        mockClock = mock(Clock.class);
        when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME)
            .thenReturn(DOI_REQUEST_CREATION_TIME)
            .thenReturn(DOI_REQUEST_UPDATE_TIME);
    }

    private Publication createPublication() throws TransactionFailedException {
        return resourceService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }
}

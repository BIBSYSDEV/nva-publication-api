package no.unit.nva.publication.messages.create;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static no.unit.nva.publication.service.impl.ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.javafaker.Faker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.StorageModelConstants;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.zalando.problem.Problem;

public class CreateMessageHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final Faker FAKER = Faker.instance();
    public static final String SOME_CURATOR = "some@curator";
    public static final Context CONTEXT = mock(Context.class);
    public static final String ALLOW_ALL_ORIGIN = "*";
    private ResourceService resourcesService;
    private MessageService messageService;
    private CreateMessageHandler handler;
    private ByteArrayOutputStream output;
    private InputStream input;
    private Publication samplePublication;

    @BeforeEach
    public void initialize() throws TransactionFailedException {
        super.init();
        resourcesService = new ResourceService(client, Clock.systemDefaultZone());
        messageService = new MessageService(client, Clock.systemDefaultZone());
        Environment environment = setupEnvironment();
        StorageModelConstants.updateEnvironment(environment);
        handler = new CreateMessageHandler(client, environment);
        output = new ByteArrayOutputStream();
        samplePublication = createSamplePublication();
    }

    private Environment setupEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALLOW_ALL_ORIGIN);
        when(environment.readEnv(StorageModelConstants.HOST_ENV_VARIABLE_NAME))
            .thenReturn("localhost");

        return environment;
    }

    @Test
    public void handlerStoresMessageWhenCreateRequestIsReceivedByAuthenticatedUser()
        throws IOException {
        CreateMessageRequest requestBody = createSampleMessage(samplePublication, randomString());

        input = createInput(requestBody);
        handler.handleRequest(input, output, CONTEXT);
        URI messageIdentifier = extractLocationFromHttpHeaders();
        Message message = fetchMessageDirectlyFromDb(samplePublication, messageIdentifier);
        assertThat(message.getText(), is(equalTo(requestBody.getMessage())));
    }

    @ParameterizedTest(name = "handler returns bad request when CreateRequest contains message: \"{0}\"")
    @NullAndEmptySource
    public void handlerReturnsBadRequestWhenCreateRequestContainsNoText(String emptyMessage)
        throws IOException {
        var requestBody = createSampleMessage(samplePublication, emptyMessage);
        input = createInput(requestBody);
        handler.handleRequest(input, output, CONTEXT);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(output);
        Problem problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(MessageService.EMPTY_MESSAGE_ERROR));
    }

    @Test
    public void handlerReturnsBadRequestWhenCreateRequestContainsNonExistentPublicationIdentifier()
        throws IOException {
        SortableIdentifier invalidIdentifier = SortableIdentifier.next();
        var requestBody = createSampleMessage(invalidIdentifier, randomString());
        input = createInput(requestBody);
        handler.handleRequest(input, output, CONTEXT);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(output);
        var problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(invalidIdentifier.toString()));
        assertThat(problem.getDetail(), containsString(PUBLICATION_NOT_FOUND_CLIENT_MESSAGE));
    }

    private URI extractLocationFromHttpHeaders() throws JsonProcessingException {
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output);
        String headerValue = response.getHeaders().get(HttpHeaders.LOCATION);
        return URI.create(headerValue);
    }

    private Message fetchMessageDirectlyFromDb(Publication samplePublication, URI messageId) {
        UserInstance owner = extractOwner(samplePublication);
        return messageService.getMessage(owner, messageId);
    }

    private UserInstance extractOwner(Publication samplePublication) {
        return new UserInstance(samplePublication.getOwner(), samplePublication.getPublisher().getId());
    }

    private InputStream createInput(CreateMessageRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(JsonUtils.objectMapper)
                   .withBody(requestBody)
                   .withFeideId(SOME_CURATOR)
                   .withCustomerId(samplePublication.getPublisher().getId().toString())
                   .build();
    }

    private CreateMessageRequest createSampleMessage(Publication savedPublication, String message) {
        return createSampleMessage(savedPublication.getIdentifier(), message);
    }

    private CreateMessageRequest createSampleMessage(SortableIdentifier identifier, String message) {
        CreateMessageRequest requestBody = new CreateMessageRequest();
        requestBody.setMessage(message);
        requestBody.setPublicationIdentifier(identifier);
        return requestBody;
    }

    private Publication createSamplePublication() throws TransactionFailedException {
        return resourcesService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }

    private String randomString() {
        return FAKER.lorem().sentence();
    }
}
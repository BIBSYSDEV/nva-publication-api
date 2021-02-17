package no.unit.nva.publication.messages.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.javafaker.Faker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    public void init() {
        super.init();
        resourcesService = new ResourceService(client, Clock.systemDefaultZone());
        messageService = new MessageService(client, Clock.systemDefaultZone());
        Environment environment = mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn(ALLOW_ALL_ORIGIN);
        handler = new CreateMessageHandler(client, environment);
        output = new ByteArrayOutputStream();
    }

    @Test
    public void handlerStoresMessageWhenCreateRequestIsReceivedByAuthenticatedUser()
        throws IOException, TransactionFailedException {
        Publication samplePublication = createSamplePublication();
        CreateMessageRequest requestBody = createSampleMessage(samplePublication);

        input = createInput(samplePublication, requestBody);
        handler.handleRequest(input, output, CONTEXT);
        String messageIdentifier = extractLocationFromHttpHeaders();
        Message message = fetchMessageDirectlyFromDb(samplePublication, messageIdentifier);
        assertThat(message.getText(), is(equalTo(requestBody.getMessage())));
    }

    private String extractLocationFromHttpHeaders() throws JsonProcessingException {
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output);
        String messageIdentifier = response.getHeaders().get(HttpHeaders.LOCATION);
        return messageIdentifier;
    }

    private Message fetchMessageDirectlyFromDb(Publication samplePublication, String messageIdentifier) {
        UserInstance owner = extractOwner(samplePublication);
        Message message = messageService.getMessage(owner, new SortableIdentifier(messageIdentifier));
        return message;
    }

    private UserInstance extractOwner(Publication samplePublication) {
        return new UserInstance(samplePublication.getOwner(), samplePublication.getPublisher().getId());
    }

    private InputStream createInput(Publication samplePublication, CreateMessageRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(JsonUtils.objectMapper)
                   .withBody(requestBody)
                   .withFeideId(SOME_CURATOR)
                   .withCustomerId(samplePublication.getPublisher().getId().toString())
                   .build();
    }

    private CreateMessageRequest createSampleMessage(Publication savedPublication) {
        CreateMessageRequest requestBody = new CreateMessageRequest();
        requestBody.setMessage(randomString());
        requestBody.setPublicationIdentifier(savedPublication.getIdentifier());
        return requestBody;
    }

    private Publication createSamplePublication() throws TransactionFailedException {
        return resourcesService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }

    private String randomString() {
        return FAKER.lorem().sentence();
    }
}
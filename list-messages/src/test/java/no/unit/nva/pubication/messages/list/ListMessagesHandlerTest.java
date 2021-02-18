package no.unit.nva.pubication.messages.list;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
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
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceMessages;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListMessagesHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String SAMPLE_USER = "some@user";
    public static final URI SOME_ORG = URI.create("https://example.com/123");
    public static final Context CONTEXT = mock(Context.class);
    public static final String SOME_OTHER_USER = "some@otheruser";
    public static final Faker FAKER = Faker.instance();
    private ListMessagesHandler handler;
    private ByteArrayOutputStream output;
    private InputStream input;
    private ResourceService resourceService;
    private MessageService messageService;

    @BeforeEach
    public void init() {
        super.init();
        Environment environment = mockEnvironment();
        handler = new ListMessagesHandler(environment);
        output = new ByteArrayOutputStream();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        messageService = new MessageService(client, Clock.systemDefaultZone());
    }

    @Test
    public void listMessagesReturnsOkWhenUserIsAuthenticated() throws IOException {
        input = sampleListRequest();
        handler.handleRequest(input, output, CONTEXT);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void listMessagesReturnsMessagesPerPublicationsForUser() throws TransactionFailedException {
        Publication publication1 = createPublication();
        Publication publication2 = createPublication();

        UserInstance owner = extractPublicationOwner(publication1);
        UserInstance sender = new UserInstance(SOME_OTHER_USER, publication1.getPublisher().getId());

        Message message1 = createMessage(publication1, sender);
        Message message2 = createMessage(publication2, sender);

        List<ResourceMessages> messages = messageService.listMessagesForUser(owner);
        List<Message> allMessages = messages.stream()
                                        .flatMap(rs -> rs.getMessages().stream())
                                        .collect(Collectors.toList());

        Message[] expectedMessages = new Message[]{message1, message2};
        assertThat(allMessages, containsInAnyOrder(expectedMessages));
    }

    public UserInstance extractPublicationOwner(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }

    public Publication createPublication() throws TransactionFailedException {
        return resourceService.createPublication(PublicationGenerator.publicationWithoutIdentifier());
    }

    private Environment mockEnvironment() {
        var env = mock(Environment.class);
        when(env.readEnv(anyString())).thenReturn("*");
        return env;
    }

    private Message createMessage(Publication publication1, UserInstance sender) throws TransactionFailedException {
        SortableIdentifier messageIdentifier = messageService.createMessage(
            sender, extractPublicationOwner(publication1),
            publication1.getIdentifier(), randomString());
        return messageService.getMessage(extractPublicationOwner(publication1), messageIdentifier);
    }

    private String randomString() {
        return FAKER.lorem().sentence();
    }

    private InputStream sampleListRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.objectMapper)
                   .withFeideId(SAMPLE_USER)
                   .withCustomerId(SOME_ORG.toString())
                   .build();
    }
}
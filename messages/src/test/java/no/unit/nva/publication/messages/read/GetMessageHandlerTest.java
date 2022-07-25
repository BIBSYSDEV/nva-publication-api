package no.unit.nva.publication.messages.read;

import static no.unit.nva.publication.messages.read.GetMessageHandler.MESSAGE_NOT_FOUND;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.messages.MessageApiConfig;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class GetMessageHandlerTest extends ResourcesLocalTest {
    
    private ResourceService resourceService;
    private MessageService messageService;
    private GetMessageHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.messageService = new MessageService(client, Clock.systemDefaultZone());
        this.handler = new GetMessageHandler();
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }
    
    @Test
    void shouldReturn404WhenMessageDoesNotExist() throws ApiGatewayException, IOException {
        var publication = createPublication();
        var sender = UserInstance.fromPublication(publication);
        var messageIdentifier = messageService.createMessage(sender, publication, randomString(), MessageType.SUPPORT);
        var input = createHttpRequest(sender, messageIdentifier);
        handler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var problem = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
        assertThat(problem.getDetail(), is(equalTo(MESSAGE_NOT_FOUND)));
    }
    
    private InputStream createHttpRequest(UserInstance sender, SortableIdentifier messageIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withNvaUsername(sender.getUserIdentifier())
            .withCustomerId(sender.getOrganizationUri())
            .withPathParameters(messagePathParameter(messageIdentifier))
            .build();
    }
    
    private Map<String, String> messagePathParameter(SortableIdentifier messageIdentifier) {
        return Map.of(MessageApiConfig.MESSAGE_IDENTIFIER_PATH_PARAMETER, messageIdentifier.toString());
    }
    
    private Publication createPublication() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}
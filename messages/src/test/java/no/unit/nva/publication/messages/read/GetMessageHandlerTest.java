package no.unit.nva.publication.messages.read;

import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.messages.MessageApiConfig.MESSAGE_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.messages.MessageApiConfig.TICKET_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetMessageHandlerTest {
    
    private GetMessageHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;
    
    @BeforeEach
    public void setup() {
        this.handler = new GetMessageHandler();
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }
    
    @Test
    void shouldRedirectToAssociatedTicketThatContainsWholeConversation() throws IOException {
        
        var message = wrapIdentifiersInSingleObject();
        var request = createHttpRequest(randomUserInstance(), message);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_SEE_OTHER)));
        var expectedLocationHeader = UriWrapper.fromHost(API_HOST)
                                         .addChild("publication")
                                         .addChild(message.getResourceIdentifier().toString())
                                         .addChild("ticket")
                                         .addChild(message.getTicketIdentifier().toString())
                                         .getUri().toString();
        
        assertThat(response.getHeaders().get("Location"), is(equalTo(expectedLocationHeader)));
    }
    
    private static Message wrapIdentifiersInSingleObject() {
        return Message.builder()
                   .withResourceIdentifier(SortableIdentifier.next())
                   .withTicketIdentifier(SortableIdentifier.next())
                   .withIdentifier(SortableIdentifier.next())
                   .build();
    }
    
    private UserInstance randomUserInstance() {
        return UserInstance.create(randomString(), randomUri());
    }
    
    private InputStream createHttpRequest(UserInstance sender, Message message)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withNvaUsername(sender.getUsername())
                   .withCustomerId(sender.getOrganizationUri())
                   .withPathParameters(messagePathParameters(message))
                   .build();
    }
    
    private Map<String, String> messagePathParameters(Message message) {
        return Map.of(
            PUBLICATION_IDENTIFIER_PATH_PARAMETER, message.getResourceIdentifier().toString(),
            TICKET_IDENTIFIER_PATH_PARAMETER, message.getTicketIdentifier().toString(),
            MESSAGE_IDENTIFIER_PATH_PARAMETER, message.getIdentifier().toString());
    }
}
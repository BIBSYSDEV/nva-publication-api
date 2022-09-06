package no.unit.nva.publication.messages;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import static no.unit.nva.publication.messages.MessageApiConfig.TICKET_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
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
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewCreateMessageHandlerTest extends ResourcesLocalTest {
    
    public static final int SINGLE_MESSAGE_CREATED = 1;
    public static final int SINGLE_EXISTING_TICKET = 0;
    private ResourceService resourceService;
    private TicketService ticketService;
    private MessageService messageService;
    private NewCreateMessageHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
        this.messageService = new MessageService(client, Clock.systemDefaultZone());
        this.handler = new NewCreateMessageHandler(messageService, ticketService);
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }
    
    @Test
    void shouldCreateMessageReferencingTicketForPublicationOwnerWithCuratorsAsRecipientWhenUserIsTheOwner()
        throws ApiGatewayException, IOException {
        var publication = draftPublicationWithoutDoi();
        var ticket = createTicket(publication);
        var user = UserInstance.fromTicket(ticket);
        var expectedText = randomString();
        var request = createNewMessageRequest(publication, ticket, user, expectedText);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var actualMessage = ticket.fetchMessages(ticketService).stream().collect(SingletonCollector.collect());
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getHeaders().get(LOCATION_HEADER),
            is(equalTo(createExpectedLocationHeader(actualMessage))));
        assertThat(actualMessage.getText(), is(equalTo(expectedText)));
    }
    
    private String createExpectedLocationHeader(Message actualMessage) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild("publication")
                   .addChild(actualMessage.getResourceIdentifier().toString())
                   .addChild("ticket")
                   .addChild(actualMessage.getTicketIdentifier().toString())
                   .addChild("message")
                   .addChild(actualMessage.getIdentifier().toString())
                   .toString();
    }
    
    private Publication draftPublicationWithoutDoi() {
        var publication = randomPublication().copy().withDoi(null).withStatus(PublicationStatus.DRAFT).build();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private InputStream createNewMessageRequest(Publication publication,
                                                TicketEntry ticket,
                                                UserInstance user,
                                                String randomString) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParameters(publication, ticket))
                   .withBody(messageBody(randomString))
                   .withNvaUsername(user.getUserIdentifier())
                   .withCustomerId(user.getOrganizationUri())
                   .build();
    }
    
    private CreateMessageRequest messageBody(String message) {
        var request = new CreateMessageRequest();
        request.setMessage(message);
        return request;
    }
    
    private static Map<String, String> pathParameters(Publication publication,
                                                      TicketEntry ticket) {
        return Map.of(
            PUBLICATION_IDENTIFIER_PATH_PARAMETER, publication.getIdentifier().toString(),
            TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString()
        );
    }
    
    private TicketEntry createTicket(Publication publication) throws ApiGatewayException {
        var newTicket = TicketEntry.requestNewTicket(publication, randomTicketType());
        return ticketService.createTicket(newTicket, newTicket.getClass());
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends TicketEntry> randomTicketType() {
        var types = TypeProvider.listSubTypes(TicketEntry.class).collect(Collectors.toList());
        return (Class<? extends TicketEntry>) randomElement(types);
    }
}
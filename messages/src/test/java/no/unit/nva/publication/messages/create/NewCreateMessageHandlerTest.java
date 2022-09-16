package no.unit.nva.publication.messages.create;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import static no.unit.nva.publication.messages.MessageApiConfig.TICKET_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.model.business.TicketEntry.SUPPORT_SERVICE_CORRESPONDENT;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NewCreateMessageHandlerTest extends ResourcesLocalTest {
    
    private ResourceService resourceService;
    private TicketService ticketService;
    private NewCreateMessageHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;
    
    public static Stream<Arguments> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class).map(Arguments::of);
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
        MessageService messageService = new MessageService(client, Clock.systemDefaultZone());
        this.handler = new NewCreateMessageHandler(messageService, ticketService);
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }
    
    @Test
    void shouldCreateMessageReferencingTicketForPublicationOwnerWithNonSpecificCuratorAsRecipientWhenUserIsTheOwner()
        throws ApiGatewayException, IOException {
        var publication = draftPublicationWithoutDoi();
        var ticket = createTicket(publication);
        var user = UserInstance.fromTicket(ticket);
        var expectedText = randomString();
        var request = createNewMessageRequestForNonElevatedUser(publication, ticket, user, expectedText);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
    
        assertThatResponseContainsCorrectInformation(response, ticket);
        var expectedRecipient = SUPPORT_SERVICE_CORRESPONDENT;
        var expectedSender = UserInstance.fromPublication(publication).getUser();
        assertThatMessageContainsTextAndCorrectCorrespondentInfo(expectedText, ticket, expectedSender,
            expectedRecipient);
    }
    
    @Test
    void shouldReturnForbiddenWhenUserAttemptsToAddMessageWhenTheyAreNotThePublicationOwnerOrCurator()
        throws ApiGatewayException, IOException {
        var publication = draftPublicationWithoutDoi();
        var ticket = createTicket(publication);
        var user = randomUserInstance(ticket.getCustomerId());
        var request = createNewMessageRequestForNonElevatedUser(publication, ticket, user, randomString());
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @Test
    void shouldCreateMessageForTicketWithRecipientThePubOwnerAndSenderTheSpecificCuratorWhenSenderIsCurator()
        throws ApiGatewayException, IOException {
        var publication = draftPublicationWithoutDoi();
        var ticket = createTicket(publication);
        var sender = UserInstance.create(randomString(), publication.getPublisher().getId());
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, expectedText);
    
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
    
        assertThatResponseContainsCorrectInformation(response, ticket);
        var expectedSender = sender.getUser();
        var expectedRecipient = UserInstance.fromPublication(publication).getUser();
        assertThatMessageContainsTextAndCorrectCorrespondentInfo(expectedText, ticket, expectedSender,
            expectedRecipient);
    }
    
    @Test
    void shouldReturnForbiddenWhenSenderIsCuratorOfAnAlienInstitution()
        throws ApiGatewayException, IOException {
        var publication = draftPublicationWithoutDoi();
        var ticket = createTicket(publication);
        var sender = UserInstance.create(randomString(), randomUri());
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, expectedText);
    
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @ParameterizedTest(name = "ticketType:{0}")
    @DisplayName("should mark ticket as unread for Publication Owner  and mark and as read for curators"
                 + "when curator sends a message")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsUnreadForPublicationOwnerWhenCuratorSendsAMessage(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = draftPublicationWithoutDoi();
        var ticket = createTicket(publication, ticketType);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(ticket.getViewedBy(), not(hasItem(SUPPORT_SERVICE_CORRESPONDENT)));
        var sender = UserInstance.create("NOT_EXPECTED", publication.getPublisher().getId());
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, randomString());
        handler.handleRequest(request, output, context);
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), not(hasItem(ticket.getOwner())));
        assertThat(updatedTicket.getViewedBy(), hasItem(SUPPORT_SERVICE_CORRESPONDENT));
    }
    
    @ParameterizedTest(name = "ticketType:{0}")
    @DisplayName("should mark ticket as unread for Curator and read for Publication Owner "
                 + "when Publication Owner sends a Message")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsUnreadForCuratorAndReadForPublicationOwnerWhenOwnerSendsAMessage(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var publication = draftPublicationWithoutDoi();
        var ticket = createTicket(publication, ticketType);
        ticket.markReadForCurators().persistUpdate(ticketService);
        ticket.markUnreadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(ticket.getOwner())));
        assertThat(ticket.getViewedBy(), hasItem(SUPPORT_SERVICE_CORRESPONDENT));
        
        var sender = UserInstance.create(ticket.getOwner(), ticket.getCustomerId());
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, randomString());
        handler.handleRequest(request, output, context);
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(updatedTicket.getViewedBy(), not(hasItem(SUPPORT_SERVICE_CORRESPONDENT)));
    }
    
    private static Map<String, String> pathParameters(Publication publication,
                                                      TicketEntry ticket) {
        return Map.of(
            PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publication.getIdentifier().toString(),
            TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString()
        );
    }
    
    private void assertThatMessageContainsTextAndCorrectCorrespondentInfo(String expectedText,
                                                                          TicketEntry ticket,
                                                                          User expectedSender,
                                                                          User expectedRecipient) {
        var actualMessage = ticket.fetchMessages(ticketService).stream().collect(SingletonCollector.collect());
        assertThat(actualMessage.getText(), is(equalTo(expectedText)));
        assertThat("Recepient was:" + actualMessage.getRecipient(), actualMessage.getRecipient(),
            is(equalTo(expectedRecipient)));
        assertThat("Sender was:" + actualMessage.getSender(), actualMessage.getSender(), is(equalTo(expectedSender)));
    }
    
    private void assertThatResponseContainsCorrectInformation(GatewayResponse<Void> response, TicketEntry ticket) {
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        
        var actualMessage = ticket.fetchMessages(ticketService).stream().collect(SingletonCollector.collect());
        assertThat(response.getHeaders().get(LOCATION_HEADER),
            is(equalTo(createExpectedLocationHeader(actualMessage))));
    }
    
    private UserInstance randomUserInstance(URI customerId) {
        return UserInstance.create(randomString(), customerId);
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
    
    private InputStream createNewMessageRequestForNonElevatedUser(Publication publication,
                                                                  TicketEntry ticket,
                                                                  UserInstance userInstance,
                                                                  String randomString) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParameters(publication, ticket))
                   .withBody(messageBody(randomString))
                   .withNvaUsername(userInstance.getUsername())
                   .withCustomerId(userInstance.getOrganizationUri())
                   .build();
    }
    
    private InputStream createNewMessageRequestForElevatedUser(Publication publication,
                                                               TicketEntry ticket,
                                                               UserInstance user,
                                                               String message) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParameters(publication, ticket))
                   .withBody(messageBody(message))
                   .withNvaUsername(user.getUsername())
                   .withCustomerId(user.getOrganizationUri())
                   .withAccessRights(user.getOrganizationUri(), AccessRight.APPROVE_DOI_REQUEST.toString())
                   .build();
    }
    
    private CreateMessageRequest messageBody(String message) {
        var request = new CreateMessageRequest();
        request.setMessage(message);
        return request;
    }
    
    private TicketEntry createTicket(Publication publication) throws ApiGatewayException {
        return createTicket(publication, randomTicketType());
    }
    
    private TicketEntry createTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends TicketEntry> randomTicketType() {
        var types = TypeProvider.listSubTypes(TicketEntry.class).collect(Collectors.toList());
        return (Class<? extends TicketEntry>) randomElement(types);
    }
}
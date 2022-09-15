package no.unit.nva.publication.publishingrequest.read;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.APPROVE_DOI_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.MessageDto;
import no.unit.nva.publication.publishingrequest.TicketConfig;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.publishingrequest.TicketTestLocal;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

class GetTicketHandlerTest extends TicketTestLocal {
    
    private GetTicketHandler handler;
    private MessageService messageService;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new GetTicketHandler(ticketService);
        this.messageService = new MessageService(client, Clock.systemDefaultZone());
    }
    
    @ParameterizedTest(name = " ticket type: {0}")
    @DisplayName("should return ticket when client is owner of associated publication "
                 + "and therefore of the ticket")
    @MethodSource("ticketTypeProvider")
    void shouldReturnTicketWhenClientIsOwnerOfAssociatedPublicationAndThereforeOfTheTicket(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var publication = resourceService.getPublicationByIdentifier(ticket.getResourceIdentifier());
        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);
        var actualTicketEntry = ticketDto.toTicket();
        assertThat(TicketDto.fromTicket(actualTicketEntry), is(equalTo(ticketDto)));
    }
    
    @ParameterizedTest(name = " ticket type: {0}")
    @DisplayName("should  return not found when publication identifier exists, but ticket identifier does not "
                 + "correspond to a ticket of that publication")
    @MethodSource("ticketTypeProvider")
    void shouldReturnNotFoundWhenPublicationIdentifierExistsButTicketIdentifierDoesCorrespondToPublication(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication();
        var otherPublication = createAndPersistDraftPublication();
        var ticket = createPersistedTicket(ticketType, otherPublication);
        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, output, CONTEXT);
    
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    @ParameterizedTest(name = " ticket type: {0}")
    @DisplayName("should  return not found when user is not the owner of the associated publication")
    @MethodSource("ticketTypeProvider")
    void shouldReturnNotFoundWhenUserIsNotTheOwnerOfTheAssociatedPublication(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication();
        var ticket = createPersistedTicket(ticketType, publication);
        var request = createHttpRequest(publication, ticket, randomOwner()).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should return ticket when curator is requester")
    @MethodSource("ticketTypeProvider")
    void shouldReturnTicketWhenCuratorIsRequester(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var request =
            createHttpRequestForElevatedUser(ticket, ticket.getCustomerId(), APPROVE_DOI_REQUEST).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);
        var actualTicketEntry = ticketDto.toTicket();
        assertThat(TicketDto.fromTicket(actualTicketEntry), is(equalTo(ticketDto)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should return Not Found when requester is curator of wrong institution")
    @MethodSource("ticketTypeProvider")
    void shouldReturnTicketWhenRequesterIsCuratorOfWrongInstitution(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var request = createHttpRequestForElevatedUser(ticket, randomUri(), APPROVE_DOI_REQUEST).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should return Not Found when requester is the wrong type of elevated user")
    @MethodSource("ticketTypeProvider")
    void shouldReturnNotFoundWhenRequestIsTheWrongTypeOfElevatedUser(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var wrongAccessRights = new HashSet<>(Arrays.asList(AccessRight.values()));
        wrongAccessRights.remove(APPROVE_DOI_REQUEST);
        var request =
            createHttpRequestForElevatedUser(ticket, ticket.getCustomerId(), randomElement(wrongAccessRights)).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should return ticket with messages when ticket has messages")
    @MethodSource("ticketTypeProvider")
    void shouldReturnTicketWithMessagesWhenTicketHasMessages(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var sender = UserInstance.fromTicket(ticket);
        var expectedMessage = messageService.createMessage(ticket, sender, randomString());
    
        var request = createHttpRequest(ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
    
        var responseBody = response.getBodyObject(TicketDto.class);
        var expectedDto = MessageDto.fromMessage(expectedMessage);
        assertThat(expectedDto, is(in(responseBody.getMessages())));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should return viewed by owner when ticket is new ")
    @MethodSource("ticketTypeProvider")
    void shouldReturnViewedByOwnerWhenTicketIsNew(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var request = createHttpRequest(ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        var responseBody = response.getBodyObject(TicketDto.class);
        assertThat(responseBody.getViewedBy(), hasItem(ticket.getOwner()));
    }
    
    @ParameterizedTest(name = "ticketType:{0}")
    @DisplayName("should mark ticket as Unread for the publication owner when ticket is marked as unread")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsUnreadForThePublicationOwnerWhenTicketIsMarkedAsUnread(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        ticket.markUnreadByOwner().persistUpdate(ticketService);
        var updatedTicket = ticket.fetch(ticketService);
        assertThatPersistedTicketsIsMarkedAsUnreadForTheOwner(updatedTicket);
        var request = createHttpRequest(ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        var responseBody = response.getBodyObject(TicketDto.class);
        assertThatHandlerReturnsDtoMakingVisibleTheFactThatTheOwnerHasNotReadTheMessage(updatedTicket, responseBody);
    }
    
    private static void assertThatHandlerReturnsDtoMakingVisibleTheFactThatTheOwnerHasNotReadTheMessage(
        TicketEntry updatedTicket, TicketDto responseBody) {
        assertThat(responseBody.getViewedBy(), not(hasItem(updatedTicket.getOwner())));
    }
    
    private static void assertThatPersistedTicketsIsMarkedAsUnreadForTheOwner(TicketEntry updatedTicket) {
        assertThat(updatedTicket.getViewedBy(), not(hasItem(updatedTicket.getOwner())));
    }
    
    private static Map<String, String> createPathParameters(Publication publication, TicketEntry ticket) {
        return Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER,
            publication.getIdentifier().toString(),
            TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME, ticket.getIdentifier().toString());
    }
    
    private HandlerRequestBuilder<TicketDto> createHttpRequestForElevatedUser(TicketEntry ticket,
                                                                              URI customerId,
                                                                              AccessRight accessRight)
        throws NotFoundException {
        return createHttpRequest(ticket)
                   .withCustomerId(customerId)
                   .withNvaUsername(randomString())
                   .withAccessRights(ticket.getCustomerId(), accessRight.toString());
    }
    
    private TicketEntry createPersistedTicket(Class<? extends TicketEntry> ticketType, Publication publication)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
    }
    
    private User randomOwner() {
        return new User(randomString());
    }
    
    private HandlerRequestBuilder<TicketDto> createHttpRequest(TicketEntry ticket)
        throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(ticket.getResourceIdentifier());
        return createHttpRequest(publication, ticket);
    }
    
    private HandlerRequestBuilder<TicketDto> createHttpRequest(Publication publication, TicketEntry ticket) {
        return createHttpRequest(publication, ticket, ticket.getOwner());
    }
    
    private HandlerRequestBuilder<TicketDto> createHttpRequest(Publication publication, TicketEntry ticket,
                                                               User owner) {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(owner.toString())
                   .withPathParameters(createPathParameters(publication, ticket));
    }
}
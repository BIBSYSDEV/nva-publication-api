package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_GONE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.ticket.TicketDtoParser.toTicket;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.ticket.MessageDto;
import no.unit.nva.publication.ticket.PublishingRequestDto;
import no.unit.nva.publication.ticket.TicketConfig;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketDtoStatus;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

class GetTicketHandlerTest extends TicketTestLocal {

    private GetTicketHandler handler;
    private MessageService messageService;

    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new GetTicketHandler(ticketService, new Environment());
        this.messageService = getMessageService();
    }

    @ParameterizedTest
    @DisplayName("should return ticket when client is owner of associated publication "
                 + "and therefore of the ticket")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnTicketWhenClientIsOwnerOfAssociatedPublicationAndThereforeOfTheTicket(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = createPersistedTicket(ticketType, publication);

        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);
        var actualTicketEntry = toTicket(ticketDto);
        var expected = TicketDto.fromTicket(actualTicketEntry);
        assertThat(ticketDto, is(equalTo(expected)));
    }

    @Test
    void shouldReturnTicketWithPublishingWorkflowWhenTicketTypeIsPublishingRequest()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var ticket = (PublishingRequestCase) createPersistedTicket(PublishingRequestCase.class, publication);

        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, PublishingRequestDto.class);
        var publishingRequestDto = response.getBodyObject(PublishingRequestDto.class);
        assertThat(publishingRequestDto.getWorkflow(), is(equalTo(ticket.getWorkflow())));
    }

    @Test
    void shouldReturnGoneWhenTicketHasBeenRemoved()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = (DoiRequest) createPersistedTicket(DoiRequest.class, publication);
        ticket.remove(UserInstance.fromTicket(ticket)).persistUpdate(ticketService);
        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, PublishingRequestDto.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_GONE)));
    }

    @Test
    void shouldReturnTicketWithStatusNewWhenTicketIsUnassignedAndInProgress()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var ticket = (PublishingRequestCase) createPersistedTicket(PublishingRequestCase.class, publication);

        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);
        assertThat(ticketDto.getStatus(), is(equalTo(TicketDtoStatus.NEW)));
    }

    @Test
    void shouldReturnTicketWithMessageWithoutTextWhenMessageHasStatusDeleted()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var ticket = (PublishingRequestCase) createPersistedTicket(PublishingRequestCase.class, publication);
        var userInstance = UserInstance.fromTicket(ticket);
        var message = messageService.createMessage(ticket, userInstance, randomString());
        messageService.deleteMessage(userInstance, message);
        var request = createHttpRequest(publication, ticket).build();

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);

        assertThat(ticketDto.getMessages().getFirst().getText(),  is(nullValue()));
    }

    @ParameterizedTest
    @DisplayName("should  return not found when publication identifier exists, but ticket identifier does not "
                 + "correspond to a ticket of that publication")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnNotFoundWhenPublicationIdentifierExistsButTicketIdentifierDoesCorrespondToPublication(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var otherPublication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = createPersistedTicket(ticketType, otherPublication);
        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    @ParameterizedTest
    @DisplayName("should return ticket when curator is requester")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnTicketWhenCuratorIsRequester(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request =
            createHttpRequestForElevatedUser(ticket, ticket.getCustomerId(), MANAGE_DOI).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);
        var actualTicketEntry = toTicket(ticketDto);
        assertThat(ticketDto, is(equalTo(TicketDto.fromTicket(actualTicketEntry))));
    }

    @ParameterizedTest
    @DisplayName("should return ticket with messages when ticket has messages")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnTicketWithMessagesWhenTicketHasMessages(Class<? extends TicketEntry> ticketType,
                                                             PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
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

    @ParameterizedTest
    @DisplayName("should return viewed by owner when ticket is new ")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotReturnViewedByOwnerWhenTicketIsNew(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = createHttpRequest(ticket).build();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        var responseBody = response.getBodyObject(TicketDto.class);
        assertTrue(responseBody.getViewedBy().isEmpty());
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as Unread for the publication owner when ticket is marked as unread")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForThePublicationOwnerWhenTicketIsMarkedAsUnread(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
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
        return Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                      publication.getIdentifier().toString(),
                      TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME, ticket.getIdentifier().toString());
    }

    private HandlerRequestBuilder<TicketDto> createHttpRequestForElevatedUser(TicketEntry ticket,
                                                                              URI customerId,
                                                                              AccessRight accessRight)
        throws NotFoundException {
        return createHttpRequest(ticket)
                   .withCurrentCustomer(customerId)
                   .withUserName(randomString())
                   .withAccessRights(customerId, accessRight);
    }

    private TicketEntry createPersistedTicket(Class<? extends TicketEntry> ticketType, Publication publication)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType)
                   .withOwner(UserInstance.fromPublication(publication).getUsername())
                   .persistNewTicket(ticketService);
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
                   .withCurrentCustomer(ticket.getCustomerId())
                   .withUserName(owner.toString())
                   .withTopLevelCristinOrgId(randomUri())
                   .withPersonCristinId(randomUri())
                   .withPathParameters(createPathParameters(publication, ticket));
    }
}
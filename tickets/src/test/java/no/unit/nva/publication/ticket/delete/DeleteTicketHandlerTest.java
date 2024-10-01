package no.unit.nva.publication.ticket.delete;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.RequestUtil.TICKET_IDENTIFIER;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static software.amazon.awssdk.http.HttpStatusCode.NOT_FOUND;
import static software.amazon.awssdk.http.HttpStatusCode.UNAUTHORIZED;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteTicketHandlerTest extends ResourcesLocalTest {

    public static final FakeContext CONTEXT = new FakeContext();
    private TicketService ticketService;
    private ResourceService resourceService;
    private DeleteTicketHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void setup() {
        super.init();
        this.output = new ByteArrayOutputStream();
        this.ticketService = getTicketService();
        this.resourceService = getResourceServiceBuilder().build();
        this.handler = new DeleteTicketHandler(ticketService);
    }

    @Test
    void shouldReturnNotFoundWhenTicketDoesNotExist() throws IOException {
        var request = deleteRequest(SortableIdentifier.next(), SortableIdentifier.next());

        handler.handleRequest(request, output, CONTEXT);

        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(), is(equalTo(NOT_FOUND)));
    }

    @Test
    void shouldReturnUnauthorizedWhenAttemptingToDeleteTicketUserDoesNotOwn() throws IOException, ApiGatewayException {
        var publication = createPublication();
        var ticket = createTicket(publication);
        var request = deleteRequest(publication.getIdentifier(), ticket.getIdentifier());

        handler.handleRequest(request, output, CONTEXT);

        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(), is(equalTo(UNAUTHORIZED)));
    }

    @Test
    void shouldReturnBadGatewayWhenUnexpectedErrorOccurs() throws IOException, ApiGatewayException {
        var publication = createPublication();
        var ticket = createTicket(publication);
        var request = deleteRequestForUser(publication.getIdentifier(), ticket.getIdentifier(),
                                           ticket.getOwner());

        new DeleteTicketHandler(ticketServiceThrowingException()).handleRequest(request, output, CONTEXT);

        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(), is(equalTo(HTTP_BAD_GATEWAY)));
    }

    @Test
    void shouldRemoveTicketSuccessfully() throws IOException, ApiGatewayException {
        var publication = createPublication();
        var ticket = createTicket(publication);
        var request = deleteRequestForUser(publication.getIdentifier(), ticket.getIdentifier(),
                                           ticket.getOwner());

        handler.handleRequest(request, output, CONTEXT);

        var removedTicket = ticket.fetch(ticketService);

        assertThat(removedTicket.getStatus(), is(equalTo(TicketStatus.REMOVED)));
        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(), is(equalTo(HTTP_OK)));
    }

    private static Map<String, String> pathParams(SortableIdentifier publicationIdentifier,
                                                  SortableIdentifier ticketIdentifier) {
        return Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString(), TICKET_IDENTIFIER,
                      ticketIdentifier.toString());
    }

    private TicketService ticketServiceThrowingException() {
        ticketService = mock(TicketService.class);
        doThrow(new RuntimeException()).when(ticketService).updateTicket(any());
        return ticketService;
    }

    private TicketEntry createTicket(Publication publication) throws ApiGatewayException {
        return TicketEntry.createNewTicket(publication, PublishingRequestCase.class, SortableIdentifier::next,
                                           publication.getPublisher().getId())
                   .persistNewTicket(ticketService);
    }

    private Publication createPublication() throws BadRequestException {
        var publication = randomPublication();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private InputStream deleteRequest(SortableIdentifier publicationIdentifier,
                                      SortableIdentifier ticketIdentifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).withPathParameters(
                pathParams(publicationIdentifier, ticketIdentifier))
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .build();
    }

    private InputStream deleteRequestForUser(SortableIdentifier publicationIdentifier,
                                             SortableIdentifier ticketIdentifier, User user)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).withPathParameters(
                pathParams(publicationIdentifier, ticketIdentifier))
                   .withUserName(nonNull(user) ? user.toString() : null)
                   .withCurrentCustomer(randomUri())
                   .build();
    }
}
package no.unit.nva.publication.publishingrequest.update;

import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.publishingrequest.TicketConfig;
import no.unit.nva.publication.publishingrequest.TicketTestLocal;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class UpdateTicketViewStatusHandlerTest extends TicketTestLocal {
    
    private UpdateTicketViewStatusHandler handler;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new UpdateTicketViewStatusHandler(ticketService);
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should mark ticket as read for owner when user is ticket owner and marks it as read")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsReadFoOwnerWhenUserIsTicketOwnerAndMarksItAsRead(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication();
        var ticket = persistTicket(publication, ticketType);
        ticket.markUnreadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(ticket.getOwner())));
        
        var httpRequest = createOwnerMarksTicket(publication, ticket, ViewStatus.READ);
        handler.handleRequest(httpRequest, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasItem(ticket.getOwner()));
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should mark ticket as unread for owner when user is ticket owner and marks it as unread")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsReadFoOwnerWhenUserIsTicketOwnerAndMarksItAsUnread(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication();
        var ticket = persistTicket(publication, ticketType);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        
        var httpRequest = createOwnerMarksTicket(publication, ticket, ViewStatus.UNREAD);
        handler.handleRequest(httpRequest, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), not(hasItem(ticket.getOwner())));
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should mark ticket as Read for all Curators when user is curator and marks it as read")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsReadForAllCuratorsWhenUserIsCuratorAndMarksItAsRead(
        Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication();
        var ticket = persistTicket(publication, ticketType);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(ticket.getViewedBy(), not(hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT)));
        
        var httpRequest = curatorMarksTicket(publication, ticket, ViewStatus.READ);
        handler.handleRequest(httpRequest, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(updatedTicket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should mark ticket as Unread for all Curators when user is curator and marks it as unread")
    @MethodSource("ticketTypeProvider")
    void shouldMarkTicketAsUnreadForAllCuratorsWhenUserIsCuratorAndMarksItAsUnread(
        Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication();
        var ticket = persistTicket(publication, ticketType);
        ticket.markReadForCurators().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(ticket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));
        
        var httpRequest = curatorMarksTicket(publication, ticket, ViewStatus.UNREAD);
        handler.handleRequest(httpRequest, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
        
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(updatedTicket.getViewedBy(), not(hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT)));
    }
    
    @Test
    void shouldNotAllowCuratorsOfAlienOrganizationsToMarkTicketsAsReadOrUnread() {
        fail()
    }
    
    private InputStream curatorMarksTicket(Publication publication, TicketEntry ticket, ViewStatus viewStatus)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateViewStatusRequest>(JsonUtils.dtoObjectMapper)
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(randomString())
                   .withAccessRights(ticket.getCustomerId(), AccessRight.APPROVE_DOI_REQUEST.toString())
                   .withBody(new UpdateViewStatusRequest(viewStatus))
                   .withPathParameters(Map.of(
                       PUBLICATION_IDENTIFIER_PATH_PARAMETER,
                       publication.getIdentifier().toString(),
                       TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                       ticket.getIdentifier().toString()))
                   .build();
    }
    
    private static InputStream createOwnerMarksTicket(Publication publication,
                                                      TicketEntry ticket,
                                                      ViewStatus viewStatus) throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateViewStatusRequest>(JsonUtils.dtoObjectMapper)
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(ticket.getOwner().toString())
                   .withBody(new UpdateViewStatusRequest(viewStatus))
                   .withPathParameters(Map.of(
                       PUBLICATION_IDENTIFIER_PATH_PARAMETER,
                       publication.getIdentifier().toString(),
                       TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                       ticket.getIdentifier().toString()))
                   .build();
    }
}
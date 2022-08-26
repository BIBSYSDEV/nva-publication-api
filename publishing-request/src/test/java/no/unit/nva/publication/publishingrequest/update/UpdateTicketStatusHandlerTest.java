package no.unit.nva.publication.publishingrequest.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.publishingrequest.TicketTest;
import no.unit.nva.publication.publishingrequest.TicketUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class UpdateTicketStatusHandlerTest extends TicketTest {
    
    private UpdateTicketStatusHandler handler;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new UpdateTicketStatusHandler(ticketService);
    }
    
    @Test
    void shouldCompletePendingDoiRequestWhenUserIsCuratorAndPublicationIsPublished()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        var completedTicket = ticket.complete(publication);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getStatus(), is(equalTo(completedTicket.getStatus())));
    }
    
    // User is not curator
    @Test
    void shouldReturnForbiddenWhenRequestingUserIsNotCurator() throws IOException, ApiGatewayException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        var completedTicket = ticket.complete(publication);
        var request = createCompleteTicketHttpRequest(
            completedTicket,
            AccessRight.USER,
            completedTicket.getCustomerId()
        );
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @Test
    void shouldReturnForbiddenWhenRequestingUserIsCuratorAtOtherCustomerThanCurrentPublisher()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        var completedTicket = ticket.complete(publication);
        var customer = randomUri();
        var request = createCompleteTicketHttpRequest(completedTicket, AccessRight.APPROVE_DOI_REQUEST, customer);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @Test
    void shouldReturnBadRequestWhenUserAttemptsToCompleteTicketForDoiRequestOnDraftPublication()
        throws ApiGatewayException,
               IOException {
        var publication = createAndPersistDraftPublication();
        var ticket = (DoiRequest) createPersistedTicket(publication, DoiRequest.class);
        ticket.setStatus(TicketStatus.COMPLETED);
        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    @Test
    void shouldReturnAcceptedWhenCompletingAnAlreadyCompletedDoiRequestTicket()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        var completedTicket = ticketService.completeTicket(ticket);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }
    
    @Disabled
    @Test
    void shouldReturnBadRequestWhenUserAttemptsToDeCompleteCompletedDoiRequest()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        ticketService.completeTicket(ticket);
        var decompletedTicket = (DoiRequest) ticket;
        decompletedTicket.setStatus(TicketStatus.PENDING);
        var request = authorizedUserCompletesTicket(decompletedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getStatus(), is(equalTo(TicketStatus.COMPLETED)));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    private InputStream authorizedUserCompletesTicket(TicketEntry ticket) throws JsonProcessingException {
        return createCompleteTicketHttpRequest(ticket, AccessRight.APPROVE_DOI_REQUEST, ticket.getCustomerId());
    }
    
    private InputStream createCompleteTicketHttpRequest(TicketEntry ticket,
                                                        AccessRight accessRight,
                                                        URI customer) throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(TicketDto.fromTicket(ticket))
                   .withAccessRights(customer, accessRight.toString())
                   .withCustomerId(customer)
                   .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER,
                       ticket.getResourceIdentifier().toString(),
                       TicketUtils.TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString()))
                   .build();
    }
}
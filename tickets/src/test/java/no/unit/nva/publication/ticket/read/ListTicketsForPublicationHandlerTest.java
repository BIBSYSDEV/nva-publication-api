package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import no.unit.nva.publication.ticket.test.TicketTestUtils;

class ListTicketsForPublicationHandlerTest extends TicketTestLocal {
    
    private ListTicketsForPublicationHandler handler;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new ListTicketsForPublicationHandler(resourceService);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnAllTicketsForPublicationWhenUserIsThePublicationOwner(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = ownerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        assertThatResponseContainsExpectedTickets(ticket);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenForPublicationWhenUserIsNotTheOwnerAndNotElevatedUser(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = nonOwnerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnAllTicketsForPublicationWhenUserIsElevatedUser(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = elevatedUserRequestsTicketsForPublication(publication);
    
        handler.handleRequest(request, output, CONTEXT);
        assertThatResponseContainsExpectedTickets(ticket);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenWhenUserIsElevatedUserOfAlienOrganization(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = elevatedUserOfAlienOrgRequestsTicketsForPublication(publication);
        
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }
    
    private static InputStream ownerRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCustomerId(publication.getPublisher().getId())
                   .withNvaUsername(publication.getResourceOwner().getOwner())
                   .build();
    }
    
    private static InputStream nonOwnerRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCustomerId(publication.getPublisher().getId())
                   .withNvaUsername(randomString())
                   .build();
    }
    
    private static Map<String, String> constructPathParameters(Publication publication) {
        return Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
            publication.getIdentifier().toString());
    }
    
    private InputStream elevatedUserOfAlienOrgRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCustomerId(customerId)
                   .withNvaUsername(randomString())
                   .withAccessRights(customerId, AccessRight.APPROVE_DOI_REQUEST.toString())
                   .build();
    }
    
    private InputStream elevatedUserRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCustomerId(publication.getPublisher().getId())
                   .withNvaUsername(randomString())
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.APPROVE_DOI_REQUEST.toString())
                   .build();
    }
    
    private void assertThatResponseContainsExpectedTickets(TicketEntry ticket) throws JsonProcessingException {
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        var actualTicketIdentifiers = body.getTickets()
                                          .stream()
                                          .map(TicketDto::toTicket)
                                          .map(Entity::getIdentifier)
                                          .collect(Collectors.toList());
        var expectedIdentifiers = ticket.getIdentifier();
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(actualTicketIdentifiers, containsInAnyOrder(expectedIdentifiers));
    }
}
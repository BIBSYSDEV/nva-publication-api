package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
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
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListTicketsForPublicationHandlerTest extends TicketTestLocal {
    
    private ListTicketsForPublicationHandler handler;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new ListTicketsForPublicationHandler(resourceService);
    }
    
    @Test
    void shouldReturnAllTicketsForPublicationWhenUserIsThePublicationOwner() throws IOException {
        var publication = createAndPersistPublication(PublicationStatus.DRAFT);
        var tickets = createTicketsOfAllTypes(publication);
        var request = ownerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        assertThatResponseContainsExpectedTickets(tickets);
    }
    
    @Test
    void shouldReturnForbiddenForPublicationWhenUserIsNotTheOwnerAndNotElevatedUser() throws IOException {
        var publication = createAndPersistPublication(PublicationStatus.DRAFT);
        var tickets = createTicketsOfAllTypes(publication);
        var request = nonOwnerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }
    
    @Test
    void shouldReturnAllTicketsForPublicationWhenUserIsElevatedUser() throws IOException {
        var publication = createAndPersistPublication(PublicationStatus.DRAFT);
        var tickets = createTicketsOfAllTypes(publication);
        var request = elevatedUserRequestsTicketsForPublication(publication);
    
        handler.handleRequest(request, output, CONTEXT);
        assertThatResponseContainsExpectedTickets(tickets);
    }
    
    @Test
    void shouldReturnForbiddenWhenUserIsElevatedUserOfAlienOrganization() throws IOException {
        var publication = createAndPersistPublication(PublicationStatus.DRAFT);
        var tickets = createTicketsOfAllTypes(publication);
        var request = elevatedUserOfAlienOrgRequestsTicketsForPublication(publication);
        
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
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
    
    private InputStream elevatedUserRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCustomerId(publication.getPublisher().getId())
                   .withNvaUsername(randomString())
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.APPROVE_DOI_REQUEST.toString())
                   .build();
    }
    
    private void assertThatResponseContainsExpectedTickets(List<TicketEntry> tickets) throws JsonProcessingException {
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        var actualTicketIdentifiers = body.getTickets()
                                          .stream()
                                          .map(TicketDto::toTicket)
                                          .map(Entity::getIdentifier)
                                          .collect(Collectors.toList());
        var expectedIdentifiers = tickets.stream().map(TicketEntry::getIdentifier).collect(Collectors.toList());
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(actualTicketIdentifiers, containsInAnyOrder(expectedIdentifiers.toArray(SortableIdentifier[]::new)));
    }
    
    private List<TicketEntry> createTicketsOfAllTypes(Publication publication) {
        return ticketTypeProvider()
                   .map(type -> (Class<? extends TicketEntry>) type)
                   .map(type -> TicketEntry.requestNewTicket(publication, type))
                   .map(attempt(unpersistedTicket -> unpersistedTicket.persistNewTicket(ticketService)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }
}
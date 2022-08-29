package no.unit.nva.publication.publishingrequest.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

class UpdateTicketStatusHandlerTest extends TicketTest {
    
    private UpdateTicketStatusHandler handler;
    
    public static Stream<Arguments> ticketAndBadStatusProvider() {
        return Stream.of(
            Arguments.of(DoiRequest.class, PublicationStatus.DRAFT),
            Arguments.of(DoiRequest.class, PublicationStatus.DRAFT_FOR_DELETION),
            Arguments.of(PublishingRequestCase.class, PublicationStatus.DRAFT_FOR_DELETION));
    }
    
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
    void shouldReturnAcceptedWhenCompletingAnAlreadyCompletedDoiRequestTicket()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedTicket(publication, DoiRequest.class);
        var completedTicket = ticketService.updateTicketStatus(ticket, COMPLETED);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should return a Bad Request response when attempting to re-open a ticket.")
    @MethodSource("ticketTypeProvider")
    void shouldReturnBadRequestWhenUserAttemptsToDeCompleteCompletedTicket(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = createPublicationForTicket(ticketType);
        var ticket = createPersistedTicket(publication, ticketType);
        ticketService.updateTicketStatus(ticket, COMPLETED);
        ticket.setStatus(TicketStatus.PENDING);
        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getStatus(), is(equalTo(COMPLETED)));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    @ParameterizedTest(name = "ticket type: {0} with status {1}")
    @DisplayName("should return a Bad Request when attempting to complete incompletable ticket cases")
    @MethodSource("ticketAndBadStatusProvider")
    void shouldReturnBadRequestWhenAttemptingToCompleteIncompletableTicketCases(Class<? extends TicketEntry> ticketType,
                                                                                PublicationStatus publicationStatus)
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication();
        var ticket = createPersistedTicket(publication, ticketType);
        resourceService.updatePublication(publication.copy().withStatus(publicationStatus).build());
        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
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
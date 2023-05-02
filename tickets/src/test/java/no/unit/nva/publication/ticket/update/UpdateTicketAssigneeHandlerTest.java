package no.unit.nva.publication.ticket.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.ticket.TicketConfig;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class UpdateTicketAssigneeHandlerTest extends TicketTestLocal {

    private UpdateTicketAssigneeHandler handler;
    public static final String SOME_ASSIGNEE = "some@user";
    public static final String TICKET_IDENTIFIER_PATH_PARAMETER = "ticketIdentifier";

    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new UpdateTicketAssigneeHandler(ticketService);
    }

    @Test
    void shouldAddAssigneeToTicketWhenUserIsCuratorAndTicketHasNoAssignee()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        assertThat(publication.getDoi(), is(nullValue()));
        var ticket = createPersistedDoiTicket(publication);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var addAssigneeTicket = ticket.updateAssignee(publication, new Username(user.getUsername()));
        var request = authorizedUserAssigneesTicket(publication, addAssigneeTicket, user);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(addAssigneeTicket.getAssignee())));
    }

    @Test
    void shouldUpdateAssigneeToTicketWhenCuratorAttemptsToClaimTheTicket()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = createPersistedDoiTicket(publication);
        var assignee = UserInstance.create(SOME_ASSIGNEE, publication.getPublisher().getId());
        ticket.setAssignee(new Username(assignee.getUsername()));

        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var addAssigneeTicket = ticket.updateAssignee(publication, new Username(user.getUsername()));

        var request = authorizedUserAssigneesTicket(publication, addAssigneeTicket, user);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var actualTicket = ticketService.fetchTicket(ticket);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        assertThat(actualTicket.getAssignee(), is(equalTo(addAssigneeTicket.getAssignee())));
    }

    @Test
    void shouldReturnForbiddenWhenRequestingUserIsNotCurator() throws IOException, ApiGatewayException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var user = UserInstance.fromTicket(ticket);
        var addAssigneeTicket = ticket.updateAssignee(publication, new Username(user.getUsername()));
        var request = createAssigneeTicketHttpRequest(
            addAssigneeTicket,
            AccessRight.USER,
            addAssigneeTicket.getCustomerId()
        );
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnForbiddenWhenCuratorAndPublisherHaveDifferentCustomers()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var user = UserInstance.fromTicket(ticket);
        var completedTicket = ticket.updateAssignee(publication, new Username(user.getUsername()));
        var customer = randomUri();
        var request = createAssigneeTicketHttpRequest(completedTicket, AccessRight.APPROVE_DOI_REQUEST, customer);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnAcceptedWhenCuratorAttemptingToClaimAnAlreadyAssignedTicket()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var completedTicket = ticketService.updateTicketAssignee(ticket, new Username(user.getUsername()));
        var request = authorizedUserAssigneesTicket(publication, completedTicket, user);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }

    private InputStream authorizedUserAssigneesTicket(Publication publication,
                                                      TicketEntry ticket,
                                                      UserInstance user) throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
            .withPathParameters(pathParameters(publication, ticket))
            .withBody(TicketDto.fromTicket(ticket))
            .withUserName(user.getUsername())
            .withCurrentCustomer(user.getOrganizationUri())
            .withAccessRights(user.getOrganizationUri(), AccessRight.APPROVE_DOI_REQUEST.toString())
            .build();
    }

    private static Map<String, String> pathParameters(Publication publication,
                                                      TicketEntry ticket) {
        return Map.of(
            PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publication.getIdentifier().toString(),
            TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString()
        );
    }

    private InputStream createAssigneeTicketHttpRequest(TicketEntry ticket,
                                                        AccessRight accessRight,
                                                        URI customer) throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
            .withBody(TicketDto.fromTicket(ticket))
            .withAccessRights(customer, accessRight.toString())
            .withCurrentCustomer(customer)
            .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                                       ticket.extractPublicationIdentifier().toString(),
                                       TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                                       ticket.getIdentifier().toString()))
            .build();
    }
}
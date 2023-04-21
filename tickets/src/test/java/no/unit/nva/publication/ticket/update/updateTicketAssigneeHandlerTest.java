package no.unit.nva.publication.ticket.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.PublicationStatus;
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

class updateTicketAssigneeHandlerTest extends TicketTestLocal {

    private updateTicketAssigneeHandler handler;
    public static final String SOME_ASSIGNEE = "some@user";

    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new updateTicketAssigneeHandler(ticketService, resourceService, new FakeDoiClient());
    }

    @Test
    void shouldAddAssigneeToTicketWhenUserIsCuratorAndTicketHasNoAssignee()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        assertThat(publication.getDoi(), is(nullValue()));
        var ticket = createPersistedDoiTicket(publication);
        var user = UserInstance.fromTicket(ticket);
        var addAssigneeTicket = ticket.updateAssignee(publication, user.getUser());
        var request = authorizedUserAssigneesTicket(addAssigneeTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(addAssigneeTicket.getAssignee())));
    }

    @Test
    void shouldAddAssigneeDoiRequestWithoutChangingAlreadyExistingDoi()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublicationWithDoi();
        assertThat(publication.getDoi(), is(notNullValue()));
        var ticket = createPersistedDoiTicket(publication);
        var user = UserInstance.fromTicket(ticket);
        var addAssigneeTicket = ticket.updateAssignee(publication, user.getUser());
        var request = authorizedUserAssigneesTicket(addAssigneeTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(addAssigneeTicket.getAssignee())));
        var publicationInDatabase = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(publicationInDatabase.getDoi(), is(equalTo(publication.getDoi())));
    }

    @Test
    void shouldUpdateAssigneeToTicketWhenCuratorAttemptsToClaimTheTicket()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = createPersistedDoiTicket(publication);
        var assignee = UserInstance.create(SOME_ASSIGNEE, publication.getPublisher().getId());
        ticket.setAssignee(assignee.getUser());

        var user = UserInstance.fromTicket(ticket);
        var addAssigneeTicket = ticket.updateAssignee(publication, user.getUser());

        var request = authorizedUserAssigneesTicket(addAssigneeTicket);
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
        var addAssigneeTicket = ticket.updateAssignee(publication, user.getUser());
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
    void shouldReturnForbiddenWhenRequestingUserIsCuratorAtOtherCustomerThanCurrentPublisher()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var user = UserInstance.fromTicket(ticket);
        var completedTicket = ticket.updateAssignee(publication, user.getUser());
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
        var user = UserInstance.fromTicket(ticket);
        var completedTicket = ticketService.updateTicketAssignee(ticket, user.getUser());
        var request = authorizedUserAssigneesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }

    private InputStream authorizedUserAssigneesTicket(TicketEntry ticket) throws JsonProcessingException {
        return createAssigneeTicketHttpRequest(ticket, AccessRight.APPROVE_DOI_REQUEST, ticket.getCustomerId());
    }

    private InputStream createAssigneeTicketHttpRequest(TicketEntry ticket,
                                                        AccessRight accessRight,
                                                        URI customer) throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
            .withBody(TicketDto.fromTicket(ticket))
            .withAccessRights(customer, accessRight.toString())
            .withCustomerId(customer)
            .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                                       ticket.extractPublicationIdentifier().toString(),
                                       TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                                       ticket.getIdentifier().toString()))
            .build();
    }
}
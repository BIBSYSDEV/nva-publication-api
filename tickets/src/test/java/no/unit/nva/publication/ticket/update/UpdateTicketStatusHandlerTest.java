package no.unit.nva.publication.ticket.update;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.TicketConfig;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

@WireMockTest(httpsEnabled = true)
class UpdateTicketStatusHandlerTest extends TicketTestLocal {

    public static final Username USER_NAME = new Username(randomString());
    public static final Username ASSIGNEE = new Username("Assignee");
    private UpdateTicketStatusHandler handler;

    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new UpdateTicketStatusHandler(ticketService, resourceService, new FakeDoiClient());
    }

    @Test
    void shouldCompletePendingDoiRequestWhenUserIsCuratorAndPublicationIsPublished()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        assertThat(publication.getDoi(), is(nullValue()));
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getStatus(), is(equalTo(completedTicket.getStatus())));
        var modifiedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(modifiedPublication.getDoi(), is(notNullValue()));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as read for the publication owner when publication owner creates new ticket")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldSetAssigneeWhenCompletingTicketAndAssigneeIsMissing(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication,ticketType, ticketService);
        var completedTicket = ticket.complete(publication);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(USER_NAME)));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as read for the publication owner when publication owner creates new ticket")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldSetAssigneeWhenClosingTicketAndAssigneeIsMissing(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication,ticketType, ticketService);
        var closedTicket = ticket.close();
        var request = authorizedUserCompletesTicket(closedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(USER_NAME)));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as read for the publication owner when publication owner creates new ticket")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotSetAssigneeToTheOneWhoFinalizesTheTicketWhenAssigneeIsPresent(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication,ticketType, ticketService);
        var completedTicket = ticketService.updateTicketAssignee(ticket, ASSIGNEE).complete(publication);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(ASSIGNEE)));
    }

    @Test
    void shouldSetFinalizedValuesWhenCuratorCompletesTheTicketEntry()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getFinalizedBy(), is(equalTo(USER_NAME)));
        assertThat(actualTicket.getFinalizedDate(), is(notNullValue()));
    }

    @Test
    void shouldCompletePendingDoiRequestWithoutChangingAlreadyExistingDoi()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublicationWithDoi();
        assertThat(publication.getDoi(), is(notNullValue()));
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getStatus(), is(equalTo(completedTicket.getStatus())));
        var publicationInDatabase = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(publicationInDatabase.getDoi(), is(equalTo(publication.getDoi())));
    }

    @Test
    void shouldReturnErrorForDoiTicketCompletedForPublicationNotSatisfyingDoiRequirement()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        publication.getEntityDescription().setMainTitle(null);
        resourceService.updatePublication(
            publication.copy().withEntityDescription(publication.getEntityDescription()).build());
        var ticket = createPersistedDoiTicket(publication);
        ticket.setStatus(COMPLETED);
        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_METHOD)));
    }

    @Test
    void shouldReturnErrorForDoiTicketCompletedWhenDoiClientRespondsWithError()
        throws ApiGatewayException, IOException {
        this.handler = new UpdateTicketStatusHandler(ticketService, resourceService,
                                                     new FakeDoiClientThrowingException());
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_GATEWAY)));
    }

    @Test
    void shouldReturnErrorForDeleteDraftDoiTicketWhenDoiClientRespondsWithError()
        throws IOException, ApiGatewayException {
        this.handler = new UpdateTicketStatusHandler(ticketService, resourceService,
                                                     new FakeDoiClientThrowingException());
        var publication = TicketTestUtils.createPersistedPublicationWithDoi(
            PublicationStatus.PUBLISHED, resourceService);
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.close();
        mockBadResponseFromDoiRegistrar(publication.getDoi());
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
    }

    @Test
    void shouldDeleteDraftDoiAndUpdatePublicationSuccessfully()
        throws IOException, ApiGatewayException {
        this.handler = new UpdateTicketStatusHandler(ticketService, resourceService, new FakeDoiClient());
        var publication = TicketTestUtils.createPersistedPublicationWithDoi(PublicationStatus.PUBLISHED,
                                                                            resourceService);
        var ticket = TicketTestUtils.createClosedTicket(publication, DoiRequest.class, ticketService);
        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        assertThat(resourceService.getPublication(publication).getDoi(), is(nullValue()));
    }

    @Test
    void shouldReturnForbiddenWhenRequestingUserIsNotCurator() throws IOException, ApiGatewayException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
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
        var ticket = createPersistedDoiTicket(publication);
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
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticketService.updateTicketStatus(ticket, COMPLETED, USER_NAME);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }

    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should return a Bad Request response when attempting to re-open a ticket.")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnBadRequestWhenUserAttemptsToDeCompleteCompletedTicket(Class<? extends TicketEntry> ticketType,
                                                                           PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticketService.updateTicketStatus(ticket, COMPLETED, USER_NAME);
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
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnBadRequestWhenAttemptingToCompleteIncompletableTicketCases(Class<? extends TicketEntry> ticketType,
                                                                                PublicationStatus publicationStatus)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(publicationStatus, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        updatePublicationStatus(publication, publicationStatus);

        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }

    @Test
    void shouldReturnNotFoundWhenSupplyingMalformedTicketIdentifier()
        throws IOException {
        var request = authorizedUserInputMalformedIdentifier(SortableIdentifier.next().toString(), randomString());
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    private void mockBadResponseFromDoiRegistrar(URI doi) {
        stubFor(WireMock.get(urlPathEqualTo("/draft/" + doiPrefix(doi) + "/" + doiSuffix(doi)))
                    .willReturn(aResponse().withBody("[]").withStatus(HTTP_BAD_GATEWAY)));
    }

    private String doiSuffix(URI doi) {
        return doi.getPath().split("/")[2];
    }

    private String doiPrefix(URI doi) {
        return doi.getPath().split("/")[1];
    }

    private void updatePublicationStatus(Publication publication, PublicationStatus newPublicationStatus)
        throws ApiGatewayException {
        if (PublicationStatus.PUBLISHED.equals(newPublicationStatus)) {
            resourceService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        } else if (PublicationStatus.DRAFT_FOR_DELETION.equals(newPublicationStatus)) {
            resourceService.markPublicationForDeletion(UserInstance.fromPublication(publication),
                                                       publication.getIdentifier());
        }
    }

    private InputStream authorizedUserInputMalformedIdentifier(String publicationIdentifier, String ticketIdentifier)
        throws JsonProcessingException {
        URI customer = randomUri();
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(DoiRequestDto.empty())
                   .withAccessRights(customer, AccessRight.APPROVE_DOI_REQUEST.toString())
                   .withCurrentCustomer(customer)
                   .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                                              publicationIdentifier,
                                              TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME, ticketIdentifier))
                   .build();
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
                   .withCurrentCustomer(customer)
                   .withUserName(USER_NAME.getValue())
                   .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                                              ticket.extractPublicationIdentifier().toString(),
                                              TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                                              ticket.getIdentifier().toString()))
                   .build();
    }
}
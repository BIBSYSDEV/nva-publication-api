package no.unit.nva.publication.ticket.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.*;
import no.unit.nva.publication.testing.http.RandomPersonServiceResponse;
import no.unit.nva.publication.ticket.*;
import no.unit.nva.publication.ticket.model.UpdateTicketRequest;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.net.HttpURLConnection.*;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

@WireMockTest(httpsEnabled = true)
public class UpdateTicketHandlerTest extends TicketTestLocal {

    public static final String SOME_ASSIGNEE = "some@user";
    public static final Username USER_NAME = new Username(randomString());
    public static final Username ASSIGNEE = new Username("Assignee");
    public static final String TICKET_IDENTIFIER_PATH_PARAMETER = "ticketIdentifier";
    private UpdateTicketHandler handler;

    private static Map<String, String> pathParameters(Publication publication,
                                                      TicketEntry ticket) {
        return Map.of(
                PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publication.getIdentifier().toString(),
                TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString()
        );
    }

    private static void assertThatTicketViewStatusIsUnchanged(TicketEntry ticket) {
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(ticket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));
    }

    private static InputStream createOwnerMarksTicket(Publication publication,
                                                      TicketEntry ticket,
                                                      ViewStatus viewStatus)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper)
                .withCurrentCustomer(ticket.getCustomerId())
                .withUserName(ticket.getOwner().toString())
                .withBody(new UpdateTicketRequest(ticket.getStatus(), null, viewStatus))
                .withPathParameters(createPathParameters(ticket, publication.getIdentifier()))
                .build();
    }

    private static InputStream elevatedUserMarksTicket(Publication publication,
                                                       TicketEntry ticket,
                                                       ViewStatus viewStatus,
                                                       URI customerId)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper)
                .withCurrentCustomer(customerId)
                .withUserName(randomString())
                .withAccessRights(customerId, AccessRight.APPROVE_DOI_REQUEST.toString())
                .withBody(new UpdateTicketRequest(ticket.getStatus(), null, viewStatus))
                .withPathParameters(createPathParameters(ticket, publication.getIdentifier()))
                .build();
    }

    private static Map<String, String> createPathParameters(TicketEntry ticket,
                                                            SortableIdentifier publicationIdentifier) {
        return Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publicationIdentifier.toString(),
                      TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString());
    }

    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new UpdateTicketHandler(ticketService, resourceService, new FakeDoiClient());
    }

    @Test
    void shouldNotUpdateTicketWhenThereIsNoEffectiveChanges() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        ticket.setAssignee(new Username(user.getUsername()));
        ticketService.updateTicket(ticket);
        var request = authorizedUserAssigneesTicket(publication, ticket, user);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        assertThat(ticket, is(equalTo(ticketService.fetchTicket(ticket))));
    }

    @Test
    void shouldCompletePendingDoiRequestWhenUserIsCuratorAndPublicationIsPublished()
            throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        assertThat(publication.getDoi(), is(nullValue()));
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication, USER_NAME);
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
    @ArgumentsSource(TicketAndPublicationStatusProvider.class)
    void shouldSetAssigneeWhenCompletingTicketAndAssigneeIsMissing(
            Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var completedTicket = ticket.complete(publication, USER_NAME);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(USER_NAME)));
    }

    @ParameterizedTest
    @ArgumentsSource(TicketAndPublicationStatusProvider.class)
    void shouldSetAssigneeWhenClosingTicketAndAssigneeIsMissing(
            Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var closedTicket = ticket.close(USER_NAME);
        var request = authorizedUserCompletesTicket(closedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(USER_NAME)));
    }

    @ParameterizedTest
    @ArgumentsSource(TicketAndPublicationStatusProvider.class)
    void shouldNotSetAssigneeToTheOneWhoFinalizesTheTicketWhenAssigneeIsPresent(
            Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var completedTicket = ticketService.updateTicketAssignee(ticket, ASSIGNEE).complete(publication, USER_NAME);
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
        var completedTicket = ticket.complete(publication, USER_NAME);
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
        var completedTicket = ticket.complete(publication, USER_NAME);
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
        this.handler = new UpdateTicketHandler(ticketService, resourceService,
                new FakeDoiClientThrowingException());
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication, USER_NAME);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_GATEWAY)));
    }

    @Test
    void shouldReturnErrorForDeleteDraftDoiTicketWhenDoiClientRespondsWithError()
            throws IOException, ApiGatewayException {
        this.handler = new UpdateTicketHandler(ticketService, resourceService,
                new FakeDoiClientThrowingException());
        var publication = TicketTestUtils.createPersistedPublicationWithDoi(
                PublicationStatus.PUBLISHED, resourceService);
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.close(USER_NAME);
        mockBadResponseFromDoiRegistrar(publication.getDoi());
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
    }

    @Test
    void shouldDeleteDraftDoiAndUpdatePublicationSuccessfully()
            throws IOException, ApiGatewayException {
        this.handler = new UpdateTicketHandler(ticketService, resourceService, new FakeDoiClient());
        var publication = TicketTestUtils.createPersistedPublicationWithDoi(PublicationStatus.PUBLISHED,
                resourceService);
        var ticket = TicketTestUtils.createClosedTicket(publication, DoiRequest.class, ticketService);
        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        assertThat(resourceService.getPublication(publication).getDoi(), is(nullValue()));
    }

    @Test
    void shouldReturnHttpForbiddenWhenRequestingUserIsNotCurator() throws IOException, ApiGatewayException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication, USER_NAME);
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
        var completedTicket = ticket.complete(publication, USER_NAME);
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
    void shouldReturnBadRequestWhenUserAttemptsToReopenCompletedTicket(Class<? extends TicketEntry> ticketType,
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

    @Test
    void shouldReturnNotFoundWhenSupplyingMalformedTicketIdentifier()
            throws IOException {
        var request = authorizedUserInputMalformedIdentifier(SortableIdentifier.next().toString(), randomString());
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }

    @Test
    void shouldAddAssigneeToTicketWhenUserIsCuratorAndTicketHasNoAssignee() throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        assertThat(publication.getDoi(), is(nullValue()));
        var ticket = createPersistedDoiTicket(publication);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var request = authorizedUserAssigneesTicket(publication, ticket, user);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(ticket.getAssignee())));
    }

    @Test
    void shouldUpdateAssigneeToTicketWhenCuratorAttemptsToClaimTheTicket()
            throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = createPersistedDoiTicket(publication);
        var assignee = UserInstance.create(SOME_ASSIGNEE, publication.getPublisher().getId());
        ticket.setAssignee(new Username(assignee.getUsername()));
        ticketService.updateTicket(ticket);

        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var addAssigneeTicket = ticket.updateAssignee(publication, new Username(assignee.getUsername()));

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
                addAssigneeTicket.getCustomerId()
        );
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnAcceptedWhenCuratorAttemptingToClaimAnAlreadyAssignedTicket()
            throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        ticket.setAssignee(USER_NAME);
        ticketService.updateTicket(ticket);
        var newAssignee = UserInstance.create(randomString(), publication.getPublisher().getId());
        var updatedTicket = ticket.updateAssignee(publication, new Username(newAssignee.getUsername()));
        var request = authorizedUserAssigneesTicket(publication, updatedTicket, newAssignee);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as read for owner when user is ticket owner and marks it as read")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsReadFoOwnerWhenUserIsTicketOwnerAndMarksItAsRead(Class<? extends TicketEntry> ticketType,
                                                                            PublicationStatus status)
            throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.markUnreadByOwner().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), not(hasItem(ticket.getOwner())));

        var httpRequest = createOwnerMarksTicket(publication, ticket, ViewStatus.READ);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));

        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasItem(ticket.getOwner()));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as unread for owner when user is ticket owner and marks it as unread")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForOwnerWhenUserIsTicketOwnerAndMarksItAsUnread(
            Class<? extends TicketEntry> ticketType, PublicationStatus status)
            throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));

        var httpRequest = createOwnerMarksTicket(publication, ticket, ViewStatus.UNREAD);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));

        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), not(hasItem(ticket.getOwner())));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as Read for all Curators when user is curator and marks it as read")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsReadForAllCuratorsWhenUserIsCuratorAndMarksItAsRead(
            Class<? extends TicketEntry> ticketType, PublicationStatus status)
            throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(ticket.getViewedBy(), not(hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT)));

        var httpRequest = curatorMarksTicket(publication, ticket, ViewStatus.READ);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));

        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(updatedTicket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as Unread for all Curators when user is curator and marks it as unread")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForAllCuratorsWhenUserIsCuratorAndMarksItAsUnread(
            Class<? extends TicketEntry> ticketType, PublicationStatus status)
            throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.markReadForCurators().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(ticket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));

        var httpRequest = curatorMarksTicket(publication, ticket, ViewStatus.UNREAD);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));

        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(updatedTicket.getViewedBy(), not(hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotProvideAnyInformationAboutTheExistenceOfATicketWhenAnAlienUserTriesToModifyTicket(Class<? extends TicketEntry> ticketType, PublicationStatus status)
            throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.markReadForCurators().persistUpdate(ticketService);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(ticket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));

        var httpRequest = alienCuratorMarksTicket(publication, ticket);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
        assertThatTicketViewStatusIsUnchanged(ticket);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenWhenTicketIdIsWrongWhenUserIsCurator(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        SortableIdentifier wrongPublicationIdentifier = SortableIdentifier.next();
        var httpRequest = curatorAttemptsToMarkExistingTicketConnectedToWrongPublication(ticket,
                wrongPublicationIdentifier);
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenWhenTicketIdIsWrongWhenUserIsOwner(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        SortableIdentifier wrongPublicationIdentifier = SortableIdentifier.next();
        var httpRequest = ownerAttemptsToMarkExistingTicketConnectedToWrongPublication(ticket,
                wrongPublicationIdentifier);
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenWhenTicketDoesNotExist(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createNonPersistedTicket(publication, ticketType);
        var httpRequest = ownerAttemptsToMarkExistingTicketConnectedToWrongPublication(ticket,
                publication.getIdentifier());
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
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

    private InputStream createAssigneeTicketHttpRequest(TicketEntry ticket,
                                                        URI customer) throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                .withBody(TicketDto.fromTicket(ticket))
                .withAccessRights(customer, AccessRight.USER.toString())
                .withCurrentCustomer(customer)
                .withUserName(USER_NAME.getValue())
                .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                        ticket.extractPublicationIdentifier().toString(),
                        TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                        ticket.getIdentifier().toString()))
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

    private InputStream curatorAttemptsToMarkExistingTicketConnectedToWrongPublication(
            TicketEntry ticket,
            SortableIdentifier wrongPublicationIdentifier)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper)
                .withCurrentCustomer(ticket.getCustomerId())
                .withUserName(randomString())
                .withBody(new UpdateTicketRequest(ticket.getStatus(), null, ViewStatus.UNREAD))
                .withPathParameters(createPathParameters(ticket, wrongPublicationIdentifier))
                .withAccessRights(ticket.getCustomerId(), AccessRight.APPROVE_DOI_REQUEST.toString())
                .build();
    }

    private InputStream curatorMarksTicket(Publication publication, TicketEntry ticket, ViewStatus viewStatus)
            throws JsonProcessingException {
        return elevatedUserMarksTicket(publication, ticket, viewStatus, ticket.getCustomerId());
    }

    private InputStream alienCuratorMarksTicket(Publication publication, TicketEntry ticket)
            throws JsonProcessingException {
        return elevatedUserMarksTicket(publication, ticket, ViewStatus.UNREAD, RandomPersonServiceResponse.randomUri());
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

    private InputStream ownerAttemptsToMarkExistingTicketConnectedToWrongPublication(
            TicketEntry ticket, SortableIdentifier wrongPublicationIdentifier)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper)
                .withBody(new UpdateTicketRequest(ticket.getStatus(), null, ViewStatus.UNREAD))
                .withCurrentCustomer(ticket.getCustomerId())
                .withUserName(ticket.getOwner().toString())
                .withPathParameters(createPathParameters(ticket, wrongPublicationIdentifier))
                .build();
    }
}

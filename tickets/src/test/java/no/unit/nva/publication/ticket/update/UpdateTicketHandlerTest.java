package no.unit.nva.publication.ticket.update;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithId;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.mock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.ticket.TicketPermissions;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.TicketAndPublicationStatusProvider;
import no.unit.nva.publication.ticket.TicketConfig;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.UpdateTicketOwnershipRequest;
import no.unit.nva.publication.ticket.UpdateTicketRequest;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

@WireMockTest(httpsEnabled = true)
public class UpdateTicketHandlerTest extends TicketTestLocal {

    public static final String SOME_ASSIGNEE = "some@user";
    public static final UserInstance USER_INSTANCE = UserInstance.create(randomString(), randomUri());
    public static final Username ASSIGNEE = new Username("Assignee");
    public static final String TICKET_IDENTIFIER_PATH_PARAMETER = "ticketIdentifier";
    private UpdateTicketHandler handler;

    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new UpdateTicketHandler(ticketService, resourceService, new FakeDoiClient(), new Environment());
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
        var completedTicket = ticket.complete(publication, USER_INSTANCE);
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
    void shouldSetAssigneeWhenCompletingTicketAndAssigneeIsMissing(Class<? extends TicketEntry> ticketType,
                                                                   PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var completedTicket = ticket.complete(publication, USER_INSTANCE);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(new Username(USER_INSTANCE.getUsername()))));
    }

    @ParameterizedTest
    @ArgumentsSource(TicketAndPublicationStatusProvider.class)
    void shouldSetAssigneeWhenClosingTicketAndAssigneeIsMissing(Class<? extends TicketEntry> ticketType,
                                                                PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var closedTicket = ticket.close(USER_INSTANCE);
        var request = authorizedUserCompletesTicket(closedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(new Username(USER_INSTANCE.getUsername()))));
    }

    @ParameterizedTest
    @ArgumentsSource(TicketAndPublicationStatusProvider.class)
    void shouldNotSetAssigneeToTheOneWhoFinalizesTheTicketWhenAssigneeIsPresent(Class<? extends TicketEntry> ticketType,
                                                                                PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var completedTicket = ticketService.updateTicketAssignee(ticket, ASSIGNEE).complete(publication, USER_INSTANCE);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getAssignee(), is(equalTo(ASSIGNEE)));
    }

    @Test
    void shouldSetFinalizedValuesWhenCuratorCompletesTheTicketEntry() throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication, USER_INSTANCE);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getFinalizedBy().getValue(), is(equalTo(USER_INSTANCE.getUsername())));
        assertThat(actualTicket.getFinalizedDate(), is(notNullValue()));
    }

    @Test
    void shouldCompletePendingDoiRequestWithoutChangingAlreadyExistingDoi() throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublicationWithDoi();
        assertThat(publication.getDoi(), is(notNullValue()));
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication, USER_INSTANCE);
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
        this.handler = new UpdateTicketHandler(ticketService,
                                               resourceService,
                                               new FakeDoiClientThrowingException(), new Environment());
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication, USER_INSTANCE);
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_GATEWAY)));
    }

    @Test
    void shouldReturnErrorForDeleteDraftDoiTicketWhenDoiClientRespondsWithError()
        throws IOException, ApiGatewayException {
        this.handler = new UpdateTicketHandler(ticketService,
                                               resourceService,
                                               new FakeDoiClientThrowingException(), new Environment());
        var publication = TicketTestUtils.createPersistedPublicationWithDoi(PublicationStatus.PUBLISHED,
                                                                            resourceService);
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.close(USER_INSTANCE);
        mockBadResponseFromDoiRegistrar(publication.getDoi());
        var request = authorizedUserCompletesTicket(completedTicket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
    }

    @Test
    void shouldDeleteDraftDoiAndUpdatePublicationSuccessfully() throws IOException, ApiGatewayException {
        this.handler = new UpdateTicketHandler(ticketService, resourceService, new FakeDoiClient(), new Environment());
        var publication = TicketTestUtils.createPersistedPublicationWithDoi(PublicationStatus.PUBLISHED,
                                                                            resourceService);
        var ticket = TicketTestUtils.createClosedTicket(publication, DoiRequest.class, ticketService);
        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        assertThat(resourceService.getPublicationByIdentifier(publication.getIdentifier()).getDoi(), is(nullValue()));
    }

    @Test
    void shouldReturnHttpForbiddenWhenRequestingUserIsNotCurator() throws IOException, ApiGatewayException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticket.complete(publication, USER_INSTANCE);
        var request = createCompleteTicketHttpRequest(completedTicket,
                                                      completedTicket.getCustomerId());
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnForbiddenWhenRequestingUserBelongsToOtherTopLevelOrgThenTicketOwnerAffiliation()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiRequestWithOwnerAffiliation(publication, randomUri());
        var completedTicket = ticket.complete(publication, USER_INSTANCE);
        var customer = randomUri();
        var request = createCompleteTicketHttpRequest(completedTicket, customer, AccessRight.MANAGE_DOI);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnAcceptedWhenCompletingAnAlreadyCompletedDoiRequestTicket()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        var completedTicket = ticketService.updateTicketStatus(ticket, COMPLETED, USER_INSTANCE);
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
        ticketService.updateTicketStatus(ticket, COMPLETED, USER_INSTANCE);
        ticket.setStatus(TicketStatus.PENDING);
        var request = authorizedUserCompletesTicket(ticket);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var actualTicket = ticketService.fetchTicket(ticket);
        assertThat(actualTicket.getStatus(), is(equalTo(COMPLETED)));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }

    @Test
    void shouldReturnForbiddenWhenSupplyingMalformedTicketIdentifier() throws IOException {
        var request = authorizedUserInputMalformedIdentifier(SortableIdentifier.next().toString(), randomString());
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
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
    void shouldUpdateAssigneeToTicketWhenCuratorAttemptsToClaimTheTicket() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = createPersistedDoiTicket(publication);
        var assignee = UserInstance.create(SOME_ASSIGNEE, publication.getPublisher().getId());
        ticket.setAssignee(new Username(assignee.getUsername()));
        ticketService.updateTicket(ticket);

        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var addAssigneeTicket = ticket.updateAssignee(new Username(assignee.getUsername()));

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
        var addAssigneeTicket = ticket.updateAssignee(new Username(user.getUsername()));
        var request = createAssigneeTicketHttpRequest(addAssigneeTicket, addAssigneeTicket.getCustomerId());
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnAcceptedWhenCuratorAttemptingToClaimAnAlreadyAssignedTicket()
        throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        ticket.setAssignee(new Username(USER_INSTANCE.getUsername()));
        ticketService.updateTicket(ticket);
        var newAssignee = UserInstance.create(randomString(), publication.getPublisher().getId());
        var updatedTicket = ticket.updateAssignee(new Username(newAssignee.getUsername()));
        var request = authorizedUserAssigneesTicket(publication, updatedTicket, newAssignee);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }

    @Test
    void shouldResetAssigneeWhenAssigneeFromRequestIsEmptyString() throws ApiGatewayException, IOException {
        var publication = createPersistAndPublishPublication();
        var ticket = createPersistedDoiTicket(publication);
        ticket.setAssignee(new Username(USER_INSTANCE.getUsername()));
        ticketService.updateTicket(ticket);
        var newAssignee = UserInstance.create(randomString(), publication.getPublisher().getId());
        var ticketWithNoAssignee = ticket.updateAssignee(new Username(""));
        var request = authorizedUserAssigneesTicket(publication, ticketWithNoAssignee, newAssignee);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(ticketService.fetchTicket(ticket).getAssignee(), is(nullValue()));
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
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.markReadByOwner().persistUpdate(ticketService);
        var httpRequest = createOwnerMarksTicket(publication, ticket, ViewStatus.UNREAD);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));

        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), not(hasItem(ticket.getOwner())));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as Read for assignee when user is curator assignee and marks it as read")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsReadForAssigneesWhenUserIsCuratorAndAssigneeAndMarksItAsRead(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = setupPersistedTicketWithAssignee(ticketType, publication);

        var httpRequest = assigneeMarksTicket(publication, ticket, ViewStatus.READ);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));

        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasItem(new User(ticket.getAssignee().toString())));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsReadWhenCuratorOpensNotViewedTicket(Class<? extends TicketEntry> ticketType,
                                                               PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = setupUnassignedPersistedTicket(ticketType, publication);
        var curator = new User(randomString());

        var httpRequest = curatorMarksTicket(publication, ticket, curator);
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        var updatedTicket = ticketService.fetchTicket(ticket);
        var viewedByList = updatedTicket.getViewedBy().stream().toList();

        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
        assertThat(viewedByList.size(), is(equalTo(1)));
        assertThat(viewedByList, hasItem(curator));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as Unread for assignee when user is curator and assignee and and marks it as "
                 + "unread")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForAssigneesWhenUserIsCuratorAndAssigneeAndMarksItAsUnread(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = setupPersistedTicketWithAssignee(ticketType, publication);
        ticket.markReadForAssignee().persistUpdate(ticketService);
        var expectedAssigneeUsername = new User(ticket.getAssignee().toString());

        assertThat(ticket.getViewedBy(), hasItem(expectedAssigneeUsername));

        var httpRequest = assigneeMarksTicket(publication, ticket, ViewStatus.UNREAD);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));

        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), not(hasItem(expectedAssigneeUsername)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenWhenTicketIdIsWrongWhenUserIsCurator(Class<? extends TicketEntry> ticketType,
                                                                   PublicationStatus status)
        throws ApiGatewayException, IOException {
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
    void shouldReturnForbiddenWhenTicketIdIsWrongWhenUserIsOwner(Class<? extends TicketEntry> ticketType,
                                                                 PublicationStatus status)
        throws ApiGatewayException, IOException {
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
    void shouldReturnForbiddenWhenTicketDoesNotExist(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createNonPersistedTicket(publication, ticketType);
        var httpRequest = ownerAttemptsToMarkExistingTicketConnectedToWrongPublication(ticket,
                                                                                       publication.getIdentifier());
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldSetPendingFilesAsApprovedFilesForPublishingRequestWhenUpdatingStatusToComplete()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(
            PublicationStatus.DRAFT, resourceService);
        var ticket = persistPublishingRequestContainingExistingPendingOpenFiles(publication);
        var closedTicket = ticket.complete(publication, USER_INSTANCE);
        var httpRequest = createCompleteTicketHttpRequest(closedTicket,
                                                          ticket.getCustomerId(),
                                                          MANAGE_RESOURCES_STANDARD,
                                                          MANAGE_PUBLISHING_REQUESTS);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var completedPublishingRequest = (PublishingRequestCase) ticketService.fetchTicket(ticket);
        var approvedFile = (File) publication.getAssociatedArtifacts().getFirst();

        var approvedFiles = completedPublishingRequest.getApprovedFiles().stream()
                                .map(File::getIdentifier)
                                .collect(Collectors.toSet());
        assertThat(approvedFiles, hasItem(approvedFile.getIdentifier()));
    }

    @Test
    void shouldSetPendingFilesAsApprovedFilesForFilesApprovalThesisWhenUpdatingStatusToComplete()
        throws ApiGatewayException, IOException {
        var publication = randomPublication(DegreeBachelor.class).copy()
                              .withAssociatedArtifacts(List.of(randomPendingOpenFile()))
                              .build();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(resourceService, UserInstance.fromPublication(publication));
        var ticket = persistFilesApprovalThesisContainingExistingPendingOpenFiles(persistedPublication);
        var closedTicket = ticket.complete(persistedPublication, USER_INSTANCE);
        var httpRequest = createCompleteTicketHttpRequest(closedTicket,
                                                          ticket.getCustomerId(),
                                                          MANAGE_RESOURCES_STANDARD,
                                                          MANAGE_DEGREE);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var completedPublishingRequest = (FilesApprovalThesis) ticketService.fetchTicket(ticket);
        var approvedFile = (File) persistedPublication.getAssociatedArtifacts().getFirst();

        var approvedFiles = completedPublishingRequest.getApprovedFiles().stream()
                                .map(File::getIdentifier)
                                .collect(Collectors.toSet());
        assertThat(approvedFiles, hasItem(approvedFile.getIdentifier()));
    }

    @Test
    void shouldEmptyFilesForApprovalWhenPublishingRequestIsBeingApproved()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(
            PublicationStatus.DRAFT, resourceService);
        var ticket = persistPublishingRequestContainingExistingPendingOpenFiles(publication);
        var expectedFilesForApproval = publication.getAssociatedArtifacts().stream()
                                           .filter(PendingFile.class::isInstance)
                                           .map(PendingFile.class::cast)
                                           .map(File.class::cast)
                                           .map(File::getIdentifier)
                                           .toArray(UUID[]::new);

        assertThat(((PublishingRequestCase) ticket).getFilesForApproval().stream().map(File::getIdentifier).toList(),
                   containsInAnyOrder(expectedFilesForApproval));

        var completedTicket = ticket.complete(publication, USER_INSTANCE);
        var httpRequest = createCompleteTicketHttpRequest(completedTicket,
                                                          ticket.getCustomerId(),
                                                          MANAGE_RESOURCES_STANDARD,
                                                          MANAGE_PUBLISHING_REQUESTS);
        handler.handleRequest(httpRequest, output, CONTEXT);
        var completedPublishingRequest = (PublishingRequestCase) ticketService.fetchTicket(ticket);
        var approvedFile = (File) publication.getAssociatedArtifacts().getFirst();

        var approvedFiles = completedPublishingRequest.getApprovedFiles().stream()
                                .map(File::getIdentifier)
                                .toList();
        assertThat(approvedFiles, hasItem(approvedFile.getIdentifier()));
        assertThat(completedPublishingRequest.getFilesForApproval(), is(emptyIterable()));
    }

    @Test
    void contributorAtAnotherInstitutionThanPublicationOwnerShouldBeAbleToUpdateTicketForPublication()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(
            PublicationStatus.PUBLISHED, resourceService);
        var contributorId = PublicationGenerator.randomUri();
        publication.getEntityDescription().setContributors(List.of(randomContributorWithId(contributorId)));
        resourceService.updatePublication(publication);
        var ticket = TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);

        var completedTicket = ticket.complete(publication, USER_INSTANCE);
        var httpRequest = userUpdatesTicket(completedTicket, contributorId);
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        Assertions.assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    @Test
    void curatorAtAnotherInstitutionThanPublicationOwnerShouldBeAbleToUpdateTicketForPublication()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(
            PublicationStatus.PUBLISHED, resourceService);
        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of())));
        resourceService.updatePublication(publication);
        var ticket = TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);

        var completedTicket = ticket.complete(publication, USER_INSTANCE);
        var httpRequest = userUpdatesTicket(completedTicket, randomUri());
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        Assertions.assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    @Test
    void shouldReturnConflictWhenApprovingPublishingRequestContainingFilesThatAreMissingMandatoryFields()
        throws ApiGatewayException, IOException {
        var fileWithoutLicense = pendingFileWithoutLicense();
        var publication =
            randomPublication().copy().withAssociatedArtifacts(List.of(fileWithoutLicense)).build();
        var persistedPublication = resourceService.createPublication(UserInstance.fromPublication(publication),
                                                                     publication);
        var ticket = persistPublishingRequestContainingExistingPendingOpenFiles(persistedPublication);
        var closedTicket = ticket.complete(persistedPublication, USER_INSTANCE);
        var httpRequest = createCompleteTicketHttpRequest(closedTicket,
                                                          ticket.getCustomerId(),
                                                          MANAGE_RESOURCES_STANDARD,
                                                          MANAGE_PUBLISHING_REQUESTS);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
    }

    @Test
    void shouldReturnConflictWhenApprovingPublishingRequestContainingFileThatIsMissingMandatoryFieldsAndValidFile()
        throws ApiGatewayException, IOException {
        var publication =
            randomPublication().copy().withAssociatedArtifacts(
                List.of(pendingFileWithoutLicense(), randomPendingOpenFile())).build();
        var persistedPublication = resourceService.createPublication(UserInstance.fromPublication(publication),
                                                                     publication);
        var ticket = persistPublishingRequestContainingExistingPendingOpenFiles(persistedPublication);
        var closedTicket = ticket.complete(persistedPublication, USER_INSTANCE);
        var httpRequest = createCompleteTicketHttpRequest(closedTicket,
                                                          ticket.getCustomerId(),
                                                          MANAGE_RESOURCES_STANDARD,
                                                          MANAGE_PUBLISHING_REQUESTS);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
    }

    @Test
    void shouldUpdateTicketOwnershipToOneOfCuratingInstitutionsWhenUserInstitutionOwnsTicket()
        throws ApiGatewayException, IOException {
        var curatingInstitution = randomUri();
        var publication =
            randomPublication().copy()
                .withCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of())))
                .build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = resourceService.createPublication(userInstance, publication);
        var ticket = GeneralSupportRequest.create(Resource.fromPublication(persistedPublication), userInstance)
                         .persistNewTicket(ticketService);
        var httpRequest = createUpdateTicketOwnershipRequest(ticket, curatingInstitution, SUPPORT);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_ACCEPTED)));
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingTicketOwnershipToNotOneOfCuratingInstitutions()
        throws ApiGatewayException, IOException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = resourceService.createPublication(userInstance, publication);
        var ticket = GeneralSupportRequest.create(Resource.fromPublication(persistedPublication), userInstance)
                         .persistNewTicket(ticketService);
        var httpRequest = createUpdateTicketOwnershipRequest(ticket, randomUri(), SUPPORT);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingTicketOwnershipAndUserInstitutionDoesNotOwnTicket()
        throws ApiGatewayException, IOException {
        var curatingInstitution = randomUri();
        var publication =
            randomPublication().copy()
                .withCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of())))
                .build();
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = resourceService.createPublication(userInstance, publication);
        var ticket = GeneralSupportRequest.create(Resource.fromPublication(persistedPublication), userInstance)
                         .persistNewTicket(ticketService);
        var httpRequest = createUpdateTicketOwnershipRequestWithRandomInstitution(ticket, curatingInstitution, SUPPORT);
        handler.handleRequest(httpRequest, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    private InputStream createUpdateTicketOwnershipRequestWithRandomInstitution(TicketEntry ticket,
                                                                                URI newOwnerAffiliation,
                                                                                AccessRight accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketOwnershipRequest>(JsonUtils.dtoObjectMapper).withBody(
                new UpdateTicketOwnershipRequest(newOwnerAffiliation, newOwnerAffiliation))
                   .withCurrentCustomer(randomUri())
                   .withTopLevelCristinOrgId(randomUri())
                   .withAccessRights(randomUri(), accessRight)
                   .withUserName(randomString())
                   .withPathParameters(createPathParameters(ticket, ticket.getResourceIdentifier()))
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream createUpdateTicketOwnershipRequest(TicketEntry ticket, URI newOwnerAffiliation,
                                                           AccessRight accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketOwnershipRequest>(JsonUtils.dtoObjectMapper).withBody(
                new UpdateTicketOwnershipRequest(newOwnerAffiliation, newOwnerAffiliation))
                   .withCurrentCustomer(ticket.getCustomerId())
                   .withTopLevelCristinOrgId(ticket.getOwnerAffiliation())
                   .withAccessRights(ticket.getOwnerAffiliation(), accessRight)
                   .withUserName(randomString())
                   .withPathParameters(createPathParameters(ticket, ticket.getResourceIdentifier()))
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private AssociatedArtifact pendingFileWithoutLicense() {
        return randomPendingOpenFile().copy().withLicense(null).buildPendingOpenFile();
    }

    private InputStream userUpdatesTicket(TicketEntry ticket, URI userId) throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper).withBody(
                new UpdateTicketRequest(null, null, ViewStatus.READ))
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .withPathParameters(createPathParameters(ticket, ticket.getResourceIdentifier()))
                   .withPersonCristinId(userId)
                   .build();
    }

    private static Map<String, String> pathParameters(Publication publication, TicketEntry ticket) {
        return Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publication.getIdentifier().toString(),
                      TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString());
    }

    private static InputStream createOwnerMarksTicket(Publication publication, TicketEntry ticket,
                                                      ViewStatus viewStatus) throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper).withCurrentCustomer(
                ticket.getCustomerId())
                   .withUserName(ticket.getOwner().toString())
                   .withBody(new UpdateTicketRequest(ticket.getStatus(), null, viewStatus))
                   .withPathParameters(createPathParameters(ticket, publication.getIdentifier()))
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private static InputStream elevatedUserMarksTicket(Publication publication, TicketEntry ticket,
                                                       URI customerId, User curator)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper).withCurrentCustomer(customerId)
                   .withUserName(curator.toString())
                   .withAccessRights(customerId, AccessRight.MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                   .withBody(new UpdateTicketRequest(ticket.getStatus(), null, ViewStatus.READ))
                   .withPathParameters(createPathParameters(ticket, publication.getIdentifier()))
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private static InputStream assigneeMarksTicket(Publication publication, TicketEntry ticket, ViewStatus viewStatus,
                                                   URI customerId) throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper).withCurrentCustomer(customerId)
                   .withUserName(ticket.getAssignee().toString())
                   .withAccessRights(customerId,
                                     MANAGE_RESOURCES_STANDARD,
                                     AccessRight.MANAGE_DOI,
                                     MANAGE_PUBLISHING_REQUESTS,
                                     AccessRight.SUPPORT)
                   .withBody(new UpdateTicketRequest(ticket.getStatus(), ticket.getAssignee(), viewStatus))
                   .withPathParameters(createPathParameters(ticket, publication.getIdentifier()))
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream assigneeMarksTicket(Publication publication, TicketEntry ticket, ViewStatus viewStatus)
        throws JsonProcessingException {
        return assigneeMarksTicket(publication, ticket, viewStatus, ticket.getCustomerId());
    }

    private static Map<String, String> createPathParameters(TicketEntry ticket,
                                                            SortableIdentifier publicationIdentifier) {
        return Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publicationIdentifier.toString(),
                      TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString());
    }

    private TicketEntry setupUnassignedPersistedTicket(Class<? extends TicketEntry> ticketType, Publication publication)
        throws ApiGatewayException {
        return TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService)
                   .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation());
    }

    private TicketEntry setupPersistedTicketWithAssignee(Class<? extends TicketEntry> ticketType,
                                                         Publication publication) throws ApiGatewayException {
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var assigneeUsername = new Username(randomString());
        ticket.setAssignee(assigneeUsername);
        ticket.persistUpdate(ticketService);
        return ticket;
    }

    private InputStream authorizedUserAssigneesTicket(Publication publication, TicketEntry ticket, UserInstance user)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper).withPathParameters(
                pathParameters(publication, ticket))
                   .withBody(new UpdateTicketRequest(ticket.getStatus(), ticket.getAssignee(), null))
                   .withUserName(user.getUsername())
                   .withCurrentCustomer(user.getCustomerId())
                   .withAccessRights(user.getCustomerId(), AccessRight.MANAGE_DOI)
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .build();
    }

    private InputStream createAssigneeTicketHttpRequest(TicketEntry ticket, URI customer)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper).withBody(TicketDto.fromTicket(ticket,
                                                                                                             Collections.emptyList(),
                                                                                                             Collections.emptyList(),
                                                                                                             mock(
                                                                                                                 TicketPermissions.class)))
                   .withCurrentCustomer(customer)
                   .withUserName(USER_INSTANCE.getUsername())
                   .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                                              ticket.getResourceIdentifier().toString(),
                                              TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                                              ticket.getIdentifier().toString()))
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream authorizedUserCompletesTicket(TicketEntry ticket) throws JsonProcessingException {
        return createCompleteTicketHttpRequest(ticket, ticket.getCustomerId(), MANAGE_RESOURCES_STANDARD,
                                               AccessRight.MANAGE_DOI,
                                               MANAGE_PUBLISHING_REQUESTS, AccessRight.SUPPORT);
    }

    private TicketEntry persistPublishingRequestContainingExistingPendingOpenFiles(Publication publication)
        throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createNewTicket(publication,
                                                                                              PublishingRequestCase.class,
                                                                                              SortableIdentifier::next)
                                                            .withOwner(
                                                                UserInstance.fromPublication(publication).getUsername())
                                                            .withOwnerAffiliation(
                                                                publication.getResourceOwner().getOwnerAffiliation());
        publishingRequest.withFilesForApproval(getPendingFiles(publication));
        return publishingRequest.persistNewTicket(ticketService);
    }

    private TicketEntry persistFilesApprovalThesisContainingExistingPendingOpenFiles(Publication publication)
        throws ApiGatewayException {
        return FilesApprovalThesis.createForUserInstitution(Resource.fromPublication(publication),
                                                            UserInstance.fromPublication(publication),
                                                            PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
                   .persistNewTicket(ticketService);
    }

    private Set<File> getPendingFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(PendingFile.class::isInstance)
                   .map(File.class::cast)
                   .collect(Collectors.toSet());
    }

    private InputStream createCompleteTicketHttpRequest(TicketEntry ticket, URI customer, AccessRight... accessRights)
        throws JsonProcessingException {
        var publication = ticket.toPublication(resourceService);
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper).withBody(
                TicketDto.fromTicket(ticket, Collections.emptyList(), Collections.emptyList(),
                                     mock(TicketPermissions.class)))
                   .withAccessRights(customer, accessRights)
                   .withCurrentCustomer(customer)
                   .withUserName(USER_INSTANCE.getUsername())
                   .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                                              ticket.getResourceIdentifier().toString(),
                                              TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                                              ticket.getIdentifier().toString()))
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .build();
    }

    private InputStream authorizedUserInputMalformedIdentifier(String publicationIdentifier, String ticketIdentifier)
        throws JsonProcessingException {
        URI customer = randomUri();
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper).withBody(DoiRequestDto.empty())
                   .withAccessRights(customer, AccessRight.MANAGE_DOI)
                   .withCurrentCustomer(customer)
                   .withPathParameters(Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME,
                                              publicationIdentifier, TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME,
                                              ticketIdentifier))
                   .withUserName(randomString())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream curatorAttemptsToMarkExistingTicketConnectedToWrongPublication(
        TicketEntry ticket, SortableIdentifier wrongPublicationIdentifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper).withCurrentCustomer(
                ticket.getCustomerId())
                   .withUserName(randomString())
                   .withBody(new UpdateTicketRequest(ticket.getStatus(), null, ViewStatus.UNREAD))
                   .withPathParameters(createPathParameters(ticket, wrongPublicationIdentifier))
                   .withAccessRights(ticket.getCustomerId(), AccessRight.MANAGE_DOI)
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream curatorMarksTicket(Publication publication, TicketEntry ticket,
                                           User curator) throws JsonProcessingException {
        return elevatedUserMarksTicket(publication, ticket, ticket.getCustomerId(), curator);
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
        TicketEntry ticket, SortableIdentifier wrongPublicationIdentifier) throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateTicketRequest>(JsonUtils.dtoObjectMapper).withBody(
                new UpdateTicketRequest(ticket.getStatus(), null, ViewStatus.UNREAD))
                   .withCurrentCustomer(ticket.getCustomerId())
                   .withUserName(ticket.getOwner().toString())
                   .withPathParameters(createPathParameters(ticket, wrongPublicationIdentifier))
                   .withPersonCristinId(randomUri())
                   .build();
    }
}

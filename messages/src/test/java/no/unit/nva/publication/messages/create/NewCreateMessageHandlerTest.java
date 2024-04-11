package no.unit.nva.publication.messages.create;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.publication.utils.RequestUtils.TICKET_IDENTIFIER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class NewCreateMessageHandlerTest extends ResourcesLocalTest {

    private ResourceService resourceService;
    private TicketService ticketService;
    private NewCreateMessageHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;

    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = getResourceServiceBuilder().build();
        this.ticketService = getTicketService();
        var messageService = getMessageService();
        this.handler = new NewCreateMessageHandler(messageService, ticketService, uriRetriever);
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenWhenUserAttemptsToAddMessageWhenTheyAreNotThePublicationOwnerOrCurator(
        Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var user = randomUserInstance(ticket.getCustomerId());
        var request = createNewMessageRequestForResourceOwner(publication, ticket, user, randomString());
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#invalidAccessRightForTicketTypeProvider")
    void shouldReturnForbiddenWhenSenderIsElevatedUserWithInvalidAccessRightForTicketType(
        Class<? extends TicketEntry> ticketType, AccessRight accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var sender = UserInstance.create(randomString(), publication.getPublisher().getId());
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, expectedText, accessRights);

        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenWhenSenderIsCuratorOfAnAlienInstitution(Class<? extends TicketEntry> ticketType,
                                                                      PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var sender = UserInstance.create(randomString(), randomUri());
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, expectedText,
                                                             MANAGE_DOI);

        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldCreateMessageAndSetCuratorAsAssigneeWhenSenderIsCuratorAndTicketHasNoAssignee(
        Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var sender = UserInstance.create(randomString(), publication.getPublisher().getId());
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, expectedText,
                                                             MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS,
                                                             MANAGE_RESOURCES_STANDARD, SUPPORT);

        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThatResponseContainsCorrectInformation(response, ticket);
        var ticketWithMessage = ticketService.fetchTicket(ticket);
        assertThat(ticketWithMessage.getAssignee(), is(equalTo(new Username(sender.getUsername()))));
        var expectedSender = sender.getUser();
        assertThatMessageContainsTextAndCorrectCorrespondentInfo(expectedText, ticket, expectedSender);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldCreateMessageAndDoNotSetCuratorAsAssigneeWhenSenderIsCuratorAndPublicationOwnerAndTicketHasNoAssignee(
        Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws ApiGatewayException, IOException {
        var curatorAndOwner = new UserInstance(new User(randomString()).toString(), randomUri(), randomUri(), null,
                                               null);
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, curatorAndOwner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, curatorAndOwner, expectedText,
                                                             MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS);

        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThatResponseContainsCorrectInformation(response, ticket);
        var ticketWithMessage = ticketService.fetchTicket(ticket);
        assertThat(ticketWithMessage.getAssignee(), is(nullValue()));
        var expectedSender = curatorAndOwner.getUser();
        assertThatMessageContainsTextAndCorrectCorrespondentInfo(expectedText, ticket, expectedSender);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldCreateMessageWhenCuratorHasValidAccessRightForTicketType(PublicationStatus publicationStatus,
                                                                        Class<? extends TicketEntry> ticketType,
                                                                        AccessRight accessRight)
        throws ApiGatewayException, IOException {

        var publication = TicketTestUtils.createPersistedPublication(publicationStatus, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var sender = new UserInstance(randomString(), publication.getPublisher().getId(),
                                      publication.getResourceOwner().getOwnerAffiliation(), null,
                                      null);
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, expectedText,
                                                             MANAGE_RESOURCES_STANDARD, accessRight);

        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThatResponseContainsCorrectInformation(response, ticket);
        var expectedSender = sender.getUser();
        assertThatMessageContainsTextAndCorrectCorrespondentInfo(expectedText, ticket, expectedSender);
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as unread for everyone except curator when curator sends a message")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForEveryoneExceptCuratorWhenCuratorSendsAMessage(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var curator = UserInstance.create("CURATOR", publication.getPublisher().getId());
        var request = createNewMessageRequestForElevatedUser(publication, ticket, curator, randomString(),
                                                             MANAGE_RESOURCES_STANDARD, MANAGE_DOI, MANAGE_DEGREE,
                                                             MANAGE_PUBLISHING_REQUESTS, SUPPORT);
        handler.handleRequest(request, output, context);
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasSize(1));
        assertThat(updatedTicket.getViewedBy(), hasItem(curator.getUser()));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as unread for everyone except owner when owner sends a message")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnreadForEveryoneExceptOwnerWhenOwnerSendsAMessage(Class<? extends TicketEntry> ticketType,
                                                                              PublicationStatus status)
        throws ApiGatewayException, IOException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var owner = UserInstance.fromPublication(publication);
        var request = createNewMessageRequestForResourceOwner(publication, ticket, owner, randomString());
        handler.handleRequest(request, output, context);
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasSize(1));
        assertThat(updatedTicket.getViewedBy(), hasItem(owner.getUser()));
    }

    @Test
    void shouldUpdateStatusToPendingWhenNewMessageCreatedForGeneralSupportRequestWithStatusCompleted()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);
        ticketService.updateTicketStatus(ticket, TicketStatus.COMPLETED, null);
        var owner = UserInstance.fromPublication(publication);
        var request = createNewMessageRequestForResourceOwner(publication, ticket, owner, randomString());
        handler.handleRequest(request, output, context);
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getStatus(), is(equalTo(TicketStatus.PENDING)));
    }

    private static Map<String, String> pathParameters(Publication publication,
                                                      TicketEntry ticket) {
        return Map.of(
            PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publication.getIdentifier().toString(),
            TICKET_IDENTIFIER, ticket.getIdentifier().toString()
        );
    }

    private void assertThatMessageContainsTextAndCorrectCorrespondentInfo(String expectedText,
                                                                          TicketEntry ticket,
                                                                          User expectedSender) {
        var actualMessage = ticket.fetchMessages(ticketService).stream().collect(SingletonCollector.collect());
        assertThat(actualMessage.getText(), is(equalTo(expectedText)));
        assertThat("Sender was:" + actualMessage.getSender(), actualMessage.getSender(),
                   is(equalTo(expectedSender)));
    }

    private void assertThatResponseContainsCorrectInformation(GatewayResponse<Void> response, TicketEntry ticket) {
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));

        var actualMessage = ticket.fetchMessages(ticketService).stream().collect(SingletonCollector.collect());
        assertThat(response.getHeaders().get(LOCATION_HEADER),
                   is(equalTo(createExpectedLocationHeader(actualMessage))));
    }

    private UserInstance randomUserInstance(URI customerId) {
        return UserInstance.create(randomString(), customerId);
    }

    private String createExpectedLocationHeader(Message actualMessage) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild("publication")
                   .addChild(actualMessage.getResourceIdentifier().toString())
                   .addChild("ticket")
                   .addChild(actualMessage.getTicketIdentifier().toString())
                   .addChild("message")
                   .addChild(actualMessage.getIdentifier().toString())
                   .toString();
    }

    private InputStream createNewMessageRequestForResourceOwner(Publication publication,
                                                                TicketEntry ticket,
                                                                UserInstance userInstance,
                                                                String randomString) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParameters(publication, ticket))
                   .withBody(messageBody(randomString))
                   .withUserName(userInstance.getUsername())
                   .withCurrentCustomer(userInstance.getCustomerId())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream createNewMessageRequestForElevatedUser(Publication publication,
                                                               TicketEntry ticket,
                                                               UserInstance user,
                                                               String message,
                                                               AccessRight... accessRights)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParameters(publication, ticket))
                   .withBody(messageBody(message))
                   .withUserName(user.getUsername())
                   .withCurrentCustomer(user.getCustomerId())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .withPersonCristinId(randomUri())
                   .withAccessRights(user.getCustomerId(), accessRights)
                   .build();
    }

    private CreateMessageRequest messageBody(String message) {
        var request = new CreateMessageRequest();
        request.setMessage(message);
        return request;
    }
}
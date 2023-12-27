package no.unit.nva.publication.messages.create;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.messages.MessageApiConfig.LOCATION_HEADER;
import static no.unit.nva.publication.messages.MessageApiConfig.TICKET_IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.APPROVE_DOI_REQUEST;
import static nva.commons.apigateway.AccessRight.APPROVE_PUBLISH_REQUEST;
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
import java.time.Clock;
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
import no.unit.nva.publication.service.impl.MessageService;
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
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
        MessageService messageService = new MessageService(client);
        this.handler = new NewCreateMessageHandler(messageService, ticketService);
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
        var request = createNewMessageRequestForNonElevatedUser(publication, ticket, user, randomString());
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
                                                             APPROVE_DOI_REQUEST);

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
                                                             APPROVE_DOI_REQUEST, APPROVE_PUBLISH_REQUEST);

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
        var curatorAndOwner = UserInstance.create(new User(randomString()), randomUri());
        var publication = TicketTestUtils.createPersistedPublicationWithOwner(status, curatorAndOwner, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, curatorAndOwner, expectedText,
                                                             APPROVE_DOI_REQUEST, APPROVE_PUBLISH_REQUEST);

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
    void shouldCreateMessageWhenCuratorHasValidAccessRightForTicketType(Class<? extends TicketEntry> ticketType,
                                                                        AccessRight accessRight)
        throws ApiGatewayException, IOException {

        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var sender = UserInstance.create(randomString(), publication.getPublisher().getId());
        var expectedText = randomString();
        var request = createNewMessageRequestForElevatedUser(publication, ticket, sender, expectedText, accessRight);

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
                                                             APPROVE_DOI_REQUEST, APPROVE_PUBLISH_REQUEST);
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
        var request = createNewMessageRequestForNonElevatedUser(publication, ticket, owner, randomString());
        handler.handleRequest(request, output, context);
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getViewedBy(), hasSize(1));
        assertThat(updatedTicket.getViewedBy(), hasItem(owner.getUser()));
    }

    @Test
    void shouldUpdateStatusToPendingWhenNewMessageCreatedForGeneralSupportRequest()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);
        ticketService.updateTicketStatus(ticket, TicketStatus.COMPLETED, null);
        var s = ticketService.fetchTicket(ticket);
        var owner = UserInstance.fromPublication(publication);
        var request = createNewMessageRequestForNonElevatedUser(publication, ticket, owner, randomString());
        handler.handleRequest(request, output, context);
        var updatedTicket = ticket.fetch(ticketService);
        assertThat(updatedTicket.getStatus(), is(equalTo(TicketStatus.PENDING)));
    }

    private static Map<String, String> pathParameters(Publication publication,
                                                      TicketEntry ticket) {
        return Map.of(
            PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publication.getIdentifier().toString(),
            TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString()
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

    private InputStream createNewMessageRequestForNonElevatedUser(Publication publication,
                                                                  TicketEntry ticket,
                                                                  UserInstance userInstance,
                                                                  String randomString) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateMessageRequest>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(pathParameters(publication, ticket))
                   .withBody(messageBody(randomString))
                   .withUserName(userInstance.getUsername())
                   .withCurrentCustomer(userInstance.getOrganizationUri())
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
                   .withCurrentCustomer(user.getOrganizationUri())
                   .withAccessRights(user.getOrganizationUri(), accessRights)
                   .build();
    }

    private CreateMessageRequest messageBody(String message) {
        var request = new CreateMessageRequest();
        request.setMessage(message);
        return request;
    }
}
package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketDtoParser;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ListTicketsForPublicationHandlerTest extends TicketTestLocal {

    private ListTicketsForPublicationHandler handler;
    private MessageService messageService;
    private UriRetriever uriRetriever;

    public static Stream<Arguments> accessRightAndTicketTypeProvider() {
        return Stream.of(Arguments.of(DoiRequest.class, AccessRight.MANAGE_DOI),
            Arguments.of(GeneralSupportRequest.class, AccessRight.SUPPORT));
    }

    public static Stream<Arguments> accessRightAndTicketTypeProviderDraft() {
        return Stream.of(Arguments.of(PublishingRequestCase.class, AccessRight.MANAGE_PUBLISHING_REQUESTS));
    }

    @BeforeEach
    public void setup() {
        super.init();
        this.uriRetriever = mock(UriRetriever.class);
        this.handler = new ListTicketsForPublicationHandler(resourceService, ticketService, uriRetriever);
        this.messageService = new MessageService(client);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnAllTicketsForPublicationWhenUserIsThePublicationOwner(Class<? extends TicketEntry> ticketType,
                                                                           PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = ownerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        assertThatResponseContainsExpectedTickets(ticket);
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotReturnRemovedTicket(Class<? extends TicketEntry> ticketType,
                                                                           PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        ticket.remove(UserInstance.fromTicket(ticket)).persistUpdate(ticketService);
        var request = ownerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), is(emptyIterable()));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnAllTicketsWithMessagesForPublicationWhenTicketContainsMessages(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var expectedTicketDto = constructDto(createPersistedTicketWithMessage(ticketType, publication));

        var request = ownerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        assertThat(body.getTickets(), containsInAnyOrder(expectedTicketDto));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenForPublicationWhenUserIsNotTheOwnerAndNotElevatedUser(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = nonOwnerRequestsTicketsForPublication(publication);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnForbiddenWhenUserIsElevatedUserOfAlienOrganization(Class<? extends TicketEntry> ticketType,
                                                                        PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = elevatedUserOfAlienOrgRequestsTicketsForPublication(publication);

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("accessRightAndTicketTypeProvider")
    void shouldListTicketsOfTypeCuratorHasAccessRightToOperateOn(
        Class<? extends TicketEntry> ticketType, AccessRight accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);

        TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
        TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);

        var request = curatorWithAccessRightRequestTicketsForPublication(publication, accessRight);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), hasSize(1));
        assertThat(body.getTickets().getFirst().ticketType(), is(equalTo(ticketType)));
    }

    @ParameterizedTest
    @MethodSource("accessRightAndTicketTypeProviderDraft")
    void shouldListTicketsOfTypeCuratorHasAccessRightToOperateOnDraft(
        Class<? extends TicketEntry> ticketType, AccessRight accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);

        TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);

        var request = curatorWithAccessRightRequestTicketsForPublication(publication, accessRight);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), hasSize(1));
        assertThat(body.getTickets().getFirst().ticketType(), is(equalTo(ticketType)));
    }

    @ParameterizedTest
    @MethodSource("accessRightAndTicketTypeProvider")
    void shouldListAllTicketsWhenCuratorIsPublicationOwnerAndHasAccessRight(
        Class<? extends TicketEntry> ticketType, AccessRight accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);

        TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
        TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);
        TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);

        var request = curatorWithAccessRightRequestTicketsForPublicationAsPublicationOwner(publication, accessRight);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), hasSize(3));
    }

    private TicketEntry createPersistedTicketWithMessage(Class<? extends TicketEntry> ticketType,
                                                         Publication publication) throws ApiGatewayException {
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        return ticket;
    }

    private TicketDto constructDto(TicketEntry ticketEntry) {
        var messages = ticketEntry.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticketEntry, messages);
    }

    private static InputStream ownerRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(publication.getResourceOwner().getOwner().getValue())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private static InputStream nonOwnerRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(randomString())
                   .withPersonCristinId(randomUri())
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
                   .withCurrentCustomer(customerId)
                   .withUserName(randomString())
                   .withAccessRights(customerId, AccessRight.MANAGE_DOI)
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream elevatedUserRequestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(randomString())
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.MANAGE_DOI)
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream curatorWithAccessRightRequestTicketsForPublication(Publication publication,
                                                                           AccessRight accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(randomString())
                   .withAccessRights(publication.getPublisher().getId(), MANAGE_RESOURCES_STANDARD,
                                     accessRight)
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .build();
    }

    private InputStream curatorWithAccessRightRequestTicketsForPublicationAsPublicationOwner(Publication publication,
                                                                           AccessRight accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(publication.getResourceOwner().getOwner().getValue())
                   .withAccessRights(publication.getPublisher().getId(), accessRight)
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private void assertThatResponseContainsExpectedTickets(TicketEntry ticket) throws JsonProcessingException {
        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        var actualTicketIdentifiers = body.getTickets()
                                          .stream()
                                          .map(TicketDtoParser::toTicket)
                                          .map(Entity::getIdentifier)
                                          .collect(Collectors.toList());
        var expectedIdentifiers = ticket.getIdentifier();
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(actualTicketIdentifiers, containsInAnyOrder(expectedIdentifiers));
    }
}
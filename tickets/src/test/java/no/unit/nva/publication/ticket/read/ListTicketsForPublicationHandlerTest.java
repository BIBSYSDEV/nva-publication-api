package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationServiceConfig;
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
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ListTicketsForPublicationHandlerTest extends TicketTestLocal {

    private ListTicketsForPublicationHandler handler;
    private MessageService messageService;

    public static Stream<Arguments> accessRightAndTicketTypeProvider() {
        return Stream.of(Arguments.of(DoiRequest.class,
                                      new AccessRight[]{AccessRight.MANAGE_DOI, MANAGE_RESOURCES_STANDARD}),
                         Arguments.of(GeneralSupportRequest.class,
                                      new AccessRight[]{AccessRight.SUPPORT, MANAGE_RESOURCES_STANDARD}),
                         Arguments.of(PublishingRequestCase.class,
                                      new AccessRight[]{AccessRight.MANAGE_PUBLISHING_REQUESTS,
                                          MANAGE_RESOURCES_STANDARD}));
    }

    public static Stream<Arguments> accessRightAndTicketTypeProviderDraft() {
        return Stream.of(Arguments.of(PublishingRequestCase.class,
                                      new AccessRight[]{AccessRight.MANAGE_PUBLISHING_REQUESTS,
                                          MANAGE_RESOURCES_STANDARD}));
    }

    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new ListTicketsForPublicationHandler(resourceService, ticketService);
        this.messageService = getMessageService();
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
    void shouldReturnAllTicketsForPublicationWhenUserIsCurator(Class<? extends TicketEntry> ticketType,
                                                               PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        mockSiktOrg();
        var ticket = TicketTestUtils.createPersistedTicket(publication, ticketType, ticketService);
        var request = curatorRquestsTicketsForPublication(publication);
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
        Class<? extends TicketEntry> ticketType, AccessRight[] accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        mockSiktOrg();

        TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
        TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);
        TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);

        var request = curatorWithAccessRightRequestTicketsForPublication(publication, accessRight);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);

        assertThat(body.getTickets(), hasSize(3));
    }

    private void mockSiktOrg() {
        when(uriRetriever.getRawContent(
            eq(URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0")),
            any())).thenReturn(
            Optional.of(IoUtils.stringFromResources(Path.of("cristin-orgs/20754.6.0.0.json"))));
    }

    @ParameterizedTest
    @MethodSource("accessRightAndTicketTypeProviderDraft")
    void shouldListTicketsOfTypeCuratorHasAccessRightToOperateOnDraft(
        Class<? extends TicketEntry> ticketType, AccessRight[] accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        mockSiktOrg();

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
        Class<? extends TicketEntry> ticketType, AccessRight[] accessRight)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);

        TicketTestUtils.createPersistedTicket(publication, DoiRequest.class, ticketService);
        TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);
        TicketTestUtils.createPersistedTicket(publication, GeneralSupportRequest.class, ticketService);

        var request = curatorWithAccessRightForPublication(publication, accessRight);
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

    private static InputStream curatorRquestsTicketsForPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(randomString())
                   .withPersonCristinId(randomUri())
                   .withAccessRights(publication.getPublisher().getId(), MANAGE_PUBLISHING_REQUESTS, MANAGE_DOI,
                                     SUPPORT, MANAGE_RESOURCES_STANDARD)
                   .withTopLevelCristinOrgId(
                       URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0"))
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

    private InputStream curatorWithAccessRightRequestTicketsForPublication(Publication publication,
                                                                           AccessRight[] accessRight)
        throws JsonProcessingException {
        var customer = publication.getCuratingInstitutions().stream().findFirst().orElseThrow();
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withPathParameters(constructPathParameters(publication))
                   .withCurrentCustomer(customer)
                   .withUserName(randomString())
                   .withAccessRights(customer, accessRight)
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(
                       URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0"))
                   .build();
    }

    private InputStream curatorWithAccessRightForPublication(Publication publication,
                                                             AccessRight[] accessRight)
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
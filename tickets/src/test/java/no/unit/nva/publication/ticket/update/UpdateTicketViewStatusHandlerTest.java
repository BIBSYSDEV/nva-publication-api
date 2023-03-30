package no.unit.nva.publication.ticket.update;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.TICKET_PATH;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.publication.ticket.TicketConfig.TICKET_IDENTIFIER_PARAMETER_NAME;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.LOCATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;
import no.unit.nva.publication.ticket.test.TicketTestUtils;

class UpdateTicketViewStatusHandlerTest extends TicketTestLocal {
    
    private UpdateTicketViewStatusHandler handler;
    
    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new UpdateTicketViewStatusHandler(ticketService);
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
        assertThat(response.getStatusCode(), is(equalTo(HTTP_SEE_OTHER)));
        
        var expectedLocationHeader = createLocationUri(publication, ticket);
        assertThat(response.getHeaders().get(LOCATION_HEADER), is(equalTo(expectedLocationHeader.toString())));
        
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
        assertThat(response.getStatusCode(), is(equalTo(HTTP_SEE_OTHER)));
        
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
        assertThat(response.getStatusCode(), is(equalTo(HTTP_SEE_OTHER)));
        
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
        assertThat(response.getStatusCode(), is(equalTo(HTTP_SEE_OTHER)));
        
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
        assertThatTicketIsUnchanged(ticket);
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
    void shouldReturnForbiddenWhenTicketDoesNotExistAndUserIsNotElevatedUser(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var ticket = TicketTestUtils.createNonPersistedTicket(publication, ticketType);
        var httpRequest = ownerAttemptsToMarkExistingTicketConnectedToWrongPublication(ticket,
            publication.getIdentifier());
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @ParameterizedTest(name = "body:{0}")
    @DisplayName("should return Bad Request when input is malformed")
    @ValueSource(strings = {
        "{\"type\": \"UpdateViewStatusRequest\"}",
        "{\"type\": \"UpdateViewStatusRequest\", \"viewedStatus\": \"\"}"})
    void shouldReturnBadRequestWhenBodyIsEmpty(String requestBody) throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication();
        var ticket = persistTicket(publication, PublishingRequestCase.class);
        var httpRequest = requestWithEmptyBody(ticket, requestBody);
        handler.handleRequest(httpRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_BAD_REQUEST)));
    }
    
    private static void assertThatTicketIsUnchanged(TicketEntry ticket) {
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
        assertThat(ticket.getViewedBy(), hasItem(TicketEntry.SUPPORT_SERVICE_CORRESPONDENT));
    }
    
    private static URI createLocationUri(Publication publication, TicketEntry ticket) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_PATH)
                   .addChild(publication.getIdentifier().toString())
                   .addChild(TICKET_PATH)
                   .addChild(ticket.getIdentifier().toString())
                   .getUri();
    }
    
    private static Map<String, String> createPathParameters(TicketEntry ticket,
                                                            SortableIdentifier publicationIdentifier) {
        return Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publicationIdentifier.toString(),
            TICKET_IDENTIFIER_PARAMETER_NAME, ticket.getIdentifier().toString());
    }
    
    private static InputStream createOwnerMarksTicket(Publication publication,
                                                      TicketEntry ticket,
                                                      ViewStatus viewStatus)
        throws JsonProcessingException, BadRequestException {
        return new HandlerRequestBuilder<UpdateViewStatusRequest>(JsonUtils.dtoObjectMapper)
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(ticket.getOwner().toString())
                   .withBody(new UpdateViewStatusRequest(viewStatus))
                   .withPathParameters(createPathParameters(ticket, publication.getIdentifier()))
                   .build();
    }
    
    private static InputStream elevatedUserMarksTicket(Publication publication,
                                                       TicketEntry ticket,
                                                       ViewStatus viewStatus,
                                                       URI customerId)
        throws JsonProcessingException, BadRequestException {
        return new HandlerRequestBuilder<UpdateViewStatusRequest>(JsonUtils.dtoObjectMapper)
                   .withCustomerId(customerId)
                   .withNvaUsername(randomString())
                   .withAccessRights(customerId, AccessRight.APPROVE_DOI_REQUEST.toString())
                   .withBody(new UpdateViewStatusRequest(viewStatus))
                   .withPathParameters(createPathParameters(ticket, publication.getIdentifier()))
                   .build();
    }
    
    private InputStream requestWithEmptyBody(TicketEntry ticket, String requestBody) throws JsonProcessingException {
        return new HandlerRequestBuilder<String>(JsonUtils.dtoObjectMapper)
                   .withBody(requestBody)
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(ticket.getOwner().toString())
                   .withPathParameters(createPathParameters(ticket, ticket.extractPublicationIdentifier()))
                   .build();
    }
    
    private InputStream ownerAttemptsToMarkExistingTicketConnectedToWrongPublication(
        TicketEntry ticket, SortableIdentifier wrongPublicationIdentifier)
        throws JsonProcessingException, BadRequestException {
        return new HandlerRequestBuilder<UpdateViewStatusRequest>(JsonUtils.dtoObjectMapper)
                   .withBody(new UpdateViewStatusRequest(ViewStatus.UNREAD))
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(ticket.getOwner().toString())
                   .withPathParameters(createPathParameters(ticket, wrongPublicationIdentifier))
                   .build();
    }
    
    private InputStream curatorAttemptsToMarkExistingTicketConnectedToWrongPublication(
        TicketEntry ticket,
        SortableIdentifier wrongPublicationIdentifier)
        throws JsonProcessingException, BadRequestException {
        return new HandlerRequestBuilder<UpdateViewStatusRequest>(JsonUtils.dtoObjectMapper)
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(randomString())
                   .withBody(new UpdateViewStatusRequest(ViewStatus.UNREAD))
                   .withPathParameters(createPathParameters(ticket, wrongPublicationIdentifier))
                   .withAccessRights(ticket.getCustomerId(), AccessRight.APPROVE_DOI_REQUEST.toString())
                   .build();
    }
    
    private InputStream alienCuratorMarksTicket(Publication publication, TicketEntry ticket)
        throws JsonProcessingException, BadRequestException {
        return elevatedUserMarksTicket(publication, ticket, ViewStatus.UNREAD, randomUri());
    }
    
    private InputStream curatorMarksTicket(Publication publication, TicketEntry ticket, ViewStatus viewStatus)
        throws JsonProcessingException, BadRequestException {
        return elevatedUserMarksTicket(publication, ticket, viewStatus, ticket.getCustomerId());
    }
}
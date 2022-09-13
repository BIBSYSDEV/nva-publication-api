package no.unit.nva.publication.publishingrequest.create;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.publishingrequest.create.CreateTicketHandler.LOCATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.DoiRequestDto;
import no.unit.nva.publication.publishingrequest.PublishingRequestDto;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.publishingrequest.TicketTestLocal;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

class CreateTicketHandlerTest extends TicketTestLocal {
    
    private CreateTicketHandler handler;
    
    public static Stream<Arguments> ticketEntryProvider() {
        return Stream.of(Arguments.of(DoiRequest.class), Arguments.of(PublishingRequestCase.class));
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        this.handler = new CreateTicketHandler(ticketService, resourceService);
    }
    
    @ParameterizedTest(name = "ticketType")
    @DisplayName("should persist ticket when publication exists, user is publication owner and "
                 + "publication meets ticket creation criteria")
    @MethodSource("ticketEntryProvider")
    void shouldPersistTicketWhenPublicationExistsUserIsOwnerAndPublicationMeetsTicketCreationCriteria(
        Class<? extends TicketEntry> ticketType) throws IOException, ApiGatewayException {
    
        var publication = createAndPersistDraftPublication();
        var requestBody = constructDto(ticketType);
        var owner = UserInstance.fromPublication(publication);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_SEE_OTHER)));
        
        var location = URI.create(response.getHeaders().get(LOCATION_HEADER));
    
        assertThatLocationHeaderHasExpectedFormat(publication, location);
        assertThatLocationHeaderPointsToCreatedTicket(location);
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should should not allow creating a ticket for non existing publication")
    @MethodSource("ticketEntryProvider")
    void shouldNotAllowTicketCreationForNonExistingPublication(Class<? extends TicketEntry> ticketType)
        throws IOException {
        var publication = nonPersistedPublication();
        var requestBody = constructDto(ticketType);
        var owner = UserInstance.fromPublication(publication);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should not allow users to create tickets for publications they do not own")
    @MethodSource("ticketEntryProvider")
    void shouldNotAllowUsersToCreateTicketsForPublicationsTheyDoNotOwn(Class<? extends TicketEntry> ticketType)
        throws IOException, ApiGatewayException {
        var publication = createPersistedDraftPublication();
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var input = createHttpTicketCreationRequest(requestBody, publication, user);
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should not allow users to create tickets for publications belonging to different organization"
                 + "than the one they are currently logged in to")
    @MethodSource("ticketEntryProvider")
    void shouldNotAllowUsersToCreateTicketsForPublicationsBelongingToDifferentOrgThanTheOneTheyAreLoggedInTo(
        Class<? extends TicketEntry> ticketType)
        throws IOException, ApiGatewayException {
        var publication = createPersistedDraftPublication();
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(publication.getResourceOwner().getOwner(), randomUri());
        var input = createHttpTicketCreationRequest(requestBody, publication, user);
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should not allow anonymous users to create tickets")
    @MethodSource("ticketEntryProvider")
    void shouldNotAllowAnonymousUsersToCreateTickets(Class<? extends TicketEntry> ticketType)
        throws IOException, ApiGatewayException {
        var publication = createPersistedDraftPublication();
        var requestBody = constructDto(ticketType);
        var input = createAnonymousHttpTicketCreationRequest(requestBody, publication);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }
    
    @Test
    void shouldNotAllowDoiRequestTicketCreationWhenPublicationHasExistingDoiProducedByNvaOrLegacySystem()
        throws IOException {
        var publication = createPersistedPublicationWithDoi();
        assertThat(publication.getDoi(), is(not(nullValue())));
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(DoiRequest.class);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
    }
    
    @Test
    void shouldNotAllowPublishingRequestTicketCreationWhenPublicationIsPublished()
        throws ApiGatewayException, IOException {
        var publication = createPersistedDraftPublication();
        resourceService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(PublishingRequestCase.class);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
    
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
    }
    
    @Test
    void shouldNotAllowPublishingRequestTicketCreationWhenPublicationIsNotPublishable() throws IOException {
        var publication = createUnpublishablePublication();
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(PublishingRequestCase.class);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
    }
    
    private Publication createUnpublishablePublication() {
        var publication = randomPublication().copy().withEntityDescription(null).build();
        publication = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        return publication;
    }
    
    private Publication createPersistedPublicationWithDoi() {
        var publication = randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private static SortableIdentifier extractTicketIdentifierFromLocation(URI location) {
        return new SortableIdentifier(UriWrapper.fromUri(location).getLastPathElement());
    }
    
    private static void assertThatLocationHeaderHasExpectedFormat(Publication publication, URI location) {
        var ticketIdentifier = extractTicketIdentifierFromLocation(location);
        var publicationIdentifier = extractPublicationIdentifierFromLocation(location);
        assertThat(publicationIdentifier, is(equalTo(publication.getIdentifier())));
        assertThat(ticketIdentifier, is(not(nullValue())));
    }
    
    private static SortableIdentifier extractPublicationIdentifierFromLocation(URI location) {
        return UriWrapper.fromUri(location)
                   .getParent()
                   .flatMap(UriWrapper::getParent)
                   .map(UriWrapper::getLastPathElement)
                   .map(SortableIdentifier::new)
                   .orElseThrow();
    }
    
    private void assertThatLocationHeaderPointsToCreatedTicket(URI ticketUri)
        throws NotFoundException {
        var publication = fetchPublication(ticketUri);
        var ticketIdentifier = extractTicketIdentifierFromLocation(ticketUri);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);
        assertThat(ticket.getResourceIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(ticket.getIdentifier(), is(equalTo(ticketIdentifier)));
    }
    
    private Publication fetchPublication(URI ticketUri) throws NotFoundException {
        var publicationIdentifier = extractPublicationIdentifierFromLocation(ticketUri);
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }
    
    private TicketDto constructDto(Class<? extends TicketEntry> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequestDto.empty();
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return PublishingRequestDto.empty();
        }
        throw new RuntimeException("Unrecognized ticket type");
    }
    
    private Publication createPersistedDraftPublication() throws ApiGatewayException {
        var publication = randomPublication();
        publication.setDoi(null); // for creating DoiRequests
        publication = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        return resourceService.getPublication(publication);
    }
    
    private InputStream createHttpTicketCreationRequest(TicketDto ticketDto,
                                                        Publication publication,
                                                        UserInstance userCredentials)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withPathParameters(Map.of("publicationIdentifier", publication.getIdentifier().toString()))
                   .withNvaUsername(userCredentials.getUserIdentifier())
                   .withCustomerId(userCredentials.getOrganizationUri())
                   .build();
    }
    
    private InputStream createAnonymousHttpTicketCreationRequest(TicketDto ticketDto,
                                                                 Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withPathParameters(Map.of("publicationIdentifier", publication.getIdentifier().toString()))
                   .build();
    }
}

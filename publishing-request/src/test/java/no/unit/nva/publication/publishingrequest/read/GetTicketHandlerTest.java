package no.unit.nva.publication.publishingrequest.read;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistDraftPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.APPROVE_DOI_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.publishingrequest.TicketUtils;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

class GetTicketHandlerTest extends ResourcesLocalTest {
    
    private ResourceService resourceService;
    private TicketService ticketService;
    private ByteArrayOutputStream outputStream;
    private FakeContext context;
    private GetTicketHandler handler;
    
    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client, Clock.systemDefaultZone());
        this.outputStream = new ByteArrayOutputStream();
        this.context = new FakeContext();
        this.handler = new GetTicketHandler(ticketService);
    }
    
    @ParameterizedTest(name = " ticket type: {0}")
    @DisplayName("should return ticket when client is owner of associated publication "
                 + "and therefore of the ticket")
    @MethodSource("ticketTypeProvider")
    void shouldReturnTicketWhenClientIsOwnerOfAssociatedPublicationAndThereforeOfTheTicket(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var publication = resourceService.getPublicationByIdentifier(ticket.getResourceIdentifier());
        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);
        var actualTicketEntry = ticketDto.toTicket();
        assertThat(TicketDto.fromTicket(actualTicketEntry), is(equalTo(ticketDto)));
    }
    
    @ParameterizedTest(name = " ticket type: {0}")
    @DisplayName("should  return not found when publication identifier exists, but ticket identifier does not "
                 + "correspond to a ticket of that publication")
    @MethodSource("ticketTypeProvider")
    void shouldReturnNotFoundWhenPublicationIdentifierExistsButTicketIdentifierDoesCorrespondToPublication(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication(resourceService);
        var otherPublication = createAndPersistDraftPublication(resourceService);
        var ticket = createPersistedTicket(ticketType, otherPublication);
        var request = createHttpRequest(publication, ticket).build();
        handler.handleRequest(request, outputStream, context);
        
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    @ParameterizedTest(name = " ticket type: {0}")
    @DisplayName("should  return not found when user is not the owner of the associated publication")
    @MethodSource("ticketTypeProvider")
    void shouldReturnNotFoundWhenUserIsNotTheOwnerOfTheAssociatedPublication(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication(resourceService);
        var ticket = createPersistedTicket(ticketType, publication);
        var request = createHttpRequest(publication, ticket, randomOwner()).build();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should return ticket when curator is requester")
    @MethodSource("ticketTypeProvider")
    void shouldReturnTicketWhenCuratorIsRequester(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var request =
            createHttpRequestForElevatedUser(ticket, ticket.getCustomerId(), APPROVE_DOI_REQUEST).build();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);
        var actualTicketEntry = ticketDto.toTicket();
        assertThat(TicketDto.fromTicket(actualTicketEntry), is(equalTo(ticketDto)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should return Not Found when requester is curator of wrong institution")
    @MethodSource("ticketTypeProvider")
    void shouldReturnTicketWhenRequesterIsCuratorOfWrongInstitution(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var request = createHttpRequestForElevatedUser(ticket, randomUri(), APPROVE_DOI_REQUEST).build();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    @ParameterizedTest(name = "ticket type:{0}")
    @DisplayName("should return Not Found when requester is the wrong type of elevated user")
    @MethodSource("ticketTypeProvider")
    void shouldReturnNotFoundWhenRequestIsTheWrongTypeOfElevatedUser(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var ticket = createPersistedTicket(ticketType);
        var wrongAccessRights = new HashSet<>(Arrays.asList(AccessRight.values()));
        wrongAccessRights.remove(APPROVE_DOI_REQUEST);
        var request =
            createHttpRequestForElevatedUser(ticket, ticket.getCustomerId(), randomElement(wrongAccessRights)).build();
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    private static Map<String, String> createPathParameters(Publication publication, TicketEntry ticket) {
        return Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER,
            publication.getIdentifier().toString(),
            TicketUtils.TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString());
    }
    
    private HandlerRequestBuilder<TicketDto> createHttpRequestForElevatedUser(TicketEntry ticket,
                                                                              URI customerId,
                                                                              AccessRight accessRight)
        throws NotFoundException {
        return createHttpRequest(ticket)
                   .withCustomerId(customerId)
                   .withNvaUsername(randomString())
                   .withAccessRights(ticket.getCustomerId(), accessRight.toString());
    }
    
    private TicketEntry createPersistedTicket(Class<? extends TicketEntry> ticketType, Publication publication)
        throws ApiGatewayException {
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        return ticketService.createTicket(ticket, ticketType);
    }
    
    private String randomOwner() {
        return randomString();
    }
    
    private HandlerRequestBuilder<TicketDto> createHttpRequest(TicketEntry ticket)
        throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(ticket.getResourceIdentifier());
        return createHttpRequest(publication, ticket);
    }
    
    private HandlerRequestBuilder<TicketDto> createHttpRequest(Publication publication, TicketEntry ticket) {
        return createHttpRequest(publication, ticket, ticket.getOwner());
    }
    
    private HandlerRequestBuilder<TicketDto> createHttpRequest(Publication publication, TicketEntry ticket,
                                                               String owner) {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(owner)
                   .withPathParameters(createPathParameters(publication, ticket));
    }
    
    private TicketEntry createPersistedTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = createAndPersistDraftPublication(resourceService);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        return ticketService.createTicket(ticket, ticketType);
    }
}
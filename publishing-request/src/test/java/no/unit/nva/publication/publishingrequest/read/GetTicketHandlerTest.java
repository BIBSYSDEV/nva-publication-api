package no.unit.nva.publication.publishingrequest.read;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.publication.publishingrequest.PublishingRequestTestUtils.createAndPersistDraftPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
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
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
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
        var ticket = createTicket(ticketType);
        var request = createHttpRequest(ticket);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, TicketDto.class);
        var ticketDto = response.getBodyObject(TicketDto.class);
        var actualTicketEntry = ticketDto.toTicket();
        assertThat(TicketDto.fromTicket(actualTicketEntry), is(equalTo(ticketDto)));
    }
    
    @ParameterizedTest(name = " ticket type: {0}")
    @DisplayName("should  return not found when ticket identifier is wrong")
    @MethodSource("ticketTypeProvider")
    void shouldReturnNotFoundWhenPublicationIdIsCorrectButTicketIdentifierIsWrong(
        Class<? extends TicketEntry> ticketType) throws ApiGatewayException, IOException {
        var publication = createAndPersistDraftPublication(resourceService);
        
        var ticket = createUnpersistedTicket(publication, ticketType);
        var request = createHttpRequest(ticket);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_NOT_FOUND)));
    }
    
    private static Map<String, String> createPathParameters(TicketEntry ticket) {
        return Map.of(PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER,
            ticket.getResourceIdentifier().toString(),
            TicketUtils.TICKET_IDENTIFIER_PATH_PARAMETER, ticket.getIdentifier().toString());
    }
    
    private TicketEntry createUnpersistedTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ConflictException {
        return TicketEntry.createNewTicket(publication, ticketType, Clock.systemDefaultZone(),
            SortableIdentifier::next);
    }
    
    private InputStream createHttpRequest(TicketEntry ticket) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withCustomerId(ticket.getCustomerId())
                   .withNvaUsername(ticket.getOwner())
                   .withPathParameters(createPathParameters(ticket))
                   .build();
    }
    
    private TicketEntry createTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = createAndPersistDraftPublication(resourceService);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        return ticketService.createTicket(ticket, ticketType);
    }
}
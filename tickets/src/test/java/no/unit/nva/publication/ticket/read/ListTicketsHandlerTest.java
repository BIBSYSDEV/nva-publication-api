package no.unit.nva.publication.ticket.read;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import publication.test.TicketTestUtils;

class ListTicketsHandlerTest extends ResourcesLocalTest {
    
    private static final Context CONTEXT = new FakeContext();
    private static final int SMALL_PUBLICATIONS_NUMBER = 2;
    private ResourceService resourceService;
    private TicketService ticketService;
    private MessageService messageService;
    private ListTicketsHandler handler;
    private ByteArrayOutputStream outputStream;
    
    @BeforeEach
    public void init() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
        this.messageService = new MessageService(client);
        this.handler = new ListTicketsHandler(ticketService);
        this.outputStream = new ByteArrayOutputStream();
    }

    @ParameterizedTest
    @MethodSource("publication.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnAllPendingTicketsOfUser(Class<? extends TicketEntry> ticketType, PublicationStatus status) throws IOException {
        var user = randomResourcesOwner();
        var expectedTickets =
            generateTickets(ticketType, status, user).map(this::constructDto).collect(Collectors.toList());
        var request = buildHttpRequest(user);
        handler.handleRequest(request, outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(body.getTickets(), containsInAnyOrder(expectedTickets.toArray(TicketDto[]::new)));
    }

    @ParameterizedTest
    @MethodSource("publication.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldReturnEmptyListWhenUserHasNoTickets(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var user = randomResourcesOwner();
        TicketTestUtils.createPersistedPublication(status, resourceService);
        var request = buildHttpRequest(user);
        handler.handleRequest(request, outputStream, CONTEXT);
        var response = GatewayResponse.fromOutputStream(outputStream, TicketCollection.class);
        var body = response.getBodyObject(TicketCollection.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(body.getTickets(), is(empty()));
    }
    
    private static UserInstance randomResourcesOwner() {
        return UserInstance.create(randomString(), randomUri());
    }
    
    private static InputStream buildHttpRequest(UserInstance user) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                   .withNvaUsername(user.getUsername())
                   .withCustomerId(user.getOrganizationUri())
                   .build();
    }
    
    private Stream<TicketEntry> generateTickets(Class<? extends TicketEntry> ticketType, PublicationStatus status,
                                                UserInstance owner) {
        return IntStream.range(0, SMALL_PUBLICATIONS_NUMBER)
                   .boxed()
                   .map(ignored -> attempt(() -> TicketTestUtils.createPersistedPublicationWithOwner(status, owner, resourceService)).orElseThrow())
                   .map(publication -> attempt(() -> TicketTestUtils.createPersistedTicket(publication, ticketType,
                                                                                ticketService)).orElseThrow());
    }
    
    private TicketDto constructDto(TicketEntry ticketEntry) {
        var messages = ticketEntry.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticketEntry, messages);
    }
}
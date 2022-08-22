package no.unit.nva.publication.publishingrequest.create;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.publishingrequest.create.CreateTicketHandler.LOCATION_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CreateTicketHandlerTest extends ResourcesLocalTest {
    
    public static final FakeContext CONTEXT = new FakeContext();
    private CreateTicketHandler handler;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private TicketService ticketService;
    
    public static Stream<TicketDto> ticketDtoProvider() {
        return Stream.of(new DoiRequestDto(), new PublishingRequestDto());
    }
    
    public static Stream<Arguments> ticketEntryProvider() {
        return Stream.of(Arguments.of(DoiRequest.class), Arguments.of(PublishingRequestCase.class));
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        this.output = new ByteArrayOutputStream();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client, Clock.systemDefaultZone());
        this.handler = new CreateTicketHandler(ticketService, resourceService);
    }
    
    @ParameterizedTest(name = "ticketType")
    @DisplayName("should persist ticket")
    @MethodSource("ticketEntryProvider")
    void shouldPersistTicket(Class<? extends TicketEntry> ticketType) throws IOException, ApiGatewayException {
        var publication = createPersistedPublication();
        var requestBody = constructDto(ticketType);
        var input = createHttpTicketCreationRequest(requestBody, publication);
        handler.handleRequest(input, output, CONTEXT);
        
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_SEE_OTHER)));
        
        var location = URI.create(response.getHeaders().get(LOCATION_HEADER));
        
        assertThatLocationHeaderHasExpectedFormat(publication, location);
        assertThatLocationHeaderPointsToCreatedTicket(location, ticketType);
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
    
    private void assertThatLocationHeaderPointsToCreatedTicket(URI ticketUri,
                                                               Class<? extends TicketEntry> ticketType)
        throws NotFoundException {
        var publication = fetchPublication(ticketUri);
        var ticketIdentifier = extractTicketIdentifierFromLocation(ticketUri);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier, ticketType);
        assertThat(ticket.getResourceIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(ticket.getIdentifier(), is(equalTo(ticketIdentifier)));
    }
    
    private Publication fetchPublication(URI ticketUri) throws NotFoundException {
        var publicationIdentifier = extractPublicationIdentifierFromLocation(ticketUri);
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }
    
    private TicketDto constructDto(Class<? extends TicketEntry> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return new DoiRequestDto();
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return new PublishingRequestDto();
        }
        throw new RuntimeException("Unrecognized ticket type");
    }
    
    private Publication createPersistedPublication() throws ApiGatewayException {
        var publication = randomPublication();
        publication = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        return resourceService.getPublication(publication);
    }
    
    private InputStream createHttpTicketCreationRequest(TicketDto ticketDto, Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withPathParameters(Map.of("publicationIdentifier", publication.getIdentifier().toString()))
                   .build();
    }
}

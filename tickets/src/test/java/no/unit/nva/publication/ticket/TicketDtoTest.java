package no.unit.nva.publication.ticket;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Clock;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TicketDtoTest extends ResourcesLocalTest {
    
    private ResourceService resourceService;
    private TicketService ticketService;
    
    public static Stream<Arguments> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class).map(Arguments::of);
    }
    
    @BeforeEach
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
    }
    
    @ParameterizedTest(name = "ticketType:{0}")
    @DisplayName("should include all publication summary fields")
    @MethodSource("ticketTypeProvider")
    void shouldIncludeAllPublicationSummaryFields(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        var publication = draftPublicationWithoutDoi();
        var ticket = TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
        var dto = TicketDto.fromTicket(ticket);
        var publicationSummary = dto.getPublicationSummary();
        assertThat(publicationSummary, doesNotHaveEmptyValues());
    }
    
    private Publication draftPublicationWithoutDoi() {
        var publication = randomPublication().copy().withDoi(null).withStatus(DRAFT).build();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
}
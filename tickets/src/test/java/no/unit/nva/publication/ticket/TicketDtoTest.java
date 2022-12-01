package no.unit.nva.publication.ticket;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.attempt.Try;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest(name = "should accept both date (legacy) and createdDate: {0}")
    @ValueSource(strings = {"date", "createdDate"})
    void shouldAcceptBothLegacyDateAndCreatedDate(String field) {
        var isoDateTime = "2022-12-01T11:07:32.039628Z";
        var input = JsonUtils.dtoObjectMapper.createObjectNode()
                        .put("type", MessageDto.TYPE)
                        .put(field, isoDateTime);

        var result = attempt(() -> JsonUtils.dtoObjectMapper.readValue(input.toString(), MessageDto.class))
                         .orElseThrow();

        assertThat(result.getCreatedDate(), is(equalTo(Instant.parse(isoDateTime))));
    }

    private Publication draftPublicationWithoutDoi() {
        var publication = randomPublication().copy().withDoi(null).withStatus(DRAFT).build();
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }
}